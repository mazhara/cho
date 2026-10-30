package com.toloka.cho.http.routes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import com.toloka.cho.domain.auth.*
import com.toloka.cho.domain.security.*
import com.toloka.cho.domain.user.User
import com.toloka.cho.server.core.Auth
import com.toloka.cho.server.fixtures.{SecuredRouteFixture, UserFixture}
import com.toloka.cho.server.http.routes.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


class AuthRoutesSpec
  extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with UserFixture
    with SecuredRouteFixture {

  val mockedAuth: Auth[IO] = probedAuth(None)


  def probedAuth(userMap: Option[Ref[IO, Map[String, String]]]): Auth[IO] = new Auth[IO] {
    def login(email: String, password: String): IO[Option[User]] =
      if (email == remiEmail && password == remiPassword)
        IO(Some(Remi))
      else IO.pure(None)

    def signUp(newUserInfo: NewUserInfo): IO[Option[User]] =
      if (newUserInfo.email == gastonEmail)
        IO.pure(Some(Gaston))
      else
        IO.pure(None)

    def changePassword(
                        email: String,
                        newPasswordInfo: NewPasswordInfo
                      ): IO[Either[String, Option[User]]] =
      if (email == remiEmail)
        if (newPasswordInfo.oldPassword == remiPassword)
          IO.pure(Right(Some(Remi)))
        else
          IO.pure(Left("Invalid password"))
      else
        IO.pure(Right(None))

    def delete(email: String): IO[Boolean] = IO.pure(true)

    def sendPasswordRecoveryToken(email: String): IO[Unit] =
      userMap
        .traverse { userMapRef =>
          userMapRef.modify { userMap =>
            (userMap + (email -> "abc123"), ())
          }
        }
        .map(_ => ())

    def recoverPasswordFromToken(
                                  email: String,
                                  token: String,
                                  newPassword: String
                                ): IO[Boolean] =
      userMap
        .traverse { userMapRef =>
          userMapRef.get
            .map { userMap =>
              userMap.get(email).filter(_ == token)
            }
            .map(_.nonEmpty)
        }
        .map(_.getOrElse(false))


    def authenticator: Authenticator[IO] = mockedAuthenticator
  }

  given logger: Logger[IO]       = Slf4jLogger.getLogger[IO]
  val authRoutes: HttpRoutes[IO] =  AuthRoutes[IO](mockedAuth, mockedAuthenticator).routes

  "AuthRoutes" - {
    "should return a 401 - Unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(remiEmail, "wrongpassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok + a JWT Token if login is successful" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(remiEmail, remiPassword))
        )
      } yield {
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return a 400 - Bad Request if the user to create already exists" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserRemi.copy(password = "Secure123!", dataProcessingConsent = true))
        )
      } yield {
        response.status shouldBe Status.BadRequest
      }
    }

    "should return a 201 - Created if the user was successfuly created" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserGaston.copy(password = "Secure123!"))
        )
      } yield {
        response.status shouldBe Status.Created
      }
    }

    "should reject sign-up when personal data processing consent is missing" in {
      val request: Request[IO] = Request(method = Method.POST, uri = uri"/auth/users")
        .withEntity(
          NewUserInfo(
            "new.reader@example.test",
            "Secure123!",
            Some("New"),
            Some("Reader"),
            None,
            Some("+33 6 11 22 33 44"),
            dataProcessingConsent = false
          )
        )

      authRoutes.orNotFound.run(request).map(_.status shouldBe Status.BadRequest)
    }

    "should reject invalid phone numbers before creating an account" in {
      List("123", "0612345678", "+11111111111", "+380123456789", "+33612345678abc").traverse { phone =>
        val request: Request[IO] = Request(method = Method.POST, uri = uri"/auth/users")
          .withEntity(NewUserGaston.copy(password = "Secure123!", phone = Some(phone)))
        authRoutes.orNotFound.run(request).map(_.status shouldBe Status.BadRequest)
      }.map(_ => succeed)
    }

    "should enforce password policy on sign-up, changes, and recovery" in {
      val weakPasswords = List("Ab1!abc", "abcdefgh1!", "ABCDEFGH1!", "Abcdefgh!", "Abcdefgh1", "Ab1!" + "a" * 61)
      for {
        jwtToken <- mockedAuthenticator.create(remiEmail)
        _ <- weakPasswords.traverse { password =>
          List(
            Request[IO](method = Method.POST, uri = uri"/auth/users")
              .withEntity(NewUserGaston.copy(password = password)),
            Request[IO](method = Method.POST, uri = uri"/auth/recover")
              .withEntity(RecoverPasswordInfo(remiEmail, "abc123", password)),
            Request[IO](method = Method.PUT, uri = uri"/auth/users/password")
              .withBearerToken(jwtToken)
              .withEntity(NewPasswordInfo(remiPassword, password))
          ).traverse(request => authRoutes.orNotFound.run(request).map(_.status shouldBe Status.BadRequest))
        }
      } yield succeed
    }

    "should return a 401 - Unauthorized if login out without a valid JWT Token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok if login out with a valid JWT Token" in {
      for {
        jwtToken <- mockedAuthenticator.create(remiEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 404 - Not Found if the user does not exist" in {
      for {
        jwtToken <- mockedAuthenticator.create(gastonEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(gastonPassword, "NewPassword1!"))
        )
      } yield {
        response.status shouldBe Status.NotFound
      }
    }

    "should return a 403 - Forbidden if the old password is incorrect" in {
      for {
        jwtToken <- mockedAuthenticator.create(remiEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("wrongPassword", "NewPassword1!"))
        )
      } yield {
        response.status shouldBe Status.Forbidden
      }
    }

    "should return a 401 - Unauthorized if the JWT Token is invalid" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(remiPassword, "NewPassword1!"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok if the password change succeed" in {
      for {
        jwtToken <- mockedAuthenticator.create(remiEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(remiPassword, "NewPassword1!"))
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthorized if a non admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(gastonEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/cornet.remi@corem.corp")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok if an admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(remiEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/cornet.remi@corem.corp")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 200 - Ok when resetting a password, and an email should be triggered" in {
      for {
        userMapRef <- Ref.of[IO, Map[String, String]](Map())
        auth       <- IO(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/reset")
            .withEntity(ForgotPasswordInfo(remiEmail))
        )
        userMap <- userMapRef.get
      } yield {
        response.status shouldBe Status.Ok
        userMap should contain key (remiEmail)
      }
    }

    "should reject a password recovery request with a password shorter than eight characters" in {
      val routes = AuthRoutes(mockedAuth, mockedAuthenticator).routes
      val request: Request[IO] = Request(method = Method.POST, uri = uri"/auth/recover")
        .withEntity(RecoverPasswordInfo(remiEmail, "abc123", "short"))

      routes.orNotFound.run(request).map(_.status shouldBe Status.BadRequest)
    }

    "should return a 200 - Ok when recovering a password for a user/token combination" in {
      for {
        userMapRef <- Ref.of[IO, Map[String, String]](Map(remiEmail -> "abc123"))
        auth       <- IO(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/recover")
            .withEntity(RecoverPasswordInfo(remiEmail, "abc123", "NewPassword1!"))
        )
        userMap <- userMapRef.get
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 403 - Forbidden when recovering a password for a user with an incorrect token" in {
      for {
        userMapRef <- Ref.of[IO, Map[String, String]](Map(remiEmail -> "abc123"))
        auth       <- IO(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/recover")
            .withEntity(RecoverPasswordInfo(remiEmail, "wrongToken", "NewPassword1!"))
        )
        userMap <- userMapRef.get
      } yield {
        response.status shouldBe Status.Forbidden
      }
    }
  }
}
