package com.toloka.cho.server.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import org.http4s.*
import org.http4s.server.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import tsec.authentication.asAuthed
import tsec.authentication.SecuredRequestHandler
import tsec.authentication.TSecAuthService
import scala.language.implicitConversions

import com.toloka.cho.domain.auth.LoginInfo
import com.toloka.cho.server.core.Auth
import com.toloka.cho.server.http.validation.syntax.HttpValidationDsl
import com.toloka.cho.domain.user.*
import com.toloka.cho.domain.auth.*
import com.toloka.cho.domain.security.*

import com.toloka.cho.server.http.responses.*
import com.toloka.cho.server.logging.syntax.log

class AuthRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (
    auth: Auth[F],
    authenticator: Authenticator[F]
) extends HttpValidationDsl[F] {

  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req.validate[LoginInfo] { loginInfo =>
      val maybeJwtToken = for {
        mayberUser <- auth.login(loginInfo.email, loginInfo.password)
        _          <- Logger[F].info(s"User logging in: ${loginInfo.email}")
        maybeToken <- mayberUser.traverse(user => authenticator.create(user.email))
      } yield maybeToken
      
      maybeJwtToken.map {
        case Some(token) => authenticator.embed(Response(Status.Ok), token)
        case None        => Response(Status.Unauthorized)
      }
    }
  }

  private val forgotPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "reset" =>
      for {
        fpInfo <- req.as[ForgotPasswordInfo].attempt
        resp <- fpInfo match {
          case Right(info) =>
            auth.sendPasswordRecoveryToken(info.email).attempt.flatMap {
              case Right(_) => Ok()
              case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to send recovery email: ${e.getMessage}")))
            }
          case Left(e) => BadRequest(ErrorResponse.fromApiError(ApiError.ValidationError(s"Invalid request: ${e.getMessage}")))
        }
      } yield resp
  }

  private val recoverPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "recover" =>
      for {
        rpInfo <- req.as[RecoverPasswordInfo].attempt
        resp <- rpInfo match {
          case Right(info) =>
            auth.recoverPasswordFromToken(
              info.email,
              info.token,
              info.newPassword
            ).flatMap { result =>
              if (result) Ok()
              else Forbidden(ErrorResponse.fromApiError(ApiError.AuthorizationError("Email/Token combination is incorrect")))
            }
          case Left(e) => BadRequest(ErrorResponse.fromApiError(ApiError.ValidationError(s"Invalid request: ${e.getMessage}")))
        }
      } yield resp
  }

  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" => 
      req.validate[NewUserInfo] { newUserInfo =>
        for {
          maybeNewUser <- auth.signUp(newUserInfo).attempt
          resp <- maybeNewUser match {
            case Right(Some(user)) => Created(user.email)
            case Right(None) => BadRequest(ErrorResponse.fromApiError(ApiError.ConflictError(s"User with email ${newUserInfo.email} already exists.")))
            case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to create user: ${e.getMessage}")))
          }
        } yield resp
      }
  }

  private val changePasswordRoute: AuthRoute[F] = {
    case req @ PUT -> Root / "users" / "password" asAuthed user =>
      req.request.validate[NewPasswordInfo] { newPasswordInfo =>
        for {
          maybeUserOrError <- auth.changePassword(user.email, newPasswordInfo).attempt
          resp <- maybeUserOrError match {
            case Right(Right(Some(_))) => Ok()
            case Right(Right(None)) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("User", user.email)))
            case Right(Left(error)) => Forbidden(ErrorResponse.fromApiError(ApiError.AuthorizationError(error)))
            case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to change password: ${e.getMessage}")))
          }
        } yield resp
      }
   }

  private val logoutRoute: AuthRoute[F] = { case req @ POST -> Root / "logout" asAuthed _ =>
    val token = req.authenticator
    for {
      _    <- authenticator.discard(token)
      resp <- Ok()
    } yield resp
  }

  private val deleteUserRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / "users" / email asAuthed user =>
      auth.delete(email).attempt.flatMap {
        case Right(true) => Ok()
        case Right(false) => NotFound(ErrorResponse.fromApiError(ApiError.NotFound("User", email)))
        case Left(e) => InternalServerError(ErrorResponse.fromApiError(ApiError.InternalServerError(s"Failed to delete user: ${e.getMessage}")))
      }
  }

  private val checkTokenRoute: AuthRoute[F] = { case GET -> Root / "checkToken" asAuthed _ =>
    Ok()
  }

  val unauthedRoutes = (loginRoute <+> createUserRoute <+> forgotPasswordRoute <+> recoverPasswordRoute)
  val authedRoutes = SecuredHandler[F].liftService(
      checkTokenRoute.restrictedTo(allRoles) |+|
      changePasswordRoute.restrictedTo(allRoles) |+|
      logoutRoute.restrictedTo(allRoles) |+|
      deleteUserRoute.restrictedTo(adminOnly)
  )

  val routes = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](
      auth: Auth[F],
      authenticator: Authenticator[F]
  ) =
    new AuthRoutes[F](auth, authenticator)
}
