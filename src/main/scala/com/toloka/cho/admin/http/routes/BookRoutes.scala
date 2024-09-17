package com.toloka.cho.admin.http.routes

import io.circe.generic.auto.*
import org.http4s.* 
import org.http4s.dsl.* 
import org.http4s.server.* 
import org.http4s.dsl.Http4sDsl
import cats.implicits.*
import cats.*
import cats.effect.*

import tsec.authentication.asAuthed
import tsec.authentication.SecuredRequestHandler
import scala.language.implicitConversions

import java.util.UUID
import com.toloka.cho.admin.domain.book.* 
import com.toloka.cho.admin.core.*
import scala.collection.mutable
import com.toloka.cho.admin.http.responces.*
import com.toloka.cho.admin.logging.syntax.*


import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger
import com.toloka.cho.admin.palyground.BooksPlayground.bookInfo
import com.toloka.cho.admin.http.validation.syntax.HttpValidationDsl
import com.toloka.cho.admin.domain.pagination.Pagination
import com.toloka.cho.admin.domain.security.*


class BookRoutes [F[_]: Concurrent: Logger: SecuredHandler] private (books: Books[F]) extends HttpValidationDsl[F] {

    object SkipQueryParem  extends OptionalQueryParamDecoderMatcher[Int]("skip")
    object LimitQueryParem extends OptionalQueryParamDecoderMatcher[Int]("limit")

    // POST /jobs?offset==x&limit=y { filters } // TODO add query params and filters
    private val allBooksRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root :? LimitQueryParem(limit) +& SkipQueryParem(skip) => 
            for {
                filter <- req.as[BookFilter]
                bookList <- books.all(filter, Pagination(limit, skip))
                resp <- Ok(bookList)
            } yield resp
    }

    // GET /jobs/uuid
    private val findBookRoute: HttpRoutes[F] =  HttpRoutes.of[F] {
        case GET -> Root / UUIDVar(id)  => 
            books.find(id).flatMap {
                case Some(book) => Ok(book)
                case None      => NotFound(FailureResponse(s"Job $id not found"))
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
                case None => NotFound(FailureResponse(s"Cannot delete job $id: not found"))
                case Some(book) if user.isAdmin => books.delete(id) *> Ok()
                case _ => Forbidden(FailureResponse("Only Admin can delete books"))
            }
      
    }

    val unauthedRoutes = (allBooksRoute <+> findBookRoute)
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
