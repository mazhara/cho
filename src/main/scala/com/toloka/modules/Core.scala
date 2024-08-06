package com.toloka.modules

import cats.effect.* 
import cats.implicits.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import com.toloka.cho.admin.core.LiveBooks
import com.toloka.cho.admin.core.Books

final class Core[F[_]] private (val books: Books[F])

// postgress ->  jobs -> core -> httpApi -> app
object Core {

    def postgresResource[F[_]: Async]: Resource[F, HikariTransactor[F]] = for {
        ec <- ExecutionContexts.fixedThreadPool(32)
        xa <- HikariTransactor.newHikariTransactor[F](
            "org.postgresql.Driver",
            "jdbc:postgresql:library",
            "docker",
            "docker",
            ec)
    } yield xa

    def apply[F[_]: Async] : Resource[F, Core[F]]= {
        postgresResource[F]
        .evalMap(postgres => LiveBooks[F](postgres))
        .map(books => new Core(books))

    }
}
