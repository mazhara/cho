package com.toloka.cho.admin.http.validation

import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*

import java.net.URL
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import com.toloka.cho.admin.domain.book.BookInfo

object validators {

    sealed trait ValidationFailure(val errorMessage: String)
    case class EmptyField(fieldName: String) extends ValidationFailure(s"'$fieldName' is empty")
    case class InvalidUrl(fieldName: String) extends ValidationFailure(s"'$fieldName' is not a valid URL")

    type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

    trait Validator[A] {
        def validate(value: A): ValidationResult[A]
    }

    def validateRequired[A](field: A, fieldName: String)(
      required: A => Boolean
    ): ValidationResult[A] =
        if (required(field)) field.validNel
        else EmptyField(fieldName).invalidNel 

    def validateUrl(field: String, fieldName: String): ValidationResult[String] =
        Try(URL(field).toURI()) match {
        case Success(_)         => field.validNel
        case Failure(exception) => InvalidUrl(fieldName).invalidNel
        }

    given bookInfoValidator: Validator[BookInfo] = (bookInfo: BookInfo) => {
        val BookInfo(
            name,
            author,
            description,
            publisher,
            year,
            inHallOnly,
            tags,
            image,
        ) = bookInfo

        val validName = validateRequired(name, "name")(_.nonEmpty)
        val validAuthor = validateRequired(author, "author")(_.nonEmpty)

        (
            validName,
            validAuthor,
            description.validNel,
            publisher.validNel,
            year.validNel,
            inHallOnly.validNel,
            tags.validNel,
            image.validNel
        ).mapN(BookInfo.apply)
    }
  
}
