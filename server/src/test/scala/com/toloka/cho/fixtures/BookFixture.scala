package com.toloka.cho.fixtures

import cats.syntax.all.*

import java.util.UUID
import com.toloka.cho.admin.palyground.BooksPlayground.bookInfo
import com.toloka.cho.domain.book.Book
import com.toloka.cho.domain.book.BookInfo


trait BookFixture {

  val NotFoundBookUuid = UUID.fromString("6ea79557-3112-4c84-a8f5-1d1e2c300948")

  val AwesomeBookUuid = UUID.fromString("843df718-ec6e-4d49-9289-f799c0f40064")

  val AwesomeBook = Book(
    AwesomeBookUuid,
    BookInfo(
      "Harry Potter",
      "JKR",
      "An awesome book",
      "broom publish",
      1991,
      false,
      Some(List("fantasy", "bestseller", "children")),
      None
    )
  )

  val InvalidBook = Book(
    null,
    BookInfo.empty
  )

  val UpdatedAwesomeBook = Book(
    AwesomeBookUuid,
    BookInfo(
      "Casual vacancy",
      "JKR",
      "An awesome book",
      "broom publish",
      1991,
      false,
      Some(List("fantasy", "bestseller", "children")),
      None
    )
  )

  val ChoewBook = BookInfo(
    "Lord of the ring",
    "T",
    "An awesome book",
    "elf publish",
    1991,
    false,
    Some(List("fantasy", "bestseller", "children")),
    None
  )

  val ChoBookWithNotFoundId = AwesomeBook.copy(id = NotFoundBookUuid)

  val AnotherAwesomeBookUuid = UUID.fromString("19a941d0-aa19-477b-9ab0-a7033ae65c2b")
  val AnotherAwesomeBook     = AwesomeBook.copy(id = AnotherAwesomeBookUuid)

  val ChoAwesomeBook =
    AwesomeBook.copy(bookInfo = AwesomeBook.bookInfo.copy(name = "the history of ukranian tractors"))

  val NewBookUuid = UUID.fromString("efcd2a64-4463-453a-ada8-b1bae1db4377")
  val AwesomeNewBook = BookInfo(
    "Hobit",
    "T",
    "An awesome book",
    "elf publish",
    1973,
    false,
    Some(List("fantasy", "bestseller", "children")),
    None
  )
}
