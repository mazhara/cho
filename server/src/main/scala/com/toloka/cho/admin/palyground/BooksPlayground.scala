package com.toloka.cho.admin.palyground

import cats.effect.* 
import doobie.*
import doobie.implicits.*
import doobie.syntax.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import com.toloka.cho.domain.book.BookInfo
import com.toloka.cho.admin.core.LiveBooks
import scala.io.StdIn
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object BooksPlayground extends IOApp.Simple {

    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
        ec <- ExecutionContexts.fixedThreadPool(32)
        xa <- HikariTransactor.newHikariTransactor[IO](
                "org.postgresql.Driver",
                "jdbc:postgresql:library",
                "docker",
                "docker",
                ec
            )
    } yield xa


    val bookInfo = BookInfo.minimal(
        "HP",
        "JK",
        "best book ever",
        "bb",
        1999,
        false
    )
    override def run: IO[Unit] = postgresResource.use { xa => 
        for {
          books <- LiveBooks[IO](xa)  
          _ <- IO(println("Ready. Next...")) *> IO(StdIn.readLine)
          id <- books.create(bookInfo)
          _  <- IO(println(s" id = $id Next... ")) *> IO(StdIn.readLine)
          list <- books.all()
          _ <- IO(println(s"All books... $books Next..")) *> IO(StdIn.readLine)
          _ <- books.update(id, bookInfo.copy(author = "HHHH"))
          newBook <- books.find(id)
          _ <- IO(println(s"Ready. Updated book $newBook .Next...")) *> IO(StdIn.readLine)
          _ <- books.delete(id)
          _ <- IO(println("Deleted. Next...")) *> IO(StdIn.readLine)
          listAfter <- books.all()
          _ <- IO(println(s"All books... $listAfter. End.")) *> IO(StdIn.readLine)
         } yield ()

    }


}