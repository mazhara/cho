package com.toloka.cho.admin.http.routes

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.server.*
import cats.effect.*
import org.http4s.dsl.Http4sDsl

class HealthRoutes [F[_]: Concurrent] private extends Http4sDsl[F]{
    private val healthRoute:  HttpRoutes[F] = {
        HttpRoutes.of[F] { case GET -> Root =>
            Ok("All going great")
        }
    }

    val routes = Router(
        "/health" -> healthRoute
    )
}

object HealthRoutes {
    def apply[F[_]: Concurrent] =  new HealthRoutes[F]
}
