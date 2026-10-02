package com.toloka.cho.server.http.routes

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
import com.toloka.cho.server.core.*
import com.toloka.cho.server.http.responses.*
import com.toloka.cho.server.logging.syntax.*


import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger
import com.toloka.cho.server.http.validation.syntax.HttpValidationDsl
import com.toloka.cho.domain.pagination.Pagination
import com.toloka.cho.domain.security.*
import com.toloka.cho.domain.AuthorInfo
import org.http4s.dsl.impl.QueryParamDecoderMatcher


class AuthorRoutes [F[_]: Concurrent: Logger: SecuredHandler] private (authors: Authors[F]) extends HttpValidationDsl[F] {
 
    // GET /authors/uuid
    private val findAuthorRoute: HttpRoutes[F] =  HttpRoutes.of[F] {
        case GET -> Root / UUIDVar(id)  => 
            authors.find(id).attempt.flatMap {
                case Right(Some(author)) => Ok(author)
                case Right(None) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Author", id.toString)))
                case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to find author: ${e.getMessage}")))
            }
    }

    private val findAuthorByPatternRoute: HttpRoutes[F] =  HttpRoutes.of[F] {
        case GET -> Root / "search":?QueryParamMatcher(searchTerm)  => 
            authors.find(searchTerm).attempt.flatMap {
                case Right(Nil) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Author", searchTerm)))
                case Right(authorList) => Ok(authorList)
                case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to search authors: ${e.getMessage}")))
            }
    }

    private val createAuthorRoute: AuthRoute[F] = {
        case req @ POST -> Root / "create" asAuthed _  =>
            req.request.validate[AuthorInfo] { authorInfo =>
                for {
                    authorId <- authors.create(authorInfo).attempt
                    resp <- authorId match {
                      case Right(id) => Created(id)
                      case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to create author: ${e.getMessage}")))
                    }
                } yield resp
            }
            
    }

    private val updateAuthorRoute: AuthRoute[F] = {
        case req @ PUT -> Root / UUIDVar(id)  asAuthed user =>
            req.request.validate[AuthorInfo] { authorInfo => 
                authors.find(id).attempt.flatMap {
                    case Right(None) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Author", id.toString)))
                    case Right(Some(_)) =>
                      if (user.isAdmin || user.isLibrarian) 
                        authors.update(id, authorInfo).attempt.flatMap {
                          case Right(_) => Ok()
                          case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to update author: ${e.getMessage}")))
                        }
                      else 
                        Forbidden(ErrorResponse.fromApiError(ApiError.AuthorizationError("You can only update your own authors")))
                    case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to check author: ${e.getMessage}")))
                }

            }
            
    }
     
    private val deleteAuthorRoute: AuthRoute[F] = {
        case req @ DELETE -> Root / UUIDVar(id) asAuthed user  =>
            authors.find(id).attempt.flatMap {
                case Right(None) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("Author", id.toString)))
                case Right(Some(_)) =>
                  if (user.isAdmin) 
                    authors.delete(id).attempt.flatMap {
                      case Right(_) => Ok()
                      case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to delete author: ${e.getMessage}")))
                    }
                  else 
                    Forbidden(ErrorResponse.fromApiError(ApiError.AuthorizationError("Only Admin can delete authors")))
                case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to check author: ${e.getMessage}")))
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
