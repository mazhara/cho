package com.toloka.cho.admin.core

import java.util.UUID
import cats.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import doobie.syntax.*
import doobie.util.fragment.*

import com.toloka.cho.domain.book.*
import com.toloka.cho.domain.pagination.*
import com.toloka.cho.admin.logging.syntax.logError


trait Books[F[_]] {
    def create(bookInfo: BookInfo): F[UUID]
    def all(): F[List[Book]]
    def all(filter: BookFilter, pagination: Pagination): F[List[Book]]
    def find(id: UUID): F[Option[Book]]
    def update (id: UUID, bookInfo: BookInfo): F[Option[Book]] 
    def delete(id: UUID): F[Int]
}

class LiveBooks[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Books[F] {

    override def create(bookInfo: BookInfo): F[UUID] = 
        sql"""
        INSERT INTO books(
            name
            ,author
            ,description
            ,publisher
            ,year
            ,inHallOnly
            ,tags
            ,image
        ) VALUES (
            ${bookInfo.name}
            ,${bookInfo.author}
            ,${bookInfo.description}
            ,${bookInfo.publisher}
            ,${bookInfo.year}
            ,false
            ,${bookInfo.tags}
            ,${bookInfo.image}
        )
        """.update
        .withUniqueGeneratedKeys[UUID]("id")
        .transact(xa)

    
    override def all(filter: BookFilter, pagination: Pagination): F[List[Book]] = {
        val selectFragment: Fragment =
        fr"""
            SELECT
                id
                , name
                , author
                , description
                , publisher
                , year
                , inHallOnly
                , tags
                , image
            """

        val fromFragment: Fragment =
            fr"FROM books"

        val whereFragment: Fragment = Fragments.whereAndOpt(
            filter.authors.toNel.map(authors => Fragments.in(fr"author", authors)),
            filter.publishers.toNel.map(publishers => Fragments.in(fr"publisher", publishers)),
            filter.tags.toNel.map(tags => Fragments.or(tags.toList.map(tag => fr"$tag=any(tags)"): _*)),
            filter.year.map(year => fr"year > $year"),
            filter.inHallOnly.some.map(inHallOnly => fr"inHallOnly = $inHallOnly")
        )

        val paginationFragment: Fragment =
        fr"ORDER BY id LIMIT ${pagination.limit} OFFSET ${pagination.skip}"

        val statement = selectFragment |+| fromFragment |+| whereFragment |+| paginationFragment

        Logger[F].info(statement.toString) *>
        statement
            .query[Book]
            .to[List]
            .transact(xa)
            .logError(e => "Failed query: ${e.getMessage}")
    }
    override def all(): F[List[Book]] = 
        sql"""
            SELECT
                id,
                name,
                author,
                description,
                publisher,
                year,
                inHallOnly,
                tags,
                image
            FROM books
        """
        .query[Book]
        .to[List]
        .transact(xa)



    override def find(id: UUID): F[Option[Book]] = 
        sql"""
            SELECT 
                id
                ,name
                ,author
                ,description
                ,publisher
                ,year
                ,inHallOnly
                ,tags
                ,image
            FROM books
            WHERE id = ${id}  
        """
        .query[Book]
        .option
        .transact(xa)
        

    override def update(id: UUID, bookInfo: BookInfo): F[Option[Book]] = 
        sql"""
            UPDATE books
                SET 
                name = ${bookInfo.name}
                ,author = ${bookInfo.author}
                ,description = ${bookInfo.description}
                ,publisher = ${bookInfo.publisher}
                ,year = ${bookInfo.year}
                ,inHallOnly = ${bookInfo.inHallOnly}
                ,tags = ${bookInfo.tags}
                ,image = ${bookInfo.image}
            WHERE id = ${id}
        """
        .update.run
        .transact(xa)
        .flatMap(_ => find(id))

    override def delete(id: UUID): F[Int] =  
        sql"""
            DELETE FROM books
            WHERE id = ${id}
        """.update.run
        .transact(xa)   
}


object LiveBooks {
    given bookRead: Read[Book] = Read[
    (
        UUID,
        String,
        String,
        String,
        String,
        Int,
        Boolean,
        Option[List[String]],
        Option[String]
    )
    ].map:
        case(
            id: UUID,
            name: String,
            author: String,
            description: String,
            publisher: String,
            year: Int,
            inHallOnly: Boolean,
            tags: Option[List[String]] @unchecked,
            image: Option[String] @unchecked
        ) =>
            Book(
                    id = id,
                    BookInfo(
                        name = name,
                        author = author,
                        description = description,
                        publisher = publisher,
                        year = year,
                        inHallOnly = inHallOnly,
                        tags = tags,
                        image= image
                )
            )
    def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveBooks[F]] = new LiveBooks[F](xa).pure
}