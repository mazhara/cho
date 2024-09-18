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
import com.toloka.cho.admin.domain.user.NewUserInfo
import com.toloka.cho.admin.domain.book.*
import com.toloka.cho.admin.http.routes.AuthRoutes
import com.toloka.cho.admin.core.Books
import com.toloka.cho.admin.domain.pagination.Pagination
import com.toloka.cho.admin.http.routes.BookRoutes


class BookRoutesSpec 
   extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with BookFixture
    with SecuredRouteFixture
    {

        val books: Books[IO] = new Books[IO] {

        override def create(bookInfo: BookInfo): IO[ju.UUID] = IO.pure(NewBookUuid)

        override def all(): IO[List[Book]] = IO.pure(List(AwesomeBook))

        override def all(filter: BookFilter, pagination: Pagination): IO[List[Book]] =
            if (filter.inHallOnly) IO.pure(List())
            else IO.pure(List(AwesomeBook))


        override def find(id: ju.UUID): IO[Option[Book]] = 
            if (id == AwesomeBookUuid) 
                IO.pure(Some(AwesomeBook))
            else 
                IO.pure(None)

        override def update(id: ju.UUID, bookInfo: BookInfo): IO[Option[Book]] = 
            if (id == AwesomeBookUuid)
                IO.pure(Some(UpdatedAwesomeBook))
            else
                IO.pure(None)

        override def delete(id: ju.UUID): IO[Int] = 
            if (id == AwesomeBookUuid) IO.pure(1)
            else IO.pure(0)

        }

        given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
        val bookRoutes: HttpRoutes[IO] = BookRoutes[IO](books).routes

        "BookRoutes" - {
            "should return a book with a given id" in {
                for {
                    response <- bookRoutes.orNotFound.run(
                    Request(method = Method.GET, uri = uri"/books/843df718-ec6e-4d49-9289-f799c0f40064")
                    )
                    retrieved <- response.as[Book]
                } yield {
                    response.status shouldBe Status.Ok
                    retrieved shouldBe AwesomeBook
                }
            }
        }

        "should return all books" in {
            for {
                response <- bookRoutes.orNotFound.run(
                Request(method = Method.POST, uri = uri"/books")
                .withEntity(BookFilter())
                )
                retrieved <- response.as[List[Book]]
            } yield {
                response.status shouldBe Status.Ok
                retrieved shouldBe List(AwesomeBook)
            }
        }

        "should return all books that satisfy a filter" in {
            for {
                response <- bookRoutes.orNotFound.run(
                Request(method = Method.POST, uri = uri"/books")
                    .withEntity(BookFilter(inHallOnly = true))
                )
                retrieved <- response.as[List[Book]]
            } yield {
                response.status shouldBe Status.Ok
                retrieved shouldBe List()
            }
        }


        "should create a new book" in {
            for {
                jwtToken <- mockedAuthenticator.create(remiEmail)
                response <- bookRoutes.orNotFound.run(
                Request(method = Method.POST, uri = uri"/books/create")
                    .withEntity(AwesomeBook.bookInfo)
                    .withBearerToken(jwtToken)
                )
                retrieved <- response.as[UUID]
            } yield {
                response.status shouldBe Status.Created
                retrieved shouldBe NewBookUuid
            }
        }

        "should only update a book that exists with the JWT token" in {
            for {
                jwtToken <- mockedAuthenticator.create(remiEmail)
                responseOk <- bookRoutes.orNotFound.run(
                Request(method = Method.PUT, uri = uri"/books/843df718-ec6e-4d49-9289-f799c0f40064")
                    .withEntity(UpdatedAwesomeBook.bookInfo)
                    .withBearerToken(jwtToken)
                )
                responseInvalid <- bookRoutes.orNotFound.run(
                Request(method = Method.PUT, uri = uri"/books/843df718-ec6e-4d49-9289-000000000000")
                    .withEntity(UpdatedAwesomeBook.bookInfo)
                    .withBearerToken(jwtToken)
                )
            } yield {
                responseOk.status shouldBe Status.Ok
                responseInvalid.status shouldBe Status.NotFound
            }
        }

        "should forbid to update a job that the JWT Token does not own" in {
            for {
                jwtToken <- mockedAuthenticator.create("somebody@gmail.com")
                response <- bookRoutes.orNotFound.run(
                    Request[IO](method = Method.PUT, uri = uri"/books/843df718-ec6e-4d49-9289-f799c0f40064")
                    .withEntity(UpdatedAwesomeBook.bookInfo)
                    .withBearerToken(jwtToken)
                )
                } yield {
                response.status shouldBe Status.Unauthorized
            }
        }

        "should only delete a job that exists" in {
            for {
                jwtToken <- mockedAuthenticator.create(remiEmail)
                responseOk <- bookRoutes.orNotFound.run(
                Request(method = Method.DELETE, uri = uri"/books/843df718-ec6e-4d49-9289-f799c0f40064")
                    .withEntity(UpdatedAwesomeBook.bookInfo)
                    .withBearerToken(jwtToken)
                )
                responseInvalid <- bookRoutes.orNotFound.run(
                Request(method = Method.DELETE, uri = uri"/books/843df718-ec6e-4d49-9289-000000000000")
                    .withEntity(UpdatedAwesomeBook.bookInfo)
                    .withBearerToken(jwtToken)
                )
            } yield {
                responseOk.status shouldBe Status.Ok
                responseInvalid.status shouldBe Status.NotFound
            }
        }


    }