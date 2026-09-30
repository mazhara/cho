package com.toloka.cho.server.http.routes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.toloka.cho.domain.admin.AdminDashboardStats
import com.toloka.cho.domain.{Publisher, PublisherInfo}
import com.toloka.cho.domain.book.*
import com.toloka.cho.domain.pagination.*
import com.toloka.cho.server.core.Books
import com.toloka.cho.server.fixtures.{SecuredRouteFixture, UserFixture}
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class AdminRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with UserFixture
    with SecuredRouteFixture {

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  private val books: Books[IO] = new Books[IO] {
    override def copyCount(): IO[Long] = IO.pure(17L)
    override def create(bookInfo: BookInfo): IO[java.util.UUID] = IO.raiseError(new UnsupportedOperationException)
    override def all(): fs2.Stream[IO, Book] = fs2.Stream.empty
    override def all(filter: BookFilter, pagination: Pagination, sort: BookSort): IO[List[Book]] = IO.pure(Nil)
    override def find(id: java.util.UUID): IO[Option[Book]] = IO.pure(None)
    override def update(id: java.util.UUID, bookInfo: BookInfo): IO[Option[Book]] = IO.pure(None)
    override def delete(id: java.util.UUID): IO[Int] = IO.pure(0)
    override def possibleFilters(): IO[BookFilter] = IO.pure(BookFilter())
    override def allPublishers(): IO[List[Publisher]] = IO.pure(Nil)
    override def createPublisher(info: PublisherInfo): IO[Int] = IO.raiseError(new UnsupportedOperationException)
    override def addCopy(bookId: java.util.UUID): IO[Option[java.util.UUID]] = IO.pure(None)
    override def deleteCopy(bookId: java.util.UUID, copyId: java.util.UUID): IO[Int] = IO.pure(0)
  }

  private val routes = AdminRoutes[IO](mockedUsers, books).routes.orNotFound

  "AdminRoutes" - {
    "returns live database count values to administrators" in {
      for {
        token <- mockedAuthenticator.create(remiEmail)
        response <- routes.run(
          Request[IO](method = Method.GET, uri = uri"/admin/stats").withBearerToken(token)
        )
        stats <- response.as[AdminDashboardStats]
      } yield {
        response.status shouldBe Status.Ok
        stats shouldBe AdminDashboardStats(userCount = 3L, copyCount = 17L)
      }
    }

    "forbids readers from viewing dashboard statistics" in {
      for {
        token <- mockedAuthenticator.create(Reader.email)
        response <- routes.run(
          Request[IO](method = Method.GET, uri = uri"/admin/stats").withBearerToken(token)
        )
      } yield response.status shouldBe Status.Unauthorized
    }

    "requires authentication to view dashboard statistics" in {
      routes.run(Request[IO](method = Method.GET, uri = uri"/admin/stats"))
        .map(_.status shouldBe Status.Unauthorized)
    }
  }
}
