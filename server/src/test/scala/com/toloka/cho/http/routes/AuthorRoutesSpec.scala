package com.toloka.cho.http.routes

import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.http4s.dsl.Http4sDsl

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import java.util.UUID
import java.{util => ju}


import com.toloka.cho.fixtures.*
import com.toloka.cho.admin.http.routes.AuthRoutes
import com.toloka.cho.domain.pagination.*
import com.toloka.cho.admin.http.routes.*
import com.toloka.cho.admin.core.Authors
import com.toloka.cho.domain.AuthorInfo
import com.toloka.cho.domain.Author


class AuthorRoutesSpec 
   extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with AuthorFixture
    with SecuredRouteFixture
    {

        val authors: Authors[IO] = new Authors[IO] {

        override def find(pattern: String): IO[List[Author]] =  
            if (existingAuthor.authorInfo.firstName.exists(_.contains(pattern)) ||
            existingAuthor.authorInfo.lastName.contains(pattern)) 
                IO.pure(List(existingAuthor))
            else 
                IO.pure(List.empty)

        override def all(): fs2.Stream[IO, Author] = ???

        override def create(authorInfo: AuthorInfo): IO[ju.UUID] = IO.pure(existingAuthorUuid)

        override def find(id: ju.UUID): IO[Option[Author]] = 
            if (id == existingAuthorUuid) 
                IO.pure(Some(existingAuthor))
            else 
                IO.pure(None)

        override def update(id: ju.UUID, authorInfo: AuthorInfo): IO[Option[Author]] = 
            if (id == existingAuthorUuid)
                IO.pure(Some(existingAuthor))
            else
                IO.pure(None)

        override def delete(id: ju.UUID): IO[Int] = 
            if (id == existingAuthorUuid) IO.pure(1)
            else IO.pure(0)

        }

        given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
        val authorRoutes: HttpRoutes[IO] = AuthorRoutes[IO](authors).routes


        "BookRoutes" - {
            "should return a author with a given id" in {
                for {
                    response <- authorRoutes.orNotFound.run(
                    Request(method = Method.GET, uri = uri"/authors/11111111-1111-1111-1111-111111111111")
                    )
                    retrieved <- response.as[Author]
                } yield {
                    response.status shouldBe Status.Ok
                    retrieved shouldBe existingAuthor
                }
            }
        }

        "should return an author by name" in {
            for {
                response <- authorRoutes.orNotFound.run(
                Request(method = Method.GET, uri = uri"/authors/search?query=Jan")
                )
                retrieved <- response.as[List[Author]]
            } yield {
                response.status shouldBe Status.Ok
                retrieved shouldBe List(existingAuthor)
            }
        }

        "should return an author by last" in {
            for {
                response <- authorRoutes.orNotFound.run(
                Request(method = Method.GET, uri = uri"/authors/search?query=Do")
                )
                retrieved <- response.as[List[Author]]
            } yield {
                response.status shouldBe Status.Ok
                retrieved shouldBe List(existingAuthor)
            }
         }

        "should create a new author" in {
            for {
                jwtToken <- mockedAuthenticator.create(remiEmail)
                response <- authorRoutes.orNotFound.run(
                Request(method = Method.POST, uri = uri"/authors/create")
                    .withEntity(existingAuthor.authorInfo)
                    .withBearerToken(jwtToken)
                )
                retrieved <- response.as[UUID]
            } yield {
                response.status shouldBe Status.Created
                retrieved shouldBe existingAuthorUuid
            }
        }

        "should only update a author that exists with the JWT token" in {
            for {
                jwtToken <- mockedAuthenticator.create(remiEmail)
                responseOk <- authorRoutes.orNotFound.run(
                Request(method = Method.PUT, uri = uri"/authors/11111111-1111-1111-1111-111111111111")
                    .withEntity(existingAuthor.authorInfo)
                    .withBearerToken(jwtToken)
                )
                responseInvalid <- authorRoutes.orNotFound.run(
                Request(method = Method.PUT, uri = uri"/authors/99999999-9999-9999-9999-999999999999")
                    .withEntity(existingAuthor.authorInfo)
                    .withBearerToken(jwtToken)
                )
            } yield {
                responseOk.status shouldBe Status.Ok
                responseInvalid.status shouldBe Status.NotFound
            }
        }

        "should forbid to update a author that the JWT Token does not own" in {
            for {
                jwtToken <- mockedAuthenticator.create("somebody@gmail.com")
                response <- authorRoutes.orNotFound.run(
                    Request[IO](method = Method.PUT, uri = uri"/authors/11111111-1111-1111-1111-111111111111")
                    .withEntity(existingAuthor.authorInfo)
                    .withBearerToken(jwtToken)
                )
                } yield {
                response.status shouldBe Status.Unauthorized
            }
        }

        "should only delete a author that exists" in {
            for {
                jwtToken <- mockedAuthenticator.create(remiEmail)
                responseOk <- authorRoutes.orNotFound.run(
                Request(method = Method.DELETE, uri = uri"/authors/11111111-1111-1111-1111-111111111111")
                    .withEntity(existingAuthor.authorInfo)
                    .withBearerToken(jwtToken)
                )
                responseInvalid <- authorRoutes.orNotFound.run(
                Request(method = Method.DELETE, uri = uri"/authors/99999999-9999-9999-9999-999999999999")
                    .withEntity(existingAuthor.authorInfo)
                    .withBearerToken(jwtToken)
                )
            } yield {
                responseOk.status shouldBe Status.Ok
                responseInvalid.status shouldBe Status.NotFound
            }
        }

    }
