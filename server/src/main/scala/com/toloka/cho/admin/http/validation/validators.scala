package com.toloka.cho.admin.http.validation

import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*

import java.net.URL
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import com.toloka.cho.domain.book.BookInfo
import com.toloka.cho.domain.auth.*
import com.toloka.cho.domain.AuthorInfo

object validators {

    sealed trait ValidationFailure(val errorMessage: String)
    case class EmptyField(fieldName: String) extends ValidationFailure(s"'$fieldName' is empty")
    case class InvalidUrl(fieldName: String) extends ValidationFailure(s"'$fieldName' is not a valid URL")
    case class InvalidEmail(fieldName: String) extends ValidationFailure(s"'$fieldName' is not a valid Email")

    type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

    trait Validator[A] {
        def validate(value: A): ValidationResult[A]
    }

    val emailRegex =
        """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

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

    def validateEmail(field: String, fieldName: String): ValidationResult[String] =
        if (emailRegex.findFirstMatchIn(field).isDefined) field.validNel
        else InvalidEmail(fieldName).invalidNel

    given authorInfoValidator: Validator[AuthorInfo] = (authorInfo: AuthorInfo) => {
        val AuthorInfo (
        firstName,
        lastName,
        authorType
        ) = authorInfo
        val validLastName = validateRequired(lastName, "lastName")(_.nonEmpty)

        (
            firstName.validNel,
            validLastName,
            authorType.validNel
        ).mapN(AuthorInfo.apply)
    }

    given bookInfoValidator: Validator[BookInfo] = (bookInfo: BookInfo) => {
        val BookInfo(
            isbn,
            title,
            description,
            authors,
            publisherId,
            publisherName,
            genre,
            publishedYear,
            tags,
            image,
            copies // todo not empty
        ) = bookInfo

        val validTitle = validateRequired(title, "title")(_.nonEmpty)
        val validAuthors = validateRequired(authors, "authors")(_.nonEmpty)

        (
            isbn.validNel,
            validTitle,
            description.validNel,
            authors.validNel,
            publisherId.validNel,
            publisherName.validNel,
            genre.validNel,
            publishedYear.validNel,
            tags.validNel,
            image.validNel,
            copies.validNel
        ).mapN(BookInfo.apply)
    }

    given loginInfoValidator: Validator[LoginInfo] = (loginInfo: LoginInfo) => {
        val validUserEmail = validateRequired(loginInfo.email, "email")(_.nonEmpty)
        .andThen(e => validateEmail(e, "mail"))

        val validUserPassword = validateRequired(loginInfo.password, "password")(_.nonEmpty)
        (validUserEmail, validUserPassword).mapN(LoginInfo.apply)
    }

    given newUserInfoValidator: Validator[NewUserInfo] = (newUserInfo: NewUserInfo) => {
        val validUserEmail = validateRequired(newUserInfo.email, "email")(_.nonEmpty)
        .andThen(e => validateEmail(e, "mail"))

        val validUserPassword = validateRequired(newUserInfo.password, "password")(_.nonEmpty)

        (
        validUserEmail,
        validUserPassword,
        newUserInfo.firstName.validNel,
        newUserInfo.lastName.validNel,
        newUserInfo.company.validNel
        ).mapN(NewUserInfo.apply)
    }

    given newPasswordInfoValidator: Validator[NewPasswordInfo] = (newPasswordInfo: NewPasswordInfo) =>
    {
        val validOldPassword =
        validateRequired(newPasswordInfo.oldPassword, "oldPassword")(_.nonEmpty)
        val validNewPassword =
        validateRequired(newPasswordInfo.newPassword, "newPassword")(_.nonEmpty)

        (
        validOldPassword,
        validNewPassword
        ).mapN(NewPasswordInfo.apply)
    }
  
}
