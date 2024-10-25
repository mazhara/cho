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
import com.toloka.cho.admin.core.*
import scala.collection.mutable
import com.toloka.cho.admin.http.responces.*
import com.toloka.cho.admin.logging.syntax.*


import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger
import com.toloka.cho.admin.http.validation.syntax.HttpValidationDsl
import com.toloka.cho.domain.pagination.Pagination
import com.toloka.cho.domain.security.*
import com.toloka.cho.domain.AuthorInfo
import org.http4s.dsl.impl.QueryParamDecoderMatcher


class AuthorRoutes [F[_]: Concurrent: Logger: SecuredHandler] private (authors: Authors[F]) extends HttpValidationDsl[F] {
 
    // GET /authors/uuid
    private val findAuthorRoute: HttpRoutes[F] =  HttpRoutes.of[F] {
        case GET -> Root / UUIDVar(id)  => 
            authors.find(id).flatMap {
                case Some(author) => Ok(author)
                case None      => NotFound(FailureResponse(s"Author $id not found"))
            }
    }

    private val findAuthorByPatternRoute: HttpRoutes[F] =  HttpRoutes.of[F] {
        case GET -> Root / "search":?QueryParamMatcher(searchTerm)  => 
            authors.find(searchTerm).flatMap {
                case Nil      => 
                    NotFound(FailureResponse(s"Author $searchTerm not found"))
                case authorList => 
                    Ok(authorList)
                
            }
    }

    private val createAuthorRoute: AuthRoute[F] = {
        case req @ POST -> Root / "create" asAuthed _  =>
            req.request.validate[AuthorInfo] { authorInfo =>
                for {
                    authorInfo <- req.request.as[AuthorInfo].logError(e => s"Parsing payload failed: $e")
                    authorId <- authors.create(authorInfo)
                    resp <- Created(authorId)
                } yield resp
            }
            
    }

    private val updateAuthorRoute: AuthRoute[F] = {
        case req @ PUT -> Root / UUIDVar(id)  asAuthed user =>
            req.request.validate[AuthorInfo] { authorInfo => 
                authors.find(id).flatMap {
                    case None => NotFound(FailureResponse(s"Cannot update author $id: not found"))
                    case Some(author) if user.isAdmin || user.isLibrarian => authors.update(id, authorInfo) *> Ok()
                    case _ => Forbidden(FailureResponse("You can only update your own authors"))

                }

            }
            
    }
     
    private val deleteAuthorRoute: AuthRoute[F] = {
        case req @ DELETE -> Root / UUIDVar(id) asAuthed user  =>
            authors.find(id).flatMap {
                case None => NotFound(FailureResponse(s"Cannot delete author $id: not found"))
                case Some(book) if user.isAdmin => authors.delete(id) *> Ok()
                case _ => Forbidden(FailureResponse("Only Admin can delete authors"))
            }
      
    }

    val unauthedRoutes = (findAuthorRoute <+> findAuthorByPatternRoute)
    val authedRoutes =  SecuredHandler[F].liftService(
        createAuthorRoute.restrictedTo(allRoles) |+|
        updateAuthorRoute.restrictedTo(allRoles) |+|
        deleteAuthorRoute.restrictedTo(allRoles)
  )


    val routes = Router(
        "/authors" -> (unauthedRoutes <+> authedRoutes)
    )
}

object AuthorRoutes {
     def apply[F[_]: Concurrent: Logger: SecuredHandler](authors: Authors[F]) = new AuthorRoutes[F](authors)
}

object QueryParamMatcher extends QueryParamDecoderMatcher[String]("query")
