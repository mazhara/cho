package com.toloka.cho.pages

import cats.effect.IO
import io.circe.Encoder
import io.circe.parser.parse
import io.circe.syntax.*
import com.toloka.cho.common.Constants
import com.toloka.cho.domain.auth.LoginResponse
import tyrian.Cmd
import tyrian.http.*


object AuthApi:
  def post[A: Encoder, M](
      endpoint: String,
      payload: A,
      onResponse: Response => M,
      onError: HttpError => M,
      additionalHeaders: List[Header] = Nil,
      method: Method = Method.Post
  ): Cmd[IO, M] =
    Http.send(
      Request(
        url = endpoint,
        method = method,
        headers = Header("Content-Type", "application/json") :: additionalHeaders,
        body = Body.json(payload.asJson.noSpaces),
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onResponse, onError)
    )

  def failure(response: Response): String =
    parse(response.body)
      .flatMap(_.hcursor.get[String]("error"))
      .getOrElse(s"Request failed (${response.status.code})")

  def endpointForLogin: String           = Constants.endpoints.login
  def endpointForSignup: String          = Constants.endpoints.signup
  def endpointForForgotPassword: String  = Constants.endpoints.forgotPassword
  def endpointForRecoverPassword: String = Constants.endpoints.recoverPassword


object AuthSession:
  private val authorizationKey = "choAuthorization"
  private val roleKey          = "choRole"
  private val emailKey         = "choEmail"
  private val firstNameKey     = "choFirstName"
  private val lastNameKey      = "choLastName"

  def save(authorization: String, user: LoginResponse): IO[Unit] =
    IO {
      org.scalajs.dom.window.sessionStorage.setItem(authorizationKey, authorization)
      org.scalajs.dom.window.sessionStorage.setItem(roleKey, user.role)
      org.scalajs.dom.window.sessionStorage.setItem(emailKey, user.email)
      setOptional(firstNameKey, user.firstName)
      setOptional(lastNameKey, user.lastName)
    }

  def authorization: Option[String] =
    stored(authorizationKey)

  def email: Option[String] =
    stored(emailKey)

  def firstName: Option[String] =
    stored(firstNameKey)

  def lastName: Option[String] =
    stored(lastNameKey)

  def role: Option[String] =
    stored(roleKey)

  private def stored(key: String): Option[String] =
    Option(org.scalajs.dom.window.sessionStorage.getItem(key)).map(_.trim).filter(_.nonEmpty)

  private def setOptional(key: String, value: Option[String]): Unit =
    value.filter(_.trim.nonEmpty) match
      case Some(nonEmptyValue) => org.scalajs.dom.window.sessionStorage.setItem(key, nonEmptyValue)
      case None                => org.scalajs.dom.window.sessionStorage.removeItem(key)

  def canAccessAdmin: Boolean =
    authorization.nonEmpty &&
      email.nonEmpty &&
      role.exists(role => role == "ADMIN" || role == "LIBRARIAN")

  def displayName: Option[String] =
    List(firstName, lastName).flatten.map(_.trim).filter(_.nonEmpty) match
      case names if names.nonEmpty => Some(names.mkString(" "))
      case _                       => email

  def initials: Option[String] =
    val names = List(firstName, lastName).flatten.map(_.trim).filter(_.nonEmpty)
    names match
      case first :: last :: _ => Some(s"${first.head}${last.head}".toUpperCase)
      case first :: Nil       => Some(first.head.toString.toUpperCase)
      case Nil                => email.flatMap(_.headOption).map(_.toString.toUpperCase)

  def clear: IO[Unit] =
    IO {
      org.scalajs.dom.window.sessionStorage.removeItem(authorizationKey)
      org.scalajs.dom.window.sessionStorage.removeItem(roleKey)
      org.scalajs.dom.window.sessionStorage.removeItem(emailKey)
      org.scalajs.dom.window.sessionStorage.removeItem(firstNameKey)
      org.scalajs.dom.window.sessionStorage.removeItem(lastNameKey)
    }
