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
import cats.effect.kernel.Async
import com.toloka.cho.domain.AuthorInfo


trait Books[F[_]] {
    def create(bookInfo: BookInfo): F[UUID]
    def all(): fs2.Stream[F, Book]
    def all(filter: BookFilter, pagination: Pagination, sort: BookSort): F[List[Book]]
    def find(id: UUID): F[Option[Book]]
    def update (id: UUID, bookInfo: BookInfo): F[Option[Book]] 
    def delete(id: UUID): F[Int]
    def possibleFilters(): F[BookFilter]
}

class LiveBooks[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Books[F] {

    import LiveBooks.bookRead

    override def create(bookInfo: BookInfo): F[UUID] = {
        val insertBookQuery =
            sql"""
            INSERT INTO Books (title, isbn, description, genre, published_year, tags, cover, publisher_id, catalog_added_at)
            VALUES (${bookInfo.title}, ${bookInfo.isbn}, ${bookInfo.description}, ${bookInfo.genre},
                    ${bookInfo.publishedYear}, ${bookInfo.tags}, ${bookInfo.image}, ${bookInfo.publisherId}, CURRENT_TIMESTAMP)
            """.update
            .withUniqueGeneratedKeys[UUID]("book_id")
            
        def insertBookCopies(bookId: UUID): ConnectionIO[Int] =
            sql"""
            INSERT INTO Book_Copies (book_id, exemplar_number, available, in_library_only)
            VALUES ($bookId, 1, true, false)
            """.update.run

        val insertAuthorsQuery = (bookId: UUID) =>
            Update[(UUID, UUID)](
            "INSERT INTO BookAuthors (book_id, author_id) VALUES (?, ?)"
            ).updateMany(bookInfo.authors.getOrElse(Map.empty).keys.map(authorId => (bookId, UUID.fromString(authorId))).toList)

        // val insertLanguagesQuery = (bookId: UUID) =>
        //     Update[(UUID, Int)](
        //     "INSERT INTO BookLanguages (book_id, language_id) VALUES (?, ?)"
        //     ).updateMany(bookInfo.languages.map(languageId => (bookId, languageId)).toList)

        (for {
            bookId <- insertBookQuery
            _ <- insertBookCopies(bookId)
            _ <- insertAuthorsQuery(bookId)
           // _ <- insertLanguagesQuery(bookId)
        } yield bookId).transact(xa)
    }

    override def all(filter: BookFilter, pagination: Pagination, sort: BookSort): F[List[Book]] = {
        val selectFragment =
            fr"""
            SELECT DISTINCT
                b.book_id,
                b.catalog_added_at AS sort_date,
                b.title,
                LOWER(b.title) AS sort_title,
                (
                    SELECT MIN(LOWER(CONCAT_WS(' ', author.first_name, author.last_name)))
                    FROM BookAuthors sort_ba
                    JOIN Authors author ON sort_ba.author_id = author.author_id
                    WHERE sort_ba.book_id = b.book_id
                ) AS sort_author
            FROM Books b
            LEFT JOIN Publishers p ON b.publisher_id = p.publisher_id
            LEFT JOIN Book_Copies bc ON b.book_id = bc.book_id
            LEFT JOIN BookAuthors ba ON b.book_id = ba.book_id
            LEFT JOIN Authors a ON ba.author_id = a.author_id
            """

        val orderFragment = sort match
            case BookSort.New    => fr"sort_date DESC NULLS LAST, book_id"
            case BookSort.Author => fr"sort_author ASC NULLS LAST, book_id"
            case BookSort.Name   => fr"sort_title ASC, book_id"

        val finalOrderFragment = sort match
            case BookSort.New    => fr"pb.sort_date DESC NULLS LAST, pb.book_id, a.author_id, bc.copy_id"
            case BookSort.Author => fr"pb.sort_author ASC NULLS LAST, pb.book_id, a.author_id, bc.copy_id"
            case BookSort.Name   => fr"LOWER(pb.title) ASC, pb.book_id, a.author_id, bc.copy_id"

        val whereFragment: Fragment = Fragments.whereAndOpt(
            filter.authors.toNel.map(authors => Fragments.in(fr"a.author_id", authors)),
            filter.publishers.toNel.map(publishers => Fragments.in(fr"p.publisher_id", publishers)),
            filter.tags.toNel.map(tags => Fragments.or(tags.toList.map(tag => fr"$tag=any(b.tags)"): _*)),
            filter.publishedYear.map(year => fr"b.published_year = $year"),
            filter.inHallOnly.some.map(inHallOnly => fr"bc.in_library_only = $inHallOnly")
        )

        val pagedBookIds =
            fr"WITH paged_books AS (" |+| selectFragment |+| whereFragment |+|
                fr"ORDER BY " |+| orderFragment |+|
                fr"LIMIT ${pagination.limit} OFFSET ${pagination.skip})"

        val bookDetails =
            fr"""
            SELECT
                b.book_id,
                b.title,
                b.isbn,
                b.description,
                b.genre,
                b.published_year,
                p.publisher_name,
                p.publisher_id,
                b.tags,
                b.cover,
                bc.copy_id,
                bc.exemplar_number,
                bc.available,
                bc.in_library_only,
                a.author_id,
                a.first_name || ' ' || a.last_name as author_name
            FROM paged_books pb
            JOIN Books b ON b.book_id = pb.book_id
            LEFT JOIN Publishers p ON b.publisher_id = p.publisher_id
            LEFT JOIN Book_Copies bc ON b.book_id = bc.book_id
            LEFT JOIN BookAuthors ba ON b.book_id = ba.book_id
            LEFT JOIN Authors a ON ba.author_id = a.author_id
            ORDER BY
            """ |+| finalOrderFragment

        val statement = pagedBookIds |+| bookDetails

        statement
            .query[Book]
            .to[List]
            .map(aggregateBookRows)
            .transact(xa)
            .logError(e => s"Failed query: ${e.getMessage}")
    }

    private def aggregateBookRows(rows: List[Book]): List[Book] = {
        val byId = rows.groupBy(_.id)
        rows.map(_.id).distinct.map { id =>
            val bookRows = byId(id)
            val firstRow = bookRows.head
            val authors = bookRows.flatMap(_.bookInfo.authors).foldLeft(Map.empty[String, String])(_ ++ _)
            val copies = bookRows
                .flatMap(_.bookInfo.copies.toList.flatten)
                .distinctBy(_.copyId)

            firstRow.copy(
                bookInfo = firstRow.bookInfo.copy(
                    authors = Option.when(authors.nonEmpty)(authors),
                    copies = Option.when(copies.nonEmpty)(copies)
                )
            )
        }
    }

    override def all(): fs2.Stream[F, Book] =
        sql"""
            SELECT 
                b.book_id,
                b.title,
                b.isbn,
                b.description,
                b.genre,
                b.published_year,
                p.publisher_name,
                p.publisher_id,
                b.tags,
                b.cover,
                bc.copy_id,
                bc.exemplar_number,
                bc.available,
                bc.in_library_only,
                a.author_id,
                a.first_name || ' ' || a.last_name as author_name
            FROM Books b
            LEFT JOIN Publishers p ON b.publisher_id = p.publisher_id
            LEFT JOIN Book_Copies bc ON b.book_id = bc.book_id
            LEFT JOIN BookAuthors ba ON b.book_id = ba.book_id
            LEFT JOIN Authors a ON ba.author_id = a.author_id
            """
            .query[Book]
            .stream
            .transact(xa)

    override def find(id: UUID): F[Option[Book]] =
        sql"""
            SELECT 
                b.book_id,                
                b.title,
                b.isbn,
                b.description,
                b.genre,
                b.published_year,
                p.publisher_name,
                p.publisher_id,
                b.tags,
                b.cover,
                bc.copy_id,
                bc.exemplar_number,
                bc.available,
                bc.in_library_only,
                a.author_id,
                a.first_name || ' ' || a.last_name as author_name
            FROM Books b
            LEFT JOIN Publishers p ON b.publisher_id = p.publisher_id
            LEFT JOIN Book_Copies bc ON b.book_id = bc.book_id
            LEFT JOIN BookAuthors ba ON b.book_id = ba.book_id
            LEFT JOIN Authors a ON ba.author_id = a.author_id
            WHERE b.book_id = $id
            """
         .query[Book].option.transact(xa)
        

    override def update(id: UUID, bookInfo: BookInfo): F[Option[Book]] = 
        val updateQuery = 
            sql"""
                UPDATE Books
                SET 
                    isbn = ${bookInfo.isbn},
                    title = ${bookInfo.title},
                    description = ${bookInfo.description},
                    genre = ${bookInfo.genre},
                    published_year = ${bookInfo.publishedYear},
                    tags = ${bookInfo.tags},
                    cover = ${bookInfo.image},
                    publisher_id = ${bookInfo.publisherId}
                WHERE book_id = $id
            """.update.run

        val deleteAuthorsQuery =
            sql"""
            DELETE FROM BookAuthors
            WHERE book_id = $id
            """.update.run

        val insertAuthorsQuery = 
            Update[(UUID, UUID)](
            "INSERT INTO BookAuthors (book_id, author_id) VALUES (?, ?)"
            ).updateMany(bookInfo.authors.getOrElse(Map.empty).keys.map(authorId => (id, UUID.fromString(authorId))).toList)

        // val deleteLanguagesQuery =
        //     sql"""
        //     DELETE FROM BookLanguages
        //     WHERE book_id = $id
        //     """.update.run

        // val insertLanguagesQuery = 
        //     Update[(UUID, Int)](
        //     "INSERT INTO BookLanguages (book_id, language_id) VALUES (?, ?)"
        //     ).updateMany(bookInfo.languages.map(languageId => (id, languageId)).toList)
        (for {
            id <- updateQuery
            // _ <- deleteAuthorsQuery
            // _ <- insertAuthorsQuery // fixme
        } yield {
           id 
        }).transact(xa).flatMap(_ => find(id))


    override def delete(id: UUID): F[Int] = 
        sql"""
            DELETE FROM Books
            WHERE book_id = $id
        """
        .update
        .run
        .transact(xa)

    override def possibleFilters(): F[BookFilter] =
        sql"""
            SELECT
            array(
                SELECT DISTINCT a.first_name || ' ' || a.last_name 
                FROM Authors a
            ) AS authors,
            array(
                SELECT DISTINCT p.publisher_name 
                FROM Publishers p
            ) AS publishers,
            array(
                SELECT DISTINCT unnest(tags) 
                FROM books
            ) AS tags,
            max(published_year),
            false
            FROM books
        """.query[BookFilter].option.transact(xa).map(_.getOrElse(BookFilter()))

}

object LiveBooks {

    given bookFilterRead: Read[BookFilter] = Read[
    (
        List[String],
        List[String],
        List[String],
        Option[Int],
        Boolean
    )
    ].map { case (authors, publishers, tags, year, isHallOnly) =>
       BookFilter(authors, publishers, tags, year, isHallOnly)
    }

    given bookRead: Read[Book] = Read[
        (UUID,        
        String, 
        Option[String], 
        Option[String], 
        Option[String], 
        Option[Int], 
        Option[String], 
        Option[Int], 
        Option[List[String]], 
        Option[String], 
        UUID, 
        Int, 
        Boolean, 
        Boolean, 
        Option[UUID], 
        Option[String])
        
    ].map {
        case (
            id,
            title,
            isbn,
            description,
            genre,
            publishedYear,
            publisherName,
            publisherId,
            tags,
            image,
            copyId,
            exemplarNumber,
            available,
            inLibraryOnly,
            authorId,
            authorName
            ) =>
            Book(
                id = id,
                bookInfo = BookInfo(
                title = title,
                isbn = isbn,
                description = description,
                authors = authorId.map(aid => Map(aid.toString() -> (authorName.getOrElse("")))),
                publisherId = publisherId,
                publisherName = publisherName,
                genre = genre,
                publishedYear = publishedYear,
                tags = tags,
                image = image,
                copies = Some(List(BookCopy(copyId, exemplarNumber, available, inLibraryOnly)))
                )
            )
        }
    

    def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveBooks[F]] = new LiveBooks[F](xa).pure
}