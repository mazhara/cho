package com.toloka.cho.domain

import java.awt.print.Book
import java.util.UUID

object book {
  case class Book(
    id: UUID,
    bookInfo: BookInfo
  )

  case class BookInfo(
    isbn: String,
    title: String,
    description: Option[String],
    authors: Map[String, String],     // Map of author ids ->  names 
    publisherId: Option[Int],
    publisherName: Option[String],           
    genre: Option[String],
    publishedYear: Option[Int],
    tags: Option[List[String]],
    image: Option[String],
    copies: List[BookCopy] 
  )

  case class BookCopy(
    copyId: UUID,
    exemplarNumber: Int,
    available: Boolean,
    inLibraryOnly: Boolean
  )

  object BookInfo {
    val empty: BookInfo = BookInfo("", "", None, Map.empty, None, None, None, None, None, None, List.empty)

    def minimal( 
    isbn: String,
    title: String,
    description: String): BookInfo = empty.copy(isbn = isbn,
      title = title)
  }

  final case class BookFilter(
    authors: List[String] = List(),
    publishers: List[String] = List(),
    tags: List[String] = List(),
    publishedYear: Option[Int] = None,
    inHallOnly: Boolean = false
  )
}