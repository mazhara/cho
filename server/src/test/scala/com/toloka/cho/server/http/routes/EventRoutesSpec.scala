package com.toloka.cho.server.http.routes

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.toloka.cho.domain.event.Event
import com.toloka.cho.server.core.Events
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

final class EventRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  private val sampleEvent = Event(
    id = UUID.fromString("40000000-0000-4000-8000-000000000001"),
    title = "Читацький клуб",
    date = "2026-10-18 14:00",
    location = "Нант",
    afisheUrl = None,
    isOnline = true,
    language = "Українська",
    description = "Обговорення книжок",
    offlineAddress = Some("Nantes"),
    registrationUrl = None,
    isOffline = true
  )

  private val events: Events[IO] = new Events[IO] {
    override def all(): IO[List[Event]] = IO.pure(List(sampleEvent))
  }

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  "EventRoutes" - {
    "returns events as JSON from GET /events" in {
      for {
        response <- EventRoutes[IO](events).routes.orNotFound.run(Request[IO](uri = uri"/events"))
        result <- response.as[List[Event]]
      } yield {
        response.status shouldBe Status.Ok
        result shouldBe List(sampleEvent)
      }
    }

  }
}
