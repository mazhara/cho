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
                              books.all(f, Pagination(limit, skip), selectedSort).attempt.flatMap {
                                case Right(bookList) => Ok(bookList)
                                case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(e.getMessage)))
                              }
                          case Left(e) => 
                            BadRequest(ErrorResponse.fromApiError(ApiError.ValidationError(s"Invalid request: ${e.getMessage}")))
                        }
                    } yield response
    }

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
            req.request.validate[BookInfo] { bookInfo =>
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
            req.request.validate[BookInfo] { bookInfo => 
                books.find(id).attempt.flatMap {
                    case Right(None) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Book", id.toString)))
                    case Right(Some(book)) =>
                      if (user.isAdmin || user.isLibrarian) 
                        books.update(id, bookInfo).attempt.flatMap {
                          case Right(_) => Ok()
                          case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to update book: ${e.getMessage}")))
                        }
                      else 
                        Forbidden(ErrorResponse.fromApiError(ApiError.AuthorizationError("You can only update your own books")))
                    case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to check book: ${e.getMessage}")))
                }

            }
            
    }

    private val deleteBookRoute: AuthRoute[F] = {
        case req @ DELETE -> Root / UUIDVar(id) asAuthed user  =>
            books.find(id).attempt.flatMap {
                case Right(None) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Book", id.toString)))
                case Right(Some(_)) =>
                  if (user.isAdmin) 
                    books.delete(id).attempt.flatMap {
                      case Right(_) => Ok()
                      case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to delete book: ${e.getMessage}")))
                    }
                  else 
                    Forbidden(ErrorResponse.fromApiError(ApiError.AuthorizationError("Only Admin can delete books")))
                case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to check book: ${e.getMessage}")))
            }
    }

    val unauthedRoutes = (allFiltersRoute <+>allBooksRoute <+> findBookRoute)
    val authedRoutes =  SecuredHandler[F].liftService(
        createBookRoute.restrictedTo(allRoles) |+|
        updateBookRoute.restrictedTo(allRoles) |+|
        deleteBookRoute.restrictedTo(allRoles)
)


    val routes = Router(
        "/books" -> (unauthedRoutes <+> authedRoutes)
    )
}

object BookRoutes {
     def apply[F[_]: Concurrent: Logger: SecuredHandler](books: Books[F]) = new BookRoutes[F](books)
}
