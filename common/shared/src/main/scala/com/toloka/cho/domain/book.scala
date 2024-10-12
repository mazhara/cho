package com.toloka.cho.domain

import java.awt.print.Book
import java.util.UUID

object book {
  case class Book(
    id: UUID,
    bookInfo: BookInfo
  )

  case class BookInfo(   
    title: String,
    isbn: Option[String],
    description: Option[String],
    authors: Option[Map[String, String]],     // Map of author ids ->  names 
    publisherId: Option[Int],
    publisherName: Option[String],           
    genre: Option[String],
    publishedYear: Option[Int],
    tags: Option[List[String]],
    image: Option[String],
    copies: Option[List[BookCopy]] // non empty
  )

  case class BookCopy(
    copyId: UUID,
    exemplarNumber: Int,
    available: Boolean,
    inLibraryOnly: Boolean
  )

  object BookInfo {
    val empty: BookInfo = BookInfo("",None, None, None, None, None, None, None, None, None, None)

    def minimal( 
    title: String): BookInfo = empty.copy(title = title)
  }

  final case class BookFilter(
    authors: List[String] = List(),
    publishers: List[String] = List(),
    tags: List[String] = List(),
    publishedYear: Option[Int] = None,
    inHallOnly: Boolean = false
  )
}