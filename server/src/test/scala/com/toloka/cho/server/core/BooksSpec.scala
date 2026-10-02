package com.toloka.cho.server.core

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
import com.toloka.cho.server.core.LiveBooks
import com.toloka.cho.server.domain.book.{BookFilter, BookSort}
import com.toloka.cho.server.domain.pagination.*


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
                        retrieved <- books.all().compile.toList
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
                        addedAt  <- sql"SELECT catalog_added_at IS NOT NULL FROM books WHERE book_id = $jobId"
                            .query[Boolean]
                            .unique
                            .transact(xa)
                    } yield (maybeBook, addedAt)

                    program.asserting { case (maybeBook, addedAt) =>
                        maybeBook.map(_.bookInfo.copy(copies = None)) shouldBe Some(AwesomeNewBook)
                        addedAt shouldBe true
                    }
                }
            }

            "should sort books and paginate by whole books" in {
                transactor.use { xa =>
                    val newBookId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")
                    val oldBookId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")
                    val newAuthorId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003")
                    val oldAuthorId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000004")
                    val firstOldAuthorId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000005")

                    val program = for {
                        _ <- sql"""
                            INSERT INTO Books (book_id, title, catalog_added_at, published_year, tags)
                            VALUES
                                ($newBookId, 'Alpha', '2026-01-01T00:00:00Z', 2000, ARRAY[]::text[]),
                                ($oldBookId, 'Zebra', '2025-01-01T00:00:00Z', 2000, ARRAY[]::text[])
                            """.update.run.transact(xa)
                        _ <- sql"""
                            INSERT INTO Authors (author_id, first_name, last_name, author_type)
                            VALUES
                                ($newAuthorId, 'Amy', 'Author', 'Author'),
                                ($oldAuthorId, 'Zoe', 'Author', 'Author'),
                                ($firstOldAuthorId, 'Aaron', 'Author', 'Author')
                            """.update.run.transact(xa)
                        _ <- sql"""
                            INSERT INTO BookAuthors (book_id, author_id)
                            VALUES
                                ($newBookId, $newAuthorId),
                                ($oldBookId, $oldAuthorId),
                                ($oldBookId, $firstOldAuthorId)
                            """.update.run.transact(xa)
                        _ <- sql"""
                            INSERT INTO Book_Copies (book_id) VALUES ($newBookId), ($oldBookId)
                            """.update.run.transact(xa)
                        books <- LiveBooks[IO](xa)
                        byNew <- books.all(BookFilter(), Pagination(10, 0), BookSort.New)
                        byAuthor <- books.all(BookFilter(), Pagination(10, 0), BookSort.Author)
                        byName <- books.all(BookFilter(), Pagination(2, 0), BookSort.Name)
                        secondNamePage <- books.all(BookFilter(), Pagination(2, 2), BookSort.Name)
                    } yield (byNew, byAuthor, byName, secondNamePage)

                    program.asserting { case (byNew, byAuthor, byName, secondNamePage) =>
                        byNew.map(_.id) shouldBe List(newBookId, oldBookId, AwesomeBookUuid)
                        byAuthor.map(_.id) shouldBe List(oldBookId, newBookId, AwesomeBookUuid)
                        byAuthor.head.bookInfo.authors.map(_.size) shouldBe Some(2)
                        byName.map(_.id) shouldBe List(newBookId, AwesomeBookUuid)
                        secondNamePage.map(_.id) shouldBe List(oldBookId)
                    }
                }
            }

            "should search titles and authors with typo tolerance and treat pattern syntax literally" in {
                transactor.use { xa =>
                    val program = for {
                        books <- LiveBooks[IO](xa)
                        typoMatch <- books.all(
                            BookFilter(search = Some("Harry Potter and the Philospher Stone")),
                            Pagination.default,
                            BookSort.Name
                        )
                        authorMatch <- books.all(
                            BookFilter(search = Some("J.K. Rowlin")),
                            Pagination.default,
                            BookSort.Name
                        )
                        injectionLike <- books.all(
                            BookFilter(search = Some("%' OR 1=1 --")),
                            Pagination.default,
                            BookSort.Name
                        )
                        count <- sql"SELECT COUNT(*) FROM books".query[Int].unique.transact(xa)
                    } yield (typoMatch, authorMatch, injectionLike, count)

                    program.asserting { case (typoMatch, authorMatch, injectionLike, count) =>
                        typoMatch.map(_.id) shouldBe List(AwesomeBookUuid)
                        authorMatch.map(_.id) shouldBe List(AwesomeBookUuid)
                        injectionLike shouldBe empty
                        count shouldBe 1
                    }
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
                        countOfbooks <- sql"SELECT COUNT(*) FROM books WHERE book_id = $AwesomeBookUuid"
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
                            BookFilter(tags = List("fantasy", "magic", "children")),
                            Pagination.default,
                            BookSort.New
                        )
                    } yield filteredJobs

                    program.asserting(_ shouldBe List(AwesomeBook))
                }
            }

            "should surface a comprehensive filter out of all books contained" in {
                transactor.use { xa =>
                    val program = for {
                    books   <- LiveBooks[IO](xa)
                    filter <- books.possibleFilters()
                    } yield filter
                    program.asserting {
                    case BookFilter(authors, publishers, tags, year, isHallOnly, _) =>
                        authors shouldBe List("J.K. Rowling")
                        publishers shouldBe List("Broom Publish")
                        tags.sorted shouldBe List("children", "fantasy", "magic").sorted
                        year shouldBe Some(1997)
                    }
                }
            }
         }  

    }
