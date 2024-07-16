package com.toloka.cho.admin.domain

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
  }
}
