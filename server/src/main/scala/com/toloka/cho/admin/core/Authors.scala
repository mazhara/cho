package com.toloka.cho.admin.core

import com.toloka.cho.domain.AuthorInfo
import java.util.UUID
import doobie.util.transactor.Transactor
import java.util.UUID
import cats.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger

import cats._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.connection._

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import doobie.syntax.*
import doobie.util.fragment.*
import cats.effect.kernel.Async
import com.toloka.cho.domain.Author
import com.toloka.cho.domain.AuthorType

trait Authors[F[_]] {
    def create(authorInfo: AuthorInfo): F[UUID]
    def delete(id: UUID): F[Int]
    def update(id: UUID, authorInfo: AuthorInfo): F[Option[Author]]
    def all(): fs2.Stream[F, Author]
    def find(id: UUID): F[Option[Author]]
    def find(id: String): F[List[Author]]
}

class LiveAuthors[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Authors[F] {

    import com.toloka.cho.admin.core.LiveAuthors.authorRead
    override def create(authorInfo: AuthorInfo): F[UUID] = {

      val query =
        sql"""
        INSERT INTO Authors (first_name, last_name, author_type) 
        VALUES (${authorInfo.firstName}, ${authorInfo.lastName}, ${authorInfo.authorType.toString}::author_type)
        RETURNING author_id
        """
       query.query[UUID].unique.transact(xa)
    }

    override def delete(id: UUID): F[Int] = {
        sql"""
        DELETE FROM authors
        WHERE author_id = $id
        """.update.run
        .transact(xa)
    }

    override def update(authorId: UUID, authorInfo: AuthorInfo): F[Option[Author]] = {
        val query =
            sql"""
                UPDATE Authors 
                SET first_name = ${authorInfo.firstName},
                    last_name = ${authorInfo.lastName},
                    author_type = ${authorInfo.authorType.toString}::author_type
                WHERE author_id = $authorId
                RETURNING author_id, first_name, last_name, author_type
                """
        query.query[Author].option.transact(xa)
    }

    override def find(id: UUID): F[Option[Author]] = {
        sql"""
        SELECT author_id, first_name, last_name, author_type
        FROM authors
        WHERE author_id = $id
        """.query[Author].option.transact(xa)
    }

    override def find(partialName: String): F[List[Author]] = {
        val searchPattern = s"%$partialName%"

        sql"""
            SELECT author_id, first_name, last_name, author_type
            FROM authors
            WHERE first_name LIKE $searchPattern OR last_name LIKE $searchPattern
        """.query[Author].to[List].transact(xa)
    }

    override def all(): fs2.Stream[F, Author] = {
        val query = sql"SELECT author_id, first_name, last_name, author_type FROM Authors"
        query.query[(UUID, Option[String], String, String)]
            .map { case (id, firstName, lastName, authorType) =>
                Author(id, AuthorInfo(firstName, lastName, AuthorType.valueOf(authorType)))
            }
            .stream
            .transact(xa)
            }
}

object LiveAuthors {

    given authorRead: Read[Author] = Read[(UUID, Option[String], String, String)].map {
        case (authorId, firstName, lastName, authorType) =>
            Author(authorId, AuthorInfo(firstName, lastName, AuthorType.valueOf(authorType)))
}
    def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveAuthors[F]] = new LiveAuthors[F](xa).pure
}