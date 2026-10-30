package com.toloka.cho.server.http.validation

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
import com.toloka.cho.domain.AuthValidation
import com.google.i18n.phonenumbers.{NumberParseException, PhoneNumberUtil}

object validators {

    sealed trait ValidationFailure(val errorMessage: String)
    case class EmptyField(fieldName: String) extends ValidationFailure(s"'$fieldName' is empty")
    case class InvalidUrl(fieldName: String) extends ValidationFailure(s"'$fieldName' is not a valid URL")
    case class InvalidEmail(fieldName: String) extends ValidationFailure(s"'$fieldName' is not a valid Email")
    case object ConsentRequired extends ValidationFailure("Consent to personal data processing is required")
    case object InvalidPassword extends ValidationFailure("Password must contain 8-64 characters, including an uppercase letter, a lowercase letter, a number, and a symbol")
    case object InvalidPhone extends ValidationFailure("Enter a valid international phone number with a country code, for example +380501234567")


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

    def validatePassword(password: String): ValidationResult[String] =
        if (AuthValidation.passwordIsValid(password)) password.validNel
        else InvalidPassword.invalidNel

    def validatePhone(phone: String): ValidationResult[String] =
        if (!AuthValidation.phoneFormatIsValid(phone)) InvalidPhone.invalidNel
        else {
          val phoneUtil = PhoneNumberUtil.getInstance()
          try {
            val parsed = phoneUtil.parse(AuthValidation.normalizePhone(phone), "ZZ")
            if (phoneUtil.isValidNumber(parsed))
              phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164).validNel
            else InvalidPhone.invalidNel
          } catch {
            case _: NumberParseException => InvalidPhone.invalidNel
          }
        }

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

        val validUserPassword = validatePassword(newUserInfo.password)
        val validPhone = newUserInfo.phone match {
          case Some(phone) => validatePhone(phone).map(Some(_))
          case None        => None.validNel
        }
        val validConsent =
          if (newUserInfo.dataProcessingConsent) true.validNel
          else ConsentRequired.invalidNel

        (
        validUserEmail,
        validUserPassword,
        newUserInfo.firstName.validNel,
        newUserInfo.lastName.validNel,
        newUserInfo.company.validNel,
        validPhone,
        validConsent
        ).mapN(NewUserInfo.apply)
    }

    given newPasswordInfoValidator: Validator[NewPasswordInfo] = (newPasswordInfo: NewPasswordInfo) =>
    {
        val validOldPassword =
        validateRequired(newPasswordInfo.oldPassword, "oldPassword")(_.nonEmpty)
        val validNewPassword =
        validatePassword(newPasswordInfo.newPassword)

        (
        validOldPassword,
        validNewPassword
        ).mapN(NewPasswordInfo.apply)
    }

    given forgotPasswordInfoValidator: Validator[ForgotPasswordInfo] = (info: ForgotPasswordInfo) =>
      validateRequired(info.email, "email")(_.nonEmpty)
        .andThen(email => validateEmail(email, "email"))
        .map(ForgotPasswordInfo.apply)

    given recoverPasswordInfoValidator: Validator[RecoverPasswordInfo] = (info: RecoverPasswordInfo) => {
      val validEmail = validateRequired(info.email, "email")(_.nonEmpty)
        .andThen(email => validateEmail(email, "email"))
      val validToken = validateRequired(info.token, "token")(_.nonEmpty)
      val validPassword = validatePassword(info.newPassword)

      (validEmail, validToken, validPassword).mapN(RecoverPasswordInfo.apply)
    }
  
}
