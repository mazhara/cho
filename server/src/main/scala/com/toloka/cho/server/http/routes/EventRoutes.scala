package com.toloka.cho.server.http.routes

import cats.effect.Concurrent
import cats.implicits.*
import com.toloka.cho.server.core.Events
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

final class EventRoutes[F[_]: Concurrent: Logger] private (events: Events[F]) extends Http4sDsl[F] {
  private val allEventsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      events.all().flatMap(Ok(_))
  }

  val routes: HttpRoutes[F] = Router("/events" -> allEventsRoute)
}

object EventRoutes {
  def apply[F[_]: Concurrent: Logger](events: Events[F]): EventRoutes[F] =
    new EventRoutes[F](events)
}
