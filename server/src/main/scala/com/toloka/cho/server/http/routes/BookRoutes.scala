package com.toloka.cho.server.http.routes


import org.http4s.*
import org.http4s.dsl.io
import _root_.io.circe.generic.auto.*
import org.http4s.server.*
import org.http4s.dsl.Http4sDsl
import cats.implicits.*
import cats.*
import cats.effect.*

import tsec.authentication.asAuthed
import tsec.authentication.SecuredRequestHandler
import scala.language.implicitConversions

import java.util.UUID
import com.toloka.cho.domain.book.*
import com.toloka.cho.domain.PublisherInfo
import com.toloka.cho.server.core.*
import com.toloka.cho.server.http.responses.*
import com.toloka.cho.server.logging.syntax.*


import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger
import com.toloka.cho.server.http.validation.syntax.HttpValidationDsl
import com.toloka.cho.domain.pagination.Pagination
import com.toloka.cho.domain.security.*


class BookRoutes [F[_]: Concurrent: Logger: SecuredHandler] private (books: Books[F]) extends HttpValidationDsl[F] {

    object SkipQueryParem  extends OptionalQueryParamDecoderMatcher[Int]("skip")
    object LimitQueryParem extends OptionalQueryParamDecoderMatcher[Int]("limit")
    object SortQueryParam extends OptionalQueryParamDecoderMatcher[String]("sort")

    private val allFiltersRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "filters" =>
        books.possibleFilters().flatMap(jf => Ok(jf))
    }

    // POST /jobs?offset==x&limit=y { filters } // TODO add query params and filters
    private val allBooksRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root :? LimitQueryParem(limit) +& SkipQueryParem(skip) +& SortQueryParam(sort) =>
            listBooks(req, limit, skip, sort, books.all)
    }

    private val managementBooksRoute: AuthRoute[F] = {
        case req @ POST -> Root / "manage" :? LimitQueryParem(limit) +& SkipQueryParem(skip) +& SortQueryParam(sort) asAuthed _ =>
            listBooks(req.request, limit, skip, sort, books.allForManagement)
    }

    private def listBooks(
        req: Request[F],
        limit: Option[Int],
        skip: Option[Int],
        sort: Option[String],
        load: (BookFilter, Pagination, BookSort) => F[List[Book]]
    ): F[Response[F]] =
            sort match
                case Some(value) if BookSort.fromQueryValue(value).isEmpty => 
                  BadRequest(ErrorResponse.fromApiError(ApiError.ValidationError("Invalid sort value")))
                case _ =>
                    val selectedSort = sort.flatMap(BookSort.fromQueryValue).getOrElse(BookSort.New)
                    for {
                        filter <- req.as[BookFilter].attempt
                        response <- filter match {
                          case Right(f) =>
                            if (f.search.exists(_.length > 100)) 
                              BadRequest(ErrorResponse.fromApiError(ApiError.ValidationError("Search text must be 100 characters or fewer")))
                            else
                              load(f, Pagination(limit, skip), selectedSort).attempt.flatMap {
                                case Right(bookList) => Ok(bookList)
                                case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(e.getMessage)))
                              }
                          case Left(e) => 
                            BadRequest(ErrorResponse.fromApiError(ApiError.ValidationError(s"Invalid request: ${e.getMessage}")))
                        }
                    } yield response
    // GET /jobs/uuid
    private val findBookRoute: HttpRoutes[F] =  HttpRoutes.of[F] {
        case GET -> Root / UUIDVar(id)  => 
            books.find(id).attempt.flatMap {
                case Right(Some(book)) => Ok(book)
                case Right(None) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Book", id.toString)))
                case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to find book: ${e.getMessage}")))
            }
    }

    private val createBookRoute: AuthRoute[F] = {
        case req @ POST -> Root / "create" asAuthed _  =>
            validateBook(req.request) { bookInfo =>
                for {
                    bookId <- books.create(bookInfo).attempt
                    resp <- bookId match {
                      case Right(id) => Created(id)
                      case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to create book: ${e.getMessage}")))
                    }
                } yield resp
            }
            
    }

    private val updateBookRoute: AuthRoute[F] = {
        case req @ PUT -> Root / UUIDVar(id)  asAuthed user =>
            validateBook(req.request) { bookInfo =>
                books.find(id).attempt.flatMap {
                    case Right(None) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Book", id.toString)))
                    case Right(Some(book)) =>
                      if (user.isAdmin || user.isLibrarian) 
                        books.update(id, bookInfo).attempt.flatMap {
                          case Right(_) => Ok()
                          case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to update book: ${e.getMessage}")))
                        }
                      else 
                        Forbidden(ErrorResponse.fromApiError(ApiError.AuthorizationError("Only admins and librarians can update books")))
                    case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to check book: ${e.getMessage}")))
                }

            }
            
    }

    private val deleteBookRoute: AuthRoute[F] = {
        case req @ DELETE -> Root / UUIDVar(id) asAuthed user  =>
            books.find(id).attempt.flatMap {
                case Right(None) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Book", id.toString)))
                case Right(Some(_)) =>
                  if (user.isAdmin || user.isLibrarian)
                    books.delete(id).attempt.flatMap {
                      case Right(_) => Ok()
                      case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to delete book: ${e.getMessage}")))
                    }
                  else 
                    Forbidden(ErrorResponse.fromApiError(ApiError.AuthorizationError("Only library staff can delete books")))
                case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to check book: ${e.getMessage}")))
            }
    }

    private def validateBook(req: Request[F])(save: BookInfo => F[Response[F]]): F[Response[F]] =
        req.validate[BookInfo] { info =>
            if info.languages.isEmpty then save(info)
            else books.allLanguages().flatMap { supported =>
                val unknown = info.languages.filterNot(supported.contains)
                if unknown.nonEmpty then
                    BadRequest(ErrorResponse.fromApiError(ApiError.ValidationError(s"Unknown book languages: ${unknown.mkString(", ")}")))
                else save(info.copy(languages = info.languages.distinct))
            }
        }

    private val allLanguagesRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case GET -> Root / "languages" => books.allLanguages().flatMap(Ok(_))
    }

    private val allPublishersRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case GET -> Root / "publishers" =>
            books.allPublishers().flatMap(Ok(_))
    }

    private val createPublisherRoute: AuthRoute[F] = {
        case req @ POST -> Root / "publishers" / "create" asAuthed _ =>
            req.request.validate[PublisherInfo] { info =>
                books.createPublisher(info).flatMap(id => Created(id))
            }
    }

    private val addCopyRoute: AuthRoute[F] = {
        case POST -> Root / UUIDVar(bookId) / "copies" asAuthed _ =>
            books.addCopy(bookId).flatMap {
                case Some(copyId) => Created(copyId)
                case None => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Book", bookId.toString)))
            }
    }

    private val deleteCopyRoute: AuthRoute[F] = {
        case DELETE -> Root / UUIDVar(bookId) / "copies" / UUIDVar(copyId) asAuthed _ =>
            books.deleteCopy(bookId, copyId).flatMap {
                case 1 => Ok()
                case _ => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Copy", copyId.toString)))
            }
    }

    val unauthedRoutes = (allFiltersRoute <+> allBooksRoute <+> findBookRoute <+> allPublishersRoute <+> allLanguagesRoute)
    val authedRoutes =  SecuredHandler[F].liftService(
        managementBooksRoute.restrictedTo(staffOnly) |+|
        createBookRoute.restrictedTo(staffOnly) |+|
        updateBookRoute.restrictedTo(allRoles) |+|
        deleteBookRoute.restrictedTo(allRoles) |+|
        createPublisherRoute.restrictedTo(staffOnly) |+|
        addCopyRoute.restrictedTo(staffOnly) |+|
        deleteCopyRoute.restrictedTo(staffOnly)
)


    val routes = Router(
        "/books" -> (unauthedRoutes <+> authedRoutes)
    )
}

object BookRoutes {
     def apply[F[_]: Concurrent: Logger: SecuredHandler](books: Books[F]) = new BookRoutes[F](books)
}
