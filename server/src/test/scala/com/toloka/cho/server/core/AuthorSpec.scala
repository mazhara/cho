package com.toloka.cho.server.core

import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import doobie.util.*
import doobie.implicits.*
import doobie.*
import doobie.postgres.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import com.toloka.cho.server.fixtures.AuthorFixture
import doobie.util.transactor
import com.toloka.cho.server.core.DoobieSpec
import com.toloka.cho.server.core.LiveAuthors
import com.toloka.cho.domain.{Author, AuthorType}

class AuthorsSpec extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with AuthorFixture {

    val initScript: String = "sql/authors.sql"
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    "authors algebra" - {
        "should return no author if the given UUID does not exist" in {
            transactor.use { xa =>
            val program = for {
                authors    <- LiveAuthors[IO](xa)
                retrieved  <- authors.find(notFoundAuthorUuid)
            } yield retrieved

            program.asserting(_ shouldBe None)
            }
        }

        "should return an author by id" in {
        transactor.use { xa =>
            val program = for {
            authors    <- LiveAuthors[IO](xa)
            retrieved  <- authors.find(existingAuthorUuid)
            } yield retrieved

            program.asserting(_ shouldBe Some(existingAuthor))
        }
        }

        "should return an author by name pattern" in {
            transactor.use { xa =>
                val program = for {
                authors    <- LiveAuthors[IO](xa)
                retrieved  <- authors.find(existingAuthor.authorInfo.firstName.get)
                } yield retrieved

                program.asserting(_ shouldBe List(existingAuthor))
           }
        }

        "should return an author by surname pattern" in {
            transactor.use { xa =>
                val program = for {
                authors    <- LiveAuthors[IO](xa)
                retrieved  <- authors.find(existingAuthor.authorInfo.lastName)
                } yield retrieved

                program.asserting(_ shouldBe List(existingAuthor))
            }
       }

        "should return all authors" in {
            transactor.use { xa =>
                val program = for {
                authors    <- LiveAuthors[IO](xa)
                retrieved  <- authors.all().compile.toList
                } yield retrieved

                program.asserting(_ should contain theSameElementsAs List(existingAuthor))
            }
        }

        "should match first and last names regardless of case" in {
            transactor.use { xa =>
                val program = for {
                    authors <- LiveAuthors[IO](xa)
                    byFirstName <- authors.find("jAn")
                    byLastName <- authors.find("dOE")
                } yield (byFirstName, byLastName)

                program.asserting { case (byFirstName, byLastName) =>
                    byFirstName shouldBe List(existingAuthor)
                    byLastName shouldBe List(existingAuthor)
                }
            }
        }

        "should round-trip every author type through create, find, update and all" in {
            transactor.use { xa =>
                val program = for {
                    authors <- LiveAuthors[IO](xa)
                    results <- AuthorType.values.toList.traverse { authorType =>
                        val info = newAuthor.copy(authorType = authorType)
                        for {
                            id <- authors.create(info)
                            found <- authors.find(id)
                            updated <- authors.update(existingAuthorUuid, updatedAuthor.copy(authorType = authorType))
                        } yield (info, found, updated)
                    }
                    all <- authors.all().compile.toList
                } yield (results, all)

                program.asserting { case (results, all) =>
                    results.foreach { case (info, found, updated) =>
                        found.map(_.authorInfo) shouldBe Some(info)
                        updated.map(_.authorInfo) shouldBe Some(updatedAuthor.copy(authorType = info.authorType))
                    }
                    all.filterNot(_.id == existingAuthorUuid).map(_.authorInfo) should contain theSameElementsAs
                        AuthorType.values.toList.map(authorType => newAuthor.copy(authorType = authorType))
                }
            }
        }

        "should create a new author" in {
            transactor.use { xa =>
                val program = for {
                authors    <- LiveAuthors[IO](xa)
                authorId   <- authors.create(newAuthor)
                maybeAuthor <- authors.find(authorId)
                } yield maybeAuthor

                program.asserting(_.map(_.authorInfo) shouldBe Some(newAuthor))
            }
        }

        "should update an existing author" in {
        transactor.use { xa =>
            val program = for {
            authors       <- LiveAuthors[IO](xa)
            maybeUpdated  <- authors.update(existingAuthorUuid, updatedAuthor)
            } yield maybeUpdated

            program.asserting(_.map(_.authorInfo) shouldBe Some(updatedAuthor))
        }
        }

        "should return none when trying to update an author that does not exist" in {
        transactor.use { xa =>
            val program = for {
            authors       <- LiveAuthors[IO](xa)
            maybeUpdated  <- authors.update(notFoundAuthorUuid, updatedAuthor)
            } yield maybeUpdated

            program.asserting(_ shouldBe None)
        }
        }

        "should delete an author if it exists" in {
            transactor.use { xa =>
                val program = for {
                authors          <- LiveAuthors[IO](xa)
                numberOfDeleted  <- authors.delete(existingAuthorUuid)
                countOfAuthors   <- sql"SELECT COUNT(*) FROM authors WHERE author_id = $existingAuthorUuid"
                                    .query[Int]
                                    .unique
                                    .transact(xa)
                } yield (numberOfDeleted, countOfAuthors)

                program.asserting {
                case (numberOfDeleted, countOfAuthors) =>
                    numberOfDeleted shouldBe 1
                    countOfAuthors shouldBe 0
                }
            }
        }

        "should return 0 deleted rows if the author ID to delete is not found" in {
            transactor.use { xa =>
                    val program = for {
                    authors          <- LiveAuthors[IO](xa)
                    numberOfDeleted  <- authors.delete(notFoundAuthorUuid)
                    } yield numberOfDeleted

                    program.asserting(_ shouldBe 0)
                }
            }
    }
}
