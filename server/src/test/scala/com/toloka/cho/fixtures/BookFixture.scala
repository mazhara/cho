package com.toloka.cho.fixtures

import cats.syntax.all.*

import java.util.UUID
import com.toloka.cho.admin.palyground.BooksPlayground.bookInfo
import com.toloka.cho.domain.book.Book
import com.toloka.cho.domain.book.BookInfo
import com.toloka.cho.domain.book.BookCopy


trait BookFixture {

  val NotFoundBookUuid = UUID.fromString("6ea79557-3112-4c84-a8f5-1d1e2c300941")

  val AwesomeBookUuid = UUID.fromString("843df718-ec6e-4d49-9289-f799c0f40064")

    val sampleCopy = BookCopy(
    copyId = UUID.fromString("90bd1ae7-14db-4b36-ac9e-fa1e23c65d1a"),
    exemplarNumber = 1,
    available = true,
    inLibraryOnly = false
  )

  val AwesomeBook = Book(
    AwesomeBookUuid,
    BookInfo(   
      title = "Harry Potter and the Philosopher Stone",
      isbn = Some("978-3-16-148410-0"),
      description = Some("A young wizard discovers his magical heritage."),
      authors = Some(Map("90bd1ae7-14db-4b36-ac9e-fa1e23c65d1a" -> "J.K. Rowling")),
      publisherId = Some(1),
      publisherName = Some("Broom Publish"),
      genre = Some("Fantasy"),
      publishedYear = Some(1997),
      tags = Some(List("fantasy", "magic", "children")),
      image = None,
      copies = Some(List(sampleCopy))
    )
  )

  val InvalidBook = Book(
    null,
    BookInfo.empty
  )

  val UpdatedAwesomeBook = Book(
    AwesomeBookUuid,
    BookInfo(
      isbn = Some("978-3-16-148410-0"),
      title = "Harry Potter and the Stone",
      description = Some("Updated bool description"),
      authors = Some(Map("90bd1ae7-14db-4b36-ac9e-fa1e23c65d1a" -> "J.K. Rowling")),
      publisherId = Some(1),
      publisherName = Some("Broom Publish"),
      genre = Some("Fantasy"),
      publishedYear = Some(1997),
      tags = Some(List("fantasy", "magic", "children")),
      image = None,
      copies = Some(List(sampleCopy))
    )
  )

  val ChoBookWithNotFoundId = AwesomeBook.copy(id = NotFoundBookUuid)

  val AnotherAwesomeBookUuid = UUID.fromString("19a941d0-aa19-477b-9ab0-a7033ae65c2b")
  val AnotherAwesomeBook     = AwesomeBook.copy(id = AnotherAwesomeBookUuid)

  val ChoAwesomeBook =
    AwesomeBook.copy(bookInfo = AwesomeBook.bookInfo.copy(title = "the history of ukranian tractors"))

  val NewBookUuid = UUID.fromString("efcd2a64-4463-453a-ada8-b1bae1db4377")
  val AwesomeNewBook = BookInfo(
    isbn = Some("978-0-00-000000-0"),
    title = "Hobbit",
    description = Some("An awesome book"),
    authors = Some(Map("90bd1ae7-14db-4b36-ac9e-fa1e23c65d1a" -> "J.K. Rowling")),
    publisherId = Some(1),
    publisherName = Some("Broom Publish"),
    genre = Some("Fantasy"),
    publishedYear = Some(1973),
    tags = Some(List("fantasy", "bestseller", "children")),
    image = None,
    copies = None
  )
}
