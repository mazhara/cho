package com.toloka.cho.pages

import cats.effect.IO
import io.circe.Encoder
import io.circe.parser.parse
import io.circe.syntax.*
import com.toloka.cho.common.Constants
import tyrian.Cmd
import tyrian.http.*

object AuthApi {
  def post[A: Encoder, M](
                           endpoint: String,
                           payload: A,
                           onResponse: Response => M,
                           onError: HttpError => M
                         ): Cmd[IO, M] =
    Http.send(
      Request(
        url = endpoint,
        method = Method.Post,
        headers = List(Header("Content-Type", "application/json")),
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

  def endpointForLogin: String = Constants.endpoints.login
  def endpointForSignup: String = Constants.endpoints.signup
  def endpointForForgotPassword: String = Constants.endpoints.forgotPassword
  def endpointForRecoverPassword: String = Constants.endpoints.recoverPassword
}

object AuthSession {
  private val authorizationKey = "choAuthorization"
  private val roleKey = "choRole"

  def save(authorization: String, role: String): IO[Unit] =
    IO {
      org.scalajs.dom.window.sessionStorage.setItem(authorizationKey, authorization)
      org.scalajs.dom.window.sessionStorage.setItem(roleKey, role)
    }

  def authorization: Option[String] =
    Option(org.scalajs.dom.window.sessionStorage.getItem(authorizationKey))

  def role: Option[String] =
    Option(org.scalajs.dom.window.sessionStorage.getItem(roleKey))

  def canAccessAdmin: Boolean =
    role.exists(role => role == "ADMIN" || role == "LIBRARIAN")

  def clear: IO[Unit] =
    IO {
      org.scalajs.dom.window.sessionStorage.removeItem(authorizationKey)
      org.scalajs.dom.window.sessionStorage.removeItem(roleKey)
    }
}
