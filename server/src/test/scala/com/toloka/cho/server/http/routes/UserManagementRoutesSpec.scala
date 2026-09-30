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

final class UserManagementRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with UserFixture
    with SecuredRouteFixture:

  given Logger[IO] = Slf4jLogger.getLogger[IO]
  private val routes = UserManagementRoutes[IO](mockedUsers).routes

  "User management routes" - {
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
