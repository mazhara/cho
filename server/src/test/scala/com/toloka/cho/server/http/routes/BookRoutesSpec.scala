package com.toloka.cho.server.http.routes

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


import com.toloka.cho.server.fixtures.*
import com.toloka.cho.server.http.routes.AuthRoutes
import com.toloka.cho.server.core.Books
import com.toloka.cho.domain.book.BookFilter
import com.toloka.cho.domain.book.BookInfo
import com.toloka.cho.domain.book.Book
import com.toloka.cho.domain.book.BookSort
import com.toloka.cho.domain.PublisherInfo
import com.toloka.cho.domain.Publisher
import com.toloka.cho.domain.pagination.*
import com.toloka.cho.server.http.routes.*
import com.toloka.cho.server.http.responses.{ApiError, ErrorResponse}


class BookRoutesSpec 
   extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with BookFixture
    with SecuredRouteFixture
    {

        val books: Books[IO] = new Books[IO] {

        override def copyCount(): IO[Long] = IO.pure(2L)

        override def possibleFilters(): IO[BookFilter] = IO(
            defaultFilter
        )

        override def create(bookInfo: BookInfo): IO[ju.UUID] = IO.pure(NewBookUuid)

        override def all(): fs2.Stream[IO, Book] = fs2.Stream.emit((AwesomeBook))

        override def all(filter: BookFilter, pagination: Pagination, sort: BookSort): IO[List[Book]] =
            if (filter.inHallOnly) IO.pure(List())
            else if (filter.search.nonEmpty) IO.pure(List(AnotherAwesomeBook))
            else
                sort match
                    case BookSort.Author => IO.pure(List(AnotherAwesomeBook))
                    case _ => IO.pure(List(AwesomeBook))


        override def allForManagement(filter: BookFilter, pagination: Pagination, sort: BookSort): IO[List[Book]] =
            IO.pure(List(AwesomeBook.copy(bookInfo = AwesomeBook.bookInfo.copy(copies = None))))

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

        override def allPublishers(): IO[List[Publisher]] =
            IO.pure(List(Publisher(1, PublisherInfo("Broom Publish", None, None, None, None))))

        override def allLanguages(): IO[List[String]] = IO.pure(List("English", "French", "Ukrainian"))

        override def createPublisher(info: PublisherInfo): IO[Int] = IO.pure(2)

        override def addCopy(bookId: ju.UUID): IO[Option[ju.UUID]] =
            IO.pure(Option.when(bookId == AwesomeBookUuid)(sampleCopy.copyId))

        override def deleteCopy(bookId: ju.UUID, copyId: ju.UUID): IO[Int] =
            IO.pure(if bookId == AwesomeBookUuid && copyId == sampleCopy.copyId then 1 else 0)

        }

        given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
        val bookRoutes: HttpRoutes[IO] = BookRoutes[IO](books).routes
        val defaultFilter: BookFilter  = BookFilter(authors = List("Awesome Company"))


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

        "should reject an unknown sort value" in {
            bookRoutes.orNotFound
                .run(Request(method = Method.POST, uri = uri"/books?sort=unknown").withEntity(BookFilter()))
                .map(_.status shouldBe Status.BadRequest)
        }

        "should pass the selected sort to the books service" in {
            for {
                response <- bookRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/books?sort=author").withEntity(BookFilter())
                )
                retrieved <- response.as[List[Book]]
            } yield retrieved shouldBe List(AnotherAwesomeBook)
        }

        "should pass search text in the request body to the books service" in {
            for {
                response <- bookRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/books").withEntity(BookFilter(search = Some("J.K. Rowling")))
                )
                retrieved <- response.as[List[Book]]
            } yield retrieved shouldBe List(AnotherAwesomeBook)
        }

        "should reject search text longer than 100 characters" in {
            bookRoutes.orNotFound
                .run(
                    Request(method = Method.POST, uri = uri"/books")
                        .withEntity(BookFilter(search = Some("x" * 101)))
                )
                .map(_.status shouldBe Status.BadRequest)
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

        "should explain the required roles when a reader updates a book" in {
            for {
                jwtToken <- mockedAuthenticator.create(Reader.email)
                response <- bookRoutes.orNotFound.run(
                    Request[IO](method = Method.PUT, uri = uri"/books/843df718-ec6e-4d49-9289-f799c0f40064")
                        .withEntity(UpdatedAwesomeBook.bookInfo)
                        .withBearerToken(jwtToken)
                )
                error <- response.as[ErrorResponse]
            } yield {
                response.status shouldBe Status.Forbidden
                error shouldBe ErrorResponse.fromApiError(ApiError.AuthorizationError("Only admins and librarians can update books"))
            }
        }

        "should allow librarians to update books" in {
            for {
                jwtToken <- mockedAuthenticator.create(gastonEmail)
                response <- bookRoutes.orNotFound.run(
                    Request[IO](method = Method.PUT, uri = uri"/books/843df718-ec6e-4d49-9289-f799c0f40064")
                        .withEntity(UpdatedAwesomeBook.bookInfo)
                        .withBearerToken(jwtToken)
                )
            } yield response.status shouldBe Status.Ok
        }

        "should reject updating a book with an unknown identity" in {
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

        "should surface all possible fitlers" in {
            for {
                response <- bookRoutes.orNotFound.run(Request(method = Method.GET, uri = uri"/books/filters"))
                filter <- response.as[BookFilter]
            } yield {
                filter shouldBe defaultFilter
            }
        }

        "should list management books for administrators and librarians" in {
            List(remiEmail, gastonEmail).traverse_ { email =>
                for {
                    token <- mockedAuthenticator.create(email)
                    response <- bookRoutes.orNotFound.run(
                        Request[IO](Method.POST, uri"/books/manage?limit=10&skip=0&sort=name")
                            .withEntity(BookFilter()).withBearerToken(token)
                    )
                    retrieved <- response.as[List[Book]]
                } yield {
                    response.status shouldBe Status.Ok
                    retrieved.map(_.id) shouldBe List(AwesomeBookUuid)
                    retrieved.head.bookInfo.copies shouldBe None
                }
            }.map(_ => succeed)
        }

        "should require a staff session for management books" in {
            for {
                token <- mockedAuthenticator.create(Reader.email)
                reader <- bookRoutes.orNotFound.run(
                    Request[IO](Method.POST, uri"/books/manage").withEntity(BookFilter()).withBearerToken(token)
                )
                anonymous <- bookRoutes.orNotFound.run(
                    Request[IO](Method.POST, uri"/books/manage").withEntity(BookFilter())
                )
            } yield {
                reader.status shouldBe Status.Unauthorized
                anonymous.status shouldBe Status.Unauthorized
            }
        }

        "should validate management search and sorting" in {
            for {
                token <- mockedAuthenticator.create(remiEmail)
                invalidSort <- bookRoutes.orNotFound.run(
                    Request[IO](Method.POST, uri"/books/manage?sort=unknown")
                        .withEntity(BookFilter()).withBearerToken(token)
                )
                invalidSearch <- bookRoutes.orNotFound.run(
                    Request[IO](Method.POST, uri"/books/manage")
                        .withEntity(BookFilter(search = Some("x" * 101))).withBearerToken(token)
                )
            } yield {
                invalidSort.status shouldBe Status.BadRequest
                invalidSearch.status shouldBe Status.BadRequest
            }
        }

        "should list available languages" in {
            bookRoutes.orNotFound.run(Request[IO](Method.GET, uri"/books/languages"))
                .flatMap(_.as[List[String]]).map(_ shouldBe List("English", "French", "Ukrainian"))
        }

        "should reject unknown languages when creating or updating a book" in {
            for {
                token <- mockedAuthenticator.create(remiEmail)
                invalid = AwesomeBook.bookInfo.copy(languages = List("Unknown"))
                create <- bookRoutes.orNotFound.run(
                    Request[IO](Method.POST, uri"/books/create").withEntity(invalid).withBearerToken(token))
                update <- bookRoutes.orNotFound.run(
                    Request[IO](Method.PUT, uri"/books/843df718-ec6e-4d49-9289-f799c0f40064")
                        .withEntity(invalid).withBearerToken(token))
            } yield {
                create.status shouldBe Status.BadRequest
                update.status shouldBe Status.BadRequest
            }
        }

        "should list publishers for the book editor" in {
            bookRoutes.orNotFound.run(Request(method = Method.GET, uri = uri"/books/publishers"))
                .flatMap(_.as[List[Publisher]])
                .map(_.map(_.info.name) shouldBe List("Broom Publish"))
        }

        "should create a publisher for library staff" in {
            for {
                jwtToken <- mockedAuthenticator.create(remiEmail)
                response <- bookRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/books/publishers/create")
                        .withEntity(PublisherInfo("New Publisher", Some("France"), None, None, None))
                        .withBearerToken(jwtToken)
                )
            } yield response.status shouldBe Status.Created
        }

        "should add and delete a book copy for library staff" in {
            for {
                jwtToken <- mockedAuthenticator.create(remiEmail)
                addResponse <- bookRoutes.orNotFound.run(
                    Request(method = Method.POST, uri = uri"/books/843df718-ec6e-4d49-9289-f799c0f40064/copies")
                        .withBearerToken(jwtToken)
                )
                deleteResponse <- bookRoutes.orNotFound.run(
                    Request(method = Method.DELETE, uri = uri"/books/843df718-ec6e-4d49-9289-f799c0f40064/copies/90bd1ae7-14db-4b36-ac9e-fa1e23c65d1a")
                        .withBearerToken(jwtToken)
                )
            } yield {
                addResponse.status shouldBe Status.Created
                deleteResponse.status shouldBe Status.Ok
            }
        }
    }
