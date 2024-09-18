package com.toloka.cho.modules

import com.toloka.cho.admin.config.PostgressConfig
import cats.effect.kernel.Async
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import cats.effect.kernel.Resource

object Database {
      def makePostgresResource[F[_]: Async](config: PostgressConfig): Resource[F, HikariTransactor[F]] = for {
        ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
        xa <- HikariTransactor.newHikariTransactor[F](
            "org.postgresql.Driver",
            config.url,
            config.user,
            config.pass,
            ec)
    } yield xa
}
