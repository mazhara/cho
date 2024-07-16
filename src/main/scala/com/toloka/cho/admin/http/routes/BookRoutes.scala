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
import scala.collection.mutable
import com.toloka.cho.admin.http.responces.*

import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger




class BookRoutes [F[_]: Concurrent: Logger] private extends Http4sDsl[F] {

    private val database = mutable.Map[UUID, Book]()
    import com.toloka.cho.admin.logging.syntax.*

    // POST /jobs?offset==x&limit=y { filters } // TODO add query params and filters
    private val allBooksRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case POST -> Root => Ok(database.values)
    }

    // GET /jobs/uuid
    private val findBookRoute: HttpRoutes[F] =  HttpRoutes.of[F] {
        case GET -> Root / UUIDVar(id)  => 
            database.get(id) match
                case Some(book) => Ok(book)
                case None      => NotFound(FailureResponse(s"Job $id not found"))
    }

     // POST /jobs { jobInfo }
    private def createBook(bookInfo: BookInfo): F[Book] =
        Book (
            id = UUID.randomUUID(), // FIXME should use UUID context bound
            bookInfo =  bookInfo
        ).pure[F]

    private val createBookRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ POST -> Root / "create" =>
            for
                bookInfo <- req.as[BookInfo].logError(e => s"Parsing payload failed: $e")
                book <- createBook(bookInfo)
                _ <- database.put(book.id, book).pure[F]
                resp <- Created(book.id)
            yield resp
    }

    private val updateBookRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ PUT -> Root / UUIDVar(id) =>
            database.get(id) match
                case Some(job) =>
                    for
                        bookInfo <- req.as[BookInfo]
                        _ <- database.put(id, job.copy(bookInfo = bookInfo)).pure[F]
                        resp <- Ok()
                    yield resp
                case None => NotFound(FailureResponse(s"Cannot update book $id: not found"))
    }

    private val deleteBookRoute: HttpRoutes[F] = HttpRoutes.of[F] {
        case req @ DELETE -> Root / UUIDVar(id) =>
            database.get(id) match
                case Some(job) =>
                    for
                        jobInfo <- req.as[BookInfo]
                        _ <- database.remove(id).pure[F]
                        resp <- Ok()
                    yield resp
                case None => NotFound(FailureResponse(s"Cannot delete job $id: not found"))
    }


    val routes = Router(
        "/books" -> (allBooksRoute <+> findBookRoute <+> createBookRoute <+> updateBookRoute <+> deleteBookRoute)
    )
}

object BookRoutes {
    def apply[F[_]: Concurrent: Logger] =  new BookRoutes[F]
}
