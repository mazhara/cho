package com.toloka.modules

import cats.effect.* 
import cats.implicits.*
import com.toloka.cho.admin.core.LiveBooks
import com.toloka.cho.admin.core.Books
import doobie.util.transactor.Transactor

final class Core[F[_]] private (val books: Books[F])

// postgress ->  jobs -> core -> httpApi -> app
object Core {

    def apply[F[_]: Async](xa: Transactor[F]) : Resource[F, Core[F]]= {
        Resource
        .eval(LiveBooks[F](xa))
        .map(books => new Core(books))

    }
}
