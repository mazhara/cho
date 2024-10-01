package com.toloka.cho.core

import cats.effect.*
import cats.effect.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import doobie.util.*
import doobie.implicits.*
import doobie.*
import doobie.postgres.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.toloka.cho.fixtures.BookFixture
import com.toloka.cho.admin.core.LiveBooks
import com.toloka.cho.domain.book.BookFilter
import com.toloka.cho.domain.pagination.*


class BooksSpec extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with BookFixture {

        val initScript: String = "sql/books.sql"
        given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

        "books algebra" - {
            "should return no book if the given UUID does not exist" in {
                transactor.use { xa =>
                    val program = for {
                        books      <- LiveBooks[IO](xa)
                        retrieved <- books.find(NotFoundBookUuid)
                    } yield retrieved

                    program.asserting(_ shouldBe None)
                }
            }

            "should return a book by id" in {
                transactor.use { xa =>
                    val program = for {
                        books      <- LiveBooks[IO](xa)
                        retrieved <- books.find(AwesomeBookUuid)
                    } yield retrieved

                    program.asserting(_ shouldBe Some(AwesomeBook))
                    }   
                }

            "should return all books" in {
                transactor.use { xa =>
                    val program = for {
                        books      <- LiveBooks[IO](xa)
                        retrieved <- books.all()
                    } yield retrieved

                    program.asserting(_ shouldBe List(AwesomeBook))
                }
            }

            "should create a new book" in {
                transactor.use { xa =>
                    val program = for {
                        books     <- LiveBooks[IO](xa)
                        jobId    <- books.create(AwesomeNewBook)
                        maybeBook <- books.find(jobId)
                    } yield maybeBook

                    program.asserting(_.map(_.bookInfo) shouldBe Some(AwesomeNewBook))
                }
            }

            "should return an updated book if it exists" in {
            transactor.use { xa =>
                    val program = for {
                        books            <- LiveBooks[IO](xa)
                        maybeUpdatedBook <- books.update(AwesomeBookUuid, UpdatedAwesomeBook.bookInfo)
                    } yield maybeUpdatedBook

                    program.asserting(_ shouldBe Some(UpdatedAwesomeBook))
                }
            }

            "should return none when trying to update a book that does not exist" in {
                transactor.use { xa =>
                    val program = for {
                        books            <- LiveBooks[IO](xa)
                        maybeUpdatedBook <- books.update(NotFoundBookUuid, UpdatedAwesomeBook.bookInfo)
                    } yield maybeUpdatedBook

                    program.asserting(_ shouldBe None)
                }
            }

            "should delete a book if it exists" in {
                transactor.use { xa =>
                    val program = for {
                        books <- LiveBooks[IO](xa)
                        numberOfDeletedbooks <- books.delete(AwesomeBookUuid)
                        countOfbooks <- sql"SELECT COUNT(*) FROM books WHERE id = $AwesomeBookUuid"
                            .query[Int]
                            .unique
                        .transact(xa)
                    } yield (numberOfDeletedbooks, countOfbooks)

                    program.asserting {
                        case (numberOfDeletedbooks, countOfbooks) => {
                            numberOfDeletedbooks shouldBe 1
                            countOfbooks shouldBe 0
                        }
                    }
                }
            }

            "should return 0 updated rows if the book ID to delete is not found" in {
                transactor.use { xa =>
                    val program = for {
                        books                <- LiveBooks[IO](xa)
                        numberOfDeletedbooks <- books.delete(NotFoundBookUuid)
                    } yield numberOfDeletedbooks

                    program.asserting(_ shouldBe 0)
                }
            }

            "should filter books by tags" in {
                transactor.use { xa =>
                    val program = for {
                        books <- LiveBooks[IO](xa)
                        filteredJobs <- books.all(
                            BookFilter(tags = List("fantasy", "bestseller", "children")),
                            Pagination.default
                        )
                    } yield filteredJobs

                    program.asserting(_ shouldBe List(AwesomeBook))
                }
            }
        }  

    }
