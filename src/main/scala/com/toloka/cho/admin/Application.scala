package com.toloka.cho.admin

import cats.*
import cats.effect.*
import cats.implicits.*

import cats.effect.IOApp
import org.http4s.ember.server.EmberServerBuilder
import com.toloka.modules.*
import pureconfig.ConfigSource
import com.toloka.cho.admin.config.EmberConfig
import com.toloka.cho.admin.config.syntax.*
import pureconfig.error.ConfigReaderException
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.toloka.cho.admin.config.AppConfig
import com.toloka.cho.admin.palyground.BooksPlayground.postgresResource

object Application extends IOApp.Simple {

  val configSourse = ConfigSource.default.load[EmberConfig]

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, AppConfig].flatMap {
      case AppConfig(postgresConfig, emberConfig) =>
      val appResource = for {
        xa <- Database.makePostgresResource[IO](postgresConfig)
        core <- Core[IO](xa)
        httpApi <- HttpApi[IO](core)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host)
          .withPort(emberConfig.port)
          .withHttpApp(httpApi.endpoints.orNotFound)
          .build
       
      } yield server

      appResource.use(_ => IO.println("Lets start!") *> IO.never)
    }

}
