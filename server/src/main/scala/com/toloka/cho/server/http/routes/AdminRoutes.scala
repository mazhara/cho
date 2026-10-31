package com.toloka.cho.server.http.routes

import cats.effect.*
import cats.implicits.*
import com.toloka.cho.domain.admin.AdminDashboardStats
import com.toloka.cho.domain.security.*
import com.toloka.cho.server.core.{Books, Users}
import com.toloka.cho.server.http.responses.{ApiError, ErrorResponse}
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.authentication.asAuthed
import tsec.authentication.SecuredRequestHandler
import scala.language.implicitConversions

final class AdminRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (
    users: Users[F],
    books: Books[F]
) extends Http4sDsl[F] {
  private val dashboardStatsRoute: AuthRoute[F] = {
    case GET -> Root / "stats" asAuthed user =>
      if (user.isAdmin || user.isLibrarian)
        (users.count(), books.copyCount()).mapN(AdminDashboardStats.apply).attempt.flatMap {
          case Right(stats) => Ok(stats)
          case Left(error) =>
            InternalServerError(
              ErrorResponse.fromApiError(
                ApiError.InternalServerError(s"Failed to load admin dashboard statistics: ${error.getMessage}")
              )
            )
        }
      else
        Forbidden(ErrorResponse.fromApiError(ApiError.AuthorizationError("Only admins and librarians can view dashboard statistics")))
  }

  val routes: HttpRoutes[F] =
    Router(
      "/admin" -> SecuredHandler[F].liftService(
        dashboardStatsRoute.restrictedTo(staffOnly)
      )
    )
}

object AdminRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](users: Users[F], books: Books[F]): AdminRoutes[F] =
    new AdminRoutes[F](users, books)
}
