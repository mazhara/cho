package com.toloka.cho.admin.http.routes

import io.circe.generic.auto.*
import org.http4s.* 
import org.http4s.dsl.* 
import org.http4s.server.* 
import org.http4s.dsl.Http4sDsl
import cats.implicits.*
import cats.*
import cats.effect.*
import java.util.UUID
import com.toloka.cho.admin.domain.book.* 
import com.toloka.cho.admin.core.*
import scala.collection.mutable
import com.toloka.cho.admin.http.responces.*

import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger
import com.toloka.cho.admin.palyground.BooksPlayground.bookInfo

class BookRoutes [F[_]: Concurrent: Logger] private (books: Books[F]) extends Http4sDsl[F] {
  
    import com.toloka.cho.admin.logging.syntax.*

    // POST /jobs?offset==x&limit=y { filters } // TODO add query params and filters
    private val allBooksRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case POST -> Root => 
            for {
                bookList <- books.all()
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

    private val createBookRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root / "create" =>
            for
                bookInfo <- req.as[BookInfo].logError(e => s"Parsing payload failed: $e")
                bookId <- books.create(bookInfo)
                resp <- Created(bookId)
            yield resp
    }

    private val updateBookRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ PUT -> Root / UUIDVar(id) =>
            for {
                bookInfo <- req.as[BookInfo]
                maybeNewBook <- books.update(id, bookInfo)
                resp <- maybeNewBook match {
                    case Some(job) => Ok()
                    case None => NotFound(FailureResponse(s"Cannot update book $id: not found"))
                } 
            } yield resp
    }
     
    private val deleteBookRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ DELETE -> Root / UUIDVar(id) =>
            books.find(id).flatMap {
                case Some(book) =>
                    for
                        _ <- books.delete(id)
                        resp <- Ok()
                    yield resp
                case None => NotFound(FailureResponse(s"Cannot delete job $id: not found"))
                
        }
    }


    val routes = Router(
        "/books" -> (allBooksRoute <+> findBookRoute <+> createBookRoute <+> updateBookRoute <+> deleteBookRoute)
    )
}

object BookRoutes {
    def apply[F[_]: Concurrent: Logger](books: Books[F]) =  new BookRoutes[F](books)
}
