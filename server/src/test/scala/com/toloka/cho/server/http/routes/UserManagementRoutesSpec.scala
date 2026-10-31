package com.toloka.cho.server.http.routes

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.toloka.cho.server.http.routes.UserManagementRoutes
import com.toloka.cho.domain.ManagedUserForm
import io.circe.generic.auto.*
import com.toloka.cho.server.fixtures.{SecuredRouteFixture, UserFixture}
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.toloka.cho.server.core.Users
import com.toloka.cho.domain.ManagedUser
import com.toloka.cho.domain.user.User
import java.sql.SQLException
import com.toloka.cho.server.core.{DoobieSpec, LiveUsers}
import org.http4s.server.Router
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

final class UserManagementRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with UserFixture
    with DoobieSpec
    with SecuredRouteFixture:

  override val initScript = "sql/users.sql"
  given Logger[IO] = Slf4jLogger.getLogger[IO]
  private val routes = UserManagementRoutes[IO](mockedUsers).routes

  private def failingUpdateRoutes(error: SQLException): HttpRoutes[IO] =
    UserManagementRoutes[IO](new Users[IO] {
      def count(): IO[Long] = mockedUsers.count()
      def find(email: String): IO[Option[User]] = mockedUsers.find(email)
      def create(user: User): IO[String] = mockedUsers.create(user)
      def update(user: User): IO[Option[User]] = mockedUsers.update(user)
      def delete(email: String): IO[Boolean] = mockedUsers.delete(email)
      def allManaged(search: String, limit: Int, skip: Int): IO[List[ManagedUser]] =
        mockedUsers.allManaged(search, limit, skip)
      def createManaged(form: ManagedUserForm, hashedPassword: String): IO[Option[ManagedUser]] =
        mockedUsers.createManaged(form, hashedPassword)
      def updateManaged(form: ManagedUserForm): IO[Option[ManagedUser]] = IO.raiseError(error)
    }).routes

  private val validUpdate = ManagedUserForm(
    "reader@example.test", "existing_username", Some("Test"), Some("Reader"), None, None, "READER", None
  )

  "User management routes" - {
    "should create, list and reassign readers and librarians through the mounted API with real persistence" in {
      transactor.use { xa =>
        for
          users <- LiveUsers[IO](xa)
          api = Router("/api/admin/users" -> UserManagementRoutes[IO](users).routes).orNotFound
          token <- mockedAuthenticator.create(remiEmail)
          readerForm = validUpdate.copy(username = "new_reader", password = Some("Reader123!"))
          createdResponse <- api.run(
            Request[IO](Method.POST, uri"/api/admin/users").withEntity(readerForm).withBearerToken(token))
          created <- createdResponse.as[ManagedUser]
          stored <- users.find(readerForm.email).map(_.get)
          passwordMatches <- BCrypt.checkpwBool[IO]("Reader123!", PasswordHash[BCrypt](stored.hashedPassword))
          promotionResponse <- api.run(
            Request[IO](Method.PUT, uri"/api/admin/users")
              .withEntity(readerForm.copy(role = "LIBRARIAN", password = None)).withBearerToken(token))
          promoted <- promotionResponse.as[ManagedUser]
          afterPromotion <- users.find(readerForm.email).map(_.get)
          demotionResponse <- api.run(
            Request[IO](Method.PUT, uri"/api/admin/users")
              .withEntity(readerForm.copy(password = None)).withBearerToken(token))
          demoted <- demotionResponse.as[ManagedUser]
          librarianResponse <- api.run(
            Request[IO](Method.POST, uri"/api/admin/users")
              .withEntity(readerForm.copy(email = "librarian@example.test", username = "new_librarian", role = "LIBRARIAN"))
              .withBearerToken(token))
          librarian <- librarianResponse.as[ManagedUser]
          listResponse <- api.run(
            Request[IO](Method.GET, uri"/api/admin/users?search=example.test").withBearerToken(token))
          listed <- listResponse.as[List[ManagedUser]]
          duplicateResponse <- api.run(
            Request[IO](Method.POST, uri"/api/admin/users").withEntity(readerForm).withBearerToken(token))
          conflictResponse <- api.run(
            Request[IO](Method.PUT, uri"/api/admin/users")
              .withEntity(readerForm.copy(username = "NEW_LIBRARIAN", password = None)).withBearerToken(token))
          deniedResponse <- mockedAuthenticator.create(gastonEmail).flatMap { staffToken =>
            api.run(Request[IO](Method.POST, uri"/api/admin/users").withEntity(readerForm).withBearerToken(staffToken))
          }
        yield
          createdResponse.status shouldBe Status.Created
          created.role shouldBe "READER"
          passwordMatches shouldBe true
          stored.hashedPassword should not be "Reader123!"
          promotionResponse.status shouldBe Status.Ok
          promoted.role shouldBe "LIBRARIAN"
          afterPromotion.hashedPassword shouldBe stored.hashedPassword
          demotionResponse.status shouldBe Status.Ok
          demoted.role shouldBe "READER"
          librarianResponse.status shouldBe Status.Created
          librarian.role shouldBe "LIBRARIAN"
          listed.map(_.email).toSet shouldBe Set(readerForm.email, librarian.email)
          duplicateResponse.status shouldBe Status.Conflict
          conflictResponse.status shouldBe Status.Conflict
          deniedResponse.status shouldBe Status.Unauthorized
      }
    }

    "should return a conflict when an updated username is already occupied" in {
      for
        token <- mockedAuthenticator.create(remiEmail)
        response <- failingUpdateRoutes(new SQLException("duplicate username", "23505")).orNotFound.run(
          Request[IO](Method.PUT, uri"/").withEntity(validUpdate).withBearerToken(token)
        )
        body <- response.bodyText.compile.string
      yield
        response.status shouldBe Status.Conflict
        body should include("That username is already in use")
    }

    "should not disguise unrelated database failures as username conflicts" in {
      val error = new SQLException("connection failed", "08006")
      for
        token <- mockedAuthenticator.create(remiEmail)
        response <- failingUpdateRoutes(error).orNotFound.run(
          Request[IO](Method.PUT, uri"/").withEntity(validUpdate).withBearerToken(token)
        )
      yield response.status shouldBe Status.InternalServerError
    }

    "should allow an administrator to list users" in {
      for
        token <- mockedAuthenticator.create(remiEmail)
        response <- routes.orNotFound.run(
          Request[IO](Method.GET, uri"/?search=reader&limit=10&skip=0").withBearerToken(token)
        )
      yield response.status shouldBe Status.Ok
    }

    "should reject invalid pagination parameters" in {
      for
        token <- mockedAuthenticator.create(remiEmail)
        response <- routes.orNotFound.run(
          Request[IO](Method.GET, uri"/?limit=all&skip=-1").withBearerToken(token)
        )
      yield response.status shouldBe Status.BadRequest
    }

    "should forbid librarians from listing users" in {
      for
        token <- mockedAuthenticator.create(gastonEmail)
        response <- routes.orNotFound.run(
          Request[IO](Method.GET, uri"/").withBearerToken(token)
        )
      yield response.status shouldBe Status.Unauthorized
    }

    "should prevent administrators from deleting their own account" in {
      for
        token <- mockedAuthenticator.create(remiEmail)
        response <- routes.orNotFound.run(
          Request[IO](Method.DELETE, Uri.unsafeFromString(s"/?email=$remiEmail")).withBearerToken(token)
        )
      yield response.status shouldBe Status.BadRequest
    }

    "should reject user creation with an invalid username" in {
      for
        token <- mockedAuthenticator.create(remiEmail)
        response <- routes.orNotFound.run(
          Request[IO](Method.POST, uri"/")
            .withEntity(
              ManagedUserForm(
                "new.user@example.test",
                "bad username",
                Some("New"),
                Some("User"),
                None,
                None,
                "READER",
                Some("long-enough-password")
              )
            )
            .withBearerToken(token)
        )
      yield response.status shouldBe Status.BadRequest
    }
  }
