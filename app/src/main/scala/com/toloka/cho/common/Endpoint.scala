package com.toloka.cho.common

import cats.effect.IO
import io.circe.Encoder
import io.circe.syntax.*
import tyrian.*
import tyrian.http.*
import io.circe.parser.*
import com.toloka.cho.core.Session


trait Endpoint[M] {
  val location: String
  val method: Method
  val onResponse: Response => M
  val onError: HttpError => M

  def call[A: Encoder](payload: A): Cmd[IO, M] =
    internalCall(payload, None)

  def call(): Cmd[IO, M] =
    internalCall(None)
  
  // Only issue the request when a token is actually present; otherwise there is nothing to
  // authorize with, and silently sending an unauthenticated request could trigger a server-side
  // 401/403 the caller isn't expecting.
  def callAuthorized(): Cmd[IO, M] =
    Session.getUserToken() match
      case Some(token) => internalCall(Some(token))
      case None        => Cmd.None

  private def internalCall[A: Encoder](payload: A, authorization: Option[String]): Cmd[IO, M] =
    Http.send(
      Request(
        url = location,
        method = method,
        headers = authorization.map(token => Header("Authorization", token)).toList,
        body = Body.json(payload.asJson.toString),
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onResponse, onError)
    )

  private def internalCall(authorization: Option[String]): Cmd[IO, M] =
    Http.send(
      Request(
        url = location,
        method = method,
        headers = authorization.map(token => Header("Authorization", token)).toList,
        body = Body.Empty,
        timeout = Request.DefaultTimeOut,
        withCredentials = false
      ),
      Decoder[M](onResponse, onError)
    )
}

object Endpoint {
  def onResponse[A: io.circe.Decoder, Msg](
      valueCb: A => Msg,
      errorCb: String => Msg
  ): Response => Msg =
    response =>
      response.status match {
        case Status(s, _) if s >= 200 && s < 300 =>
          val json   = response.body
          val parsed = parse(json).flatMap(_.as[A])
          parsed match {
            case Left(parsingError) => errorCb(s"Parsing error: $parsingError")
            case Right(value)       => valueCb(value)
          }
        case Status(_, message) =>
          errorCb(s"Error: $message")
      }

  def onResponseText[Msg](
    valueCb: String => Msg,
    errorCb: String => Msg
  ): Response => Msg =
    response =>
        response.status match {
          case Status(s, _) if s >= 200 && s < 300 =>
            valueCb(response.body)
          case Status(s, _) if s >= 400 && s < 500 =>
            val json = response.body
            val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
            parsed match {
              case Left(e)  => errorCb(s"Error: $e")
              case Right(e) => errorCb(e)
            }
          case _ =>
            errorCb("Unkown reply from server.")
        }
}