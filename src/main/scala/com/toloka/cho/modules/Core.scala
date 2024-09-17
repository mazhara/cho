package com.toloka.cho.modules

import cats.effect.* 
import cats.implicits.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import com.toloka.cho.admin.core.*
import com.toloka.cho.admin.config.*

final class Core[F[_]] private (val books: Books[F], val users: Users[F], val auth: Auth[F])

// postgress ->  jobs -> core -> httpApi -> app
object Core {

    def apply[F[_]: Async: Logger](
      xa: Transactor[F],
      tokenConfig: TokenConfig,
      emailServiceConfig: EmailServiceConfig
  ): Resource[F, Core[F]] = {
        val coreF = for {
            books <- LiveBooks[F](xa)
            users <- LiveUsers[F](xa)
            tokens <- LiveTokens[F](users)(xa, tokenConfig)
            emails <- LiveEmails[F](emailServiceConfig)
            auth   <- LiveAuth[F](users, tokens, emails)
        } yield new Core(books, users, auth)

        Resource.eval(coreF)
    }   
    
}
