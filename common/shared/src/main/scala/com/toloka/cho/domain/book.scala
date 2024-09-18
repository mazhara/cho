package com.toloka.cho.domain

import java.awt.print.Book
import java.util.UUID

object book {
  case class Book(
    id: UUID,
    bookInfo: BookInfo
  )

  case class BookInfo(
    name: String,
    author: String,
    description: String,
    publisher: String,
    year: Int,
    inHallOnly: Boolean,
    tags: Option[List[String]],
    image: Option[String]
  )


  object BookInfo {
    val empty: BookInfo = BookInfo("", "", "", "",0, false, None, None)

    def minimal( name: String,
    author: String,
    description: String,
    publisher: String,
    year: Int,
    inHallOnly: Boolean): BookInfo = BookInfo(
      name = name,
      author = author,
      description = description,
      publisher = publisher,
      year = year,
      inHallOnly = inHallOnly,
      tags = None,
      image = None
    )
  }

  final case class BookFilter(
    authors: List[String] = List(),
    publishers: List[String] = List(),
    tags: List[String] = List(),
    year: Option[Int] = None,
    inHallOnly: Boolean = false
  )
}