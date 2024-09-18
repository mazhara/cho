package com.toloka.cho.modules

import cats.* 
import cats.implicits.*
import cats.effect.*
import cats.data.*
import org.http4s.* 
import org.http4s.dsl.* 
import org.http4s.server.* 


import tsec.authentication.AugmentedJWT
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import tsec.authentication.JWTAuthenticator
import tsec.authentication.BackingStore
import tsec.common.SecureRandomId

import com.toloka.cho.admin.http.routes.BookRoutes
import com.toloka.cho.admin.http.routes.HealthRoutes
import org.typelevel.log4cats.Logger
import com.toloka.cho.admin.http.routes.AuthRoutes
import com.toloka.cho.domain.security.*
import tsec.authentication.SecuredRequestHandler
import com.toloka.cho.admin.config.SecurityConfig
import com.toloka.cho.admin.core.Users
import com.toloka.cho.domain.user.User

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]) {
  given securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes              = HealthRoutes[F].routes
  private val bookRoutes                 = BookRoutes[F](core.books).routes
  private val authRoutes = AuthRoutes[F](core.auth, authenticator).routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> bookRoutes <+> authRoutes)
  )
}


object HttpApi {
  def createAuthenticator[F[_]: Sync](
      users: Users[F],
      securityConfig: SecurityConfig
  ): F[Authenticator[F]] = {
    val idStore: IdentityStore[F, String, User] = (email: String) => OptionT(users.find(email))

    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] = OptionT(ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def update(v: JwtToken): F[JwtToken] = put(v)

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
