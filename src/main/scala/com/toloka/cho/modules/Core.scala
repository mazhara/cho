package com.toloka.cho.modules

import cats.effect.* 
import cats.implicits.*
import com.toloka.cho.admin.core.LiveBooks
import com.toloka.cho.admin.core.Books
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import com.toloka.cho.admin.core.LiveUsers
import com.toloka.cho.admin.core.LiveAuth
import com.toloka.cho.admin.config.SecurityConfig
import com.toloka.cho.admin.core.Auth

final class Core[F[_]] private (val books: Books[F], val auth: Auth[F])

// postgress ->  jobs -> core -> httpApi -> app
object Core {

    def apply[F[_]: Async : Logger](xa: Transactor[F])(securityConfig: SecurityConfig): Resource[F, Core[F]] = {
        val coreF = for {
            books <- LiveBooks[F](xa)
            users <- LiveUsers[F](xa)
            auth <- LiveAuth[F](users)(securityConfig)
        } yield new Core(books, auth)

        Resource.eval(coreF)
    }   
    
}
