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
import scala.collection.mutable
import com.toloka.cho.server.http.responces.*
import com.toloka.cho.server.logging.syntax.*


import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger
import com.toloka.cho.server.http.validation.syntax.HttpValidationDsl
import com.toloka.cho.domain.pagination.Pagination
import com.toloka.cho.server.domain.security.*


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
                case Some(value) if BookSort.fromQueryValue(value).isEmpty => BadRequest("Invalid sort value")
                case _ =>
                    val selectedSort = sort.flatMap(BookSort.fromQueryValue).getOrElse(BookSort.New)
                    for {
                        filter <- req.as[BookFilter]
                        response <-
                            if (filter.search.exists(_.length > 100)) BadRequest("Search text must be 100 characters or fewer")
                            else
                                books.all(filter, Pagination(limit, skip), selectedSort).flatMap(Ok(_))
                    } yield response
    }

    // GET /jobs/uuid
    private val findBookRoute: HttpRoutes[F] =  HttpRoutes.of[F] {
        case GET -> Root / UUIDVar(id)  => 
            books.find(id).flatMap {
                case Some(book) => Ok(book)
                case None      => NotFound(FailureResponse(s"Book $id not found"))
            }
    }

    private val createBookRoute: AuthRoute[F] = {
        case req @ POST -> Root / "create" asAuthed _  =>
            req.request.validate[BookInfo] { bookInfo =>
                for {
                    bookInfo <- req.request.as[BookInfo].logError(e => s"Parsing payload failed: $e")
                    bookId <- books.create(bookInfo)
                    resp <- Created(bookId)
                } yield resp
            }
            
    }

    private val updateBookRoute: AuthRoute[F] = {
        case req @ PUT -> Root / UUIDVar(id)  asAuthed user =>
            req.request.validate[BookInfo] { bookInfo => 
                books.find(id).flatMap {
                    case None => NotFound(FailureResponse(s"Cannot update book $id: not found"))
                    case Some(book) if user.isAdmin || user.isLibrarian => books.update(id, bookInfo) *> Ok()
                    case _ => Forbidden(FailureResponse("You can only update your own books"))

                }

            }
            
    }

    private val deleteBookRoute: AuthRoute[F] = {
        case req @ DELETE -> Root / UUIDVar(id) asAuthed user  =>
            books.find(id).flatMap {
                case None => NotFound(FailureResponse(s"Cannot delete book $id: not found"))
                case Some(book) if user.isAdmin => books.delete(id) *> Ok()
                case _ => Forbidden(FailureResponse("Only Admin can delete books"))
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
