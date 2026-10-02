package com.toloka.cho.core

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.toloka.cho.admin.core.LiveEvents
import com.toloka.cho.server.fixtures.EventFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class EventsSpec extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with DoobieSpec
  with EventFixture {
  val initScript: String = "sql/events.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "events algebra" - {
    "should return all events" in {
      transactor.use { xa =>
        val program = for {
          events <- LiveEvents[IO](xa)
          retrieved <- events.all()
        } yield retrieved

        program.asserting(_ shouldBe AllEvents)
      }
    }
  }
}
