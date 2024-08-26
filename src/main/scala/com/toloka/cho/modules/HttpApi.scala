package com.toloka.cho.modules

import cats.* 
import cats.implicits.*
import cats.effect.*
import org.http4s.* 
import org.http4s.dsl.* 
import org.http4s.server.* 
import com.toloka.cho.admin.http.routes.BookRoutes
import com.toloka.cho.admin.http.routes.HealthRoutes
import org.typelevel.log4cats.Logger
import com.toloka.cho.admin.http.routes.AuthRoutes

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F]) {
  private val healthRoutes = HealthRoutes[F].routes
  private val bookRoutes = BookRoutes[F](core.books, core.auth.authenticator).routes
  private val authRoutes   = AuthRoutes[F](core.auth).routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> bookRoutes <+> authRoutes)
  )
}


object HttpApi {
   def apply[F[_]: Concurrent: Logger](core: Core[F]): Resource[F, HttpApi[F]] = Resource.pure(new HttpApi[F](core))
}
