package com.toloka.cho.server.http.routes

import cats.effect.Async
import cats.implicits.*
import com.toloka.cho.server.core.Users
import com.toloka.cho.server.http.responses.FailureResponse
import com.toloka.cho.server.http.validation.syntax.HttpValidationDsl
import com.toloka.cho.domain.ManagedUserForm
import com.toloka.cho.domain.security.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.authentication.{SecuredRequestHandler, asAuthed}
import tsec.passwordhashers.jca.BCrypt
import java.sql.SQLException

final class UserManagementRoutes[F[_]: Async: Logger: SecuredHandler] private (
    users: Users[F]
) extends Http4sDsl[F]
    with HttpValidationDsl[F]:

  private val listUsersRoute: AuthRoute[F] = {
    case req @ GET -> Root :? SearchQuery(search) +& LimitQuery(limit) +& SkipQuery(skip) asAuthed user =>
      if !user.isAdmin then Forbidden(FailureResponse("Only administrators can manage users"))
      else
        val parsedLimit = limit.fold(Some(50))(_.toIntOption)
        val parsedSkip = skip.fold(Some(0))(_.toIntOption)
        (parsedLimit, parsedSkip) match
          case (Some(pageSize), Some(offset)) if pageSize >= 1 && pageSize <= 100 && offset >= 0 =>
            users.allManaged(search.getOrElse(""), pageSize, offset).flatMap(Ok(_))
          case _ => BadRequest(FailureResponse("Invalid user list pagination"))
  }

  private val createUserRoute: AuthRoute[F] = {
    case req @ POST -> Root asAuthed user =>
      if !user.isAdmin then Forbidden(FailureResponse("Only administrators can create users"))
      else
        req.request.validate[ManagedUserForm] { form =>
          form.password match
            case None => BadRequest(FailureResponse("A password is required when creating a user"))
            case Some(password) =>
              BCrypt.hashpw[F](password).flatMap { hashedPassword =>
                users.createManaged(form, hashedPassword).flatMap {
                  case Some(created) => Created(created)
                  case None => Conflict(FailureResponse("That email or username is already in use"))
                }
              }.recoverWith {
                case error: SQLException if error.getSQLState == "23505" =>
                  Conflict(FailureResponse("That email or username is already in use"))
                case error: SQLException =>
                  Logger[F].error(error)("Failed to create managed user") *>
                    InternalServerError(FailureResponse("Could not create the user"))
              }
        }
  }

  private val updateUserRoute: AuthRoute[F] = {
    case req @ PUT -> Root asAuthed user =>
      if !user.isAdmin then Forbidden(FailureResponse("Only administrators can update users"))
      else
        req.request.validate[ManagedUserForm] { form =>
          val hashedPassword = form.password.traverse(BCrypt.hashpw[F])
          hashedPassword.flatMap { password =>
            users.updateManaged(form.copy(password = password)).flatMap {
              case Some(updated) => Ok(updated)
              case None => NotFound(FailureResponse(s"User ${form.email} was not found"))
            }.recoverWith {
              case error: SQLException if error.getSQLState == "23505" =>
                Conflict(FailureResponse("That username is already in use"))
              case error: SQLException =>
                Logger[F].error(error)("Failed to update managed user") *>
                  InternalServerError(FailureResponse("Could not update the user"))
            }
          }
        }
  }

  private val deleteUserRoute: AuthRoute[F] = {
    case req @ DELETE -> Root :? EmailQuery(email) asAuthed user =>
      if !user.isAdmin then Forbidden(FailureResponse("Only administrators can delete users"))
      else email match
        case None => BadRequest(FailureResponse("An email address is required"))
        case Some(targetEmail) if targetEmail.equalsIgnoreCase(user.email) =>
          BadRequest(FailureResponse("You cannot delete your own account"))
        case Some(targetEmail) =>
          users.delete(targetEmail).flatMap {
            case true => Ok()
            case false => NotFound(FailureResponse(s"User $targetEmail was not found"))
          }
  }

  private object SearchQuery extends OptionalQueryParamDecoderMatcher[String]("search")
  private object LimitQuery extends OptionalQueryParamDecoderMatcher[String]("limit")
  private object SkipQuery extends OptionalQueryParamDecoderMatcher[String]("skip")
  private object EmailQuery extends OptionalQueryParamDecoderMatcher[String]("email")

  val routes: HttpRoutes[F] =
    SecuredHandler[F].liftService(
      listUsersRoute.restrictedTo(adminOnly) |+|
        createUserRoute.restrictedTo(adminOnly) |+|
        updateUserRoute.restrictedTo(adminOnly) |+|
        deleteUserRoute.restrictedTo(adminOnly)
    )

object UserManagementRoutes:
  def apply[F[_]: Async: Logger: SecuredHandler](users: Users[F]): UserManagementRoutes[F] =
    new UserManagementRoutes[F](users)
