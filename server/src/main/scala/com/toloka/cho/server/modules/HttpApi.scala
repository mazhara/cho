package com.toloka.cho.server.modules

import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.Async
import cats.implicits.*
import com.toloka.cho.server.config.SecurityConfig
import com.toloka.cho.server.core.Users
import com.toloka.cho.server.http.routes.{AdminRoutes, AuthRoutes, AuthorRoutes, BookRoutes, HealthRoutes, EventRoutes, UserManagementRoutes}
import com.toloka.cho.domain.security.*
import com.toloka.cho.domain.user.User
import org.http4s.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.authentication.{BackingStore, IdentityStore, JWTAuthenticator, SecuredRequestHandler}
import tsec.common.SecureRandomId
import tsec.mac.jca.HMACSHA256

class HttpApi[F[_]: Async: Logger] private (core: Core[F], authenticator: Authenticator[F]) {
  given securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes              = HealthRoutes[F].routes
  private val bookRoutes                 = BookRoutes[F](core.books).routes
  private val authorRoutes                 = AuthorRoutes[F](core.authors).routes
  private val eventRoutes                  = EventRoutes[F](core.events).routes
  private val authRoutes = AuthRoutes[F](core.auth, authenticator).routes
  private val adminRoutes = AdminRoutes[F](core.users, core.books).routes
  private val userManagementRoutes = UserManagementRoutes[F](core.users).routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> bookRoutes <+> authorRoutes <+> eventRoutes <+> authRoutes <+> adminRoutes <+>
      Router("/admin/users" -> userManagementRoutes))
  )
}


object HttpApi {
  def createAuthenticator[F[_]: Sync](
      users: Users[F],
      securityConfig: SecurityConfig
  ): F[Authenticator[F]] = {
    val idStore: IdentityStore[F, String, User] = (email: String) => OptionT(users.find(email))

    // Keep the credential snapshot server-side, never in the JWT sent to the browser.
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, (JwtToken, String)]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] =
          for {
            entry <- OptionT(ref.get.map(_.get(id)))
            (token, passwordHash) = entry
            user <- OptionT(users.find(token.identity))
            valid <- OptionT.pure[F](token).filter(_ => user.hashedPassword == passwordHash)
          } yield valid

        override def put(elem: JwtToken): F[JwtToken] =
          users.find(elem.identity).flatMap {
            case Some(user) =>
              ref.update(_ + (elem.id -> (elem, user.hashedPassword))).as(elem)
            case None =>
              Sync[F].raiseError(new IllegalStateException("Cannot create a session for a missing user"))
          }

        override def update(v: JwtToken): F[JwtToken] =
          ref.modify { store =>
            store.get(v.id) match {
              case Some((_, passwordHash)) =>
                (store + (v.id -> (v, passwordHash)), Right(v))
              case None =>
                (store, Left(new IllegalStateException("Cannot update a missing session")))
            }
          }.flatMap(_.liftTo[F])

        override def delete(id: SecureRandomId): F[Unit] = ref.modify(store => (store - id, ()))
      }
    }

    val keyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))

    for {
      key        <- keyF
      tokenStore <- tokenStoreF
    } yield JWTAuthenticator.backed.inBearerToken(
      expiryDuration = securityConfig.jwtExpiryDuration, // Token expiration
      maxIdle = None,                                    // Max idle time (optional)
      identityStore = idStore,                           // Id Store
      tokenStore = tokenStore,                           // Hash key
      signingKey = key
    )
  }

  def apply[F[_]: Async: Logger](
      core: Core[F],
      securityConfig: SecurityConfig
  ): Resource[F, HttpApi[F]] =
    Resource
      .eval(createAuthenticator(core.users, securityConfig))
      .map(authenticator => new HttpApi[F](core, authenticator))

}
