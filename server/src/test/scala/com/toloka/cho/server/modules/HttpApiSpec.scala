package com.toloka.cho.server.modules

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.toloka.cho.domain.auth.NewPasswordInfo
import com.toloka.cho.domain.security.*
import com.toloka.cho.domain.user.User
import com.toloka.cho.server.config.SecurityConfig
import com.toloka.cho.server.core.{Auth, Emails, LiveAuth, Tokens, Users}
import com.toloka.cho.server.fixtures.UserFixture
import com.toloka.cho.server.http.routes.AuthRoutes
import org.http4s.*
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.SecuredRequestHandler
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256
import scala.concurrent.duration.*

class HttpApiSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UserFixture {
  given Logger[IO] = Slf4jLogger.getLogger[IO]

  private def checkSessions(change: Auth[IO] => IO[Boolean], revoked: Boolean): IO[org.scalatest.Assertion] =
    for {
      state <- Ref.of[IO, Map[String, User]](Map(Remi.email -> Remi, Gaston.email -> Gaston))
      users = new Users[IO] {
        def count(): IO[Long] = state.get.map(_.size.toLong)
        def find(email: String): IO[Option[User]] = state.get.map(_.get(email))
        def create(user: User): IO[String] = state.update(_ + (user.email -> user)).as(user.email)
        def update(user: User): IO[Option[User]] =
          state.update(_ + (user.email -> user)).as(Some(user))
        def delete(email: String): IO[Boolean] = state.update(_ - email).as(true)
      }
      recoveryToken <- Ref.of[IO, Boolean](true)
      tokens = new Tokens[IO] {
        def getToken(email: String): IO[Option[String]] = IO.pure(Some("recovery-code"))
        def checkToken(email: String, token: String): IO[Boolean] =
          recoveryToken.get.map(valid => valid && token == "recovery-code")
        def invalidateToken(email: String): IO[Unit] = recoveryToken.set(false)
      }
      emails = new Emails[IO] {
        def sendEmail(to: String, subject: String, content: String): IO[Unit] = IO.unit
        def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] = IO.unit
      }
      auth <- LiveAuth[IO](users, tokens, emails)
      authenticator <- HttpApi.createAuthenticator[IO](users, SecurityConfig("test-session-signing-key", 1.day))
      first <- authenticator.create(remiEmail)
      second <- authenticator.create(remiEmail)
      other <- authenticator.create(gastonEmail)
      routes = {
        given SecuredHandler[IO] = SecuredRequestHandler(authenticator)
        AuthRoutes[IO](auth, authenticator).routes.orNotFound
      }
      check = (token: JwtToken) =>
        routes.run(
          Request[IO](uri = uri"/auth/checkToken").putHeaders(
            Authorization(Credentials.Token(AuthScheme.Bearer, JWTMac.toEncodedString[IO, HMACSHA256](token.jwt)))
          )
        ).map(_.status)
      before <- check(first)
      changed <- change(auth)
      afterFirst <- check(first)
      afterSecond <- check(second)
      afterOther <- check(other)
      fresh <- authenticator.create(remiEmail)
      afterFresh <- check(fresh)
    } yield {
      before shouldBe Status.Ok
      changed shouldBe revoked
      afterFirst shouldBe (if revoked then Status.Unauthorized else Status.Ok)
      afterSecond shouldBe (if revoked then Status.Unauthorized else Status.Ok)
      afterOther shouldBe Status.Ok
      afterFresh shouldBe Status.Ok
    }

  "Sessions" - {
    "reject all existing account sessions after password recovery but allow a new login" in {
      checkSessions(_.recoverPasswordFromToken(remiEmail, "recovery-code", "replacement-password"), revoked = true)
    }

    "reject all existing account sessions after an authenticated password change" in {
      checkSessions(
        _.changePassword(remiEmail, NewPasswordInfo(remiPassword, "replacement-password"))
          .map(_.exists(_.nonEmpty)),
        revoked = true
      )
    }

    "preserve sessions when recovery fails" in {
      checkSessions(_.recoverPasswordFromToken(remiEmail, "wrong-code", "replacement-password"), revoked = false)
    }

    "preserve sessions when the old password is incorrect" in {
      checkSessions(
        _.changePassword(remiEmail, NewPasswordInfo("wrong-password", "replacement-password"))
          .map(_.exists(_.nonEmpty)),
        revoked = false
      )
    }
  }
}
