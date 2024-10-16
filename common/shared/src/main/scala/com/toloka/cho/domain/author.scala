package com.toloka.cho.domain

  import java.util.UUID

case class Author(
    id: UUID,
    authorInfo: AuthorInfo
)

case class AuthorInfo(
    firstName: Option[String],
    lastName: String,
    authorType: AuthorType
)

enum AuthorType:
  case Author
  case Composer
  case Editor
  case Illustrator

object AuthorType:

  def fromString(value: String): Option[AuthorType] = value.toLowerCase match
    case "author"      => Some(Author)
    case "composer"    => Some(Composer)
    case "editor"      => Some(Editor)
    case "illustrator" => Some(Illustrator)
    case _             => None


  def toString(authorType: AuthorType): String = authorType match
    case Author      => "Author"
    case Composer    => "Composer"
    case Editor      => "Editor"
    case Illustrator => "Illustrator"
