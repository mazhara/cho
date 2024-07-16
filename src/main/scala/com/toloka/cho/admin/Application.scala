package com.toloka.cho.admin

import cats.*
import cats.effect.*
import cats.implicits.*

import cats.effect.IOApp
import org.http4s.ember.server.EmberServerBuilder
import com.toloka.cho.admin.http.HttpApi
import pureconfig.ConfigSource
import com.toloka.cho.admin.config.EmberConfig
import com.toloka.cho.admin.config.syntax.*
import pureconfig.error.ConfigReaderException

object Application extends IOApp.Simple {

  val configSourse = ConfigSource.default.load[EmberConfig]

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
      EmberServerBuilder
        .default[IO]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(HttpApi[IO].endpoints.orNotFound)
        .build
        .use(_ => IO.println("Lets start!") *> IO.never)
    }

}
