package com.toloka.cho.server.core

import cats.effect.*
import cats.effect.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*
import com.toloka.cho.server.fixtures.UserFixture
import com.toloka.cho.server.config.TokenConfig
import com.toloka.cho.server.core.LiveTokens
import doobie.implicits.*

class TokensSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with UserFixture {
  val initScript: String   = "sql/recoverytokens.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Tokens algebra" - {
    "stores generated and refreshed token expiration in milliseconds" in {
      transactor.use { xa =>
        for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(2.minutes))
          before <- IO.realTime.map(_.toMillis)
          _ <- tokens.generateToken(remiEmail)
          generatedExpiration <- sql"SELECT expiration FROM recoverytokens WHERE email=$remiEmail"
            .query[Long].unique.transact(xa)
          _ <- tokens.updateToken(remiEmail)
          updatedExpiration <- sql"SELECT expiration FROM recoverytokens WHERE email=$remiEmail"
            .query[Long].unique.transact(xa)
          after <- IO.realTime.map(_.toMillis)
        } yield {
          List(generatedExpiration, updatedExpiration).foreach { expiration =>
            expiration should be >= (before + 2.minutes.toMillis)
            expiration should be <= (after + 2.minutes.toMillis)
          }
        }
      }
    }

    "should not create token for a non-existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10.minutes))
          token  <- tokens.getToken("somebody@someemail.com")
        } yield token

        program.asserting(_ shouldBe None)
      }
    }

    "should create a token for a existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10.minutes))
          token  <- tokens.getToken(remiEmail)
        } yield token

        program.asserting(_ shouldBe defined)
      }
    }

    "should not validate expired tokens" in {
      transactor.use { xa =>
        val program = for {
          tokens     <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100.millis))
          maybeToken <- tokens.getToken(remiEmail)
          _          <- IO.sleep(500.millis)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(remiEmail, token)
            case None        => IO.pure(false)
          }
        } yield isTokenValid

        program.asserting(_ shouldBe false)
      }
    }

    "should validate fresh tokens" in {
      transactor.use { xa =>
        val program = for {
          tokens     <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10.minutes))
          maybeToken <- tokens.getToken(remiEmail)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(remiEmail, token)
            case None        => IO.pure(false)
          }
        } yield isTokenValid

        program.asserting(_ shouldBe true)
      }
    }

    "should only validate tokens for the user that generated them" in {
      transactor.use { xa =>
        val program = for {
          tokens     <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(10.minutes))
          maybeToken <- tokens.getToken(remiEmail)
          isRemiTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(remiEmail, token)
            case None        => IO.pure(false)
          }
          isOtherTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken("someoneelse@gmail.com", token)
            case None        => IO.pure(false)
          }
        } yield (isRemiTokenValid, isOtherTokenValid)

        program.asserting { case (isRemiTokenValid, isOtherTokenValid) =>
          isRemiTokenValid shouldBe true
          isOtherTokenValid shouldBe false
        }
      }
    }
  }
}
