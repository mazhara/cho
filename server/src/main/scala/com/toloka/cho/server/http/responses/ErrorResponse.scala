package com.toloka.cho.server.http.responses

import io.circe.{Decoder, Encoder, HCursor, Json}
import org.http4s.Status

/** Base trait for all API errors */
sealed trait ApiError extends Product with Serializable {
  def message: String
  def status: Status
  def errorType: String
}

/** Error hierarchy */
object ApiError {
  
  // Not Found errors (404)
  final case class NotFound(resourceType: String, identifier: String) extends ApiError {
    override def message: String = s"$resourceType '$identifier' not found"
    override def status: Status = Status.NotFound
    override def errorType: String = "not_found"
  }

  // Validation errors (400)
  final case class ValidationError(details: String) extends ApiError {
    override def message: String = details
    override def status: Status = Status.BadRequest
    override def errorType: String = "validation_error"
  }

  // Authentication errors (401)
  final case class AuthenticationError(details: String) extends ApiError {
    override def message: String = details
    override def status: Status = Status.Unauthorized
    override def errorType: String = "authentication_error"
  }

  // Authorization errors (403)
  final case class AuthorizationError(details: String) extends ApiError {
    override def message: String = details
    override def status: Status = Status.Forbidden
    override def errorType: String = "authorization_error"
  }

  // Internal server errors (500)
  final case class InternalServerError(details: String) extends ApiError {
    override def message: String = details
    override def status: Status = Status.InternalServerError
    override def errorType: String = "internal_server_error"
  }

  // Conflict errors (409)
  final case class ConflictError(details: String) extends ApiError {
    override def message: String = details
    override def status: Status = Status.Conflict
    override def errorType: String = "conflict_error"
  }

  // Manual encoder for ApiError (cannot use deriveEncoder because of Throwable issues)
  implicit val encoder: Encoder[ApiError] = Encoder.instance { error =>
    Json.obj(
      "error" -> Json.fromString(error.message),
      "errorType" -> Json.fromString(error.errorType),
      "status" -> Json.fromInt(error.status.code)
    )
  }

  // Manual decoder for ApiError
  implicit val decoder: Decoder[ApiError] = Decoder.instance { cursor =>
    for {
      errorType <- cursor.downField("errorType").as[String]
      message <- cursor.downField("error").as[String]
      status <- cursor.downField("status").as[Int]
    } yield errorType match {
      case "not_found" => NotFound(message, "")
      case "validation_error" => ValidationError(message)
      case "authentication_error" => AuthenticationError(message)
      case "authorization_error" => AuthorizationError(message)
      case "internal_server_error" => InternalServerError(message)
      case "conflict_error" => ConflictError(message)
      case _ => ValidationError(message)
    }
  }
}

/** Response wrapper for errors */
final case class ErrorResponse(
    error: String,
    errorType: String,
    status: Int,
    details: Option[String] = None
)

object ErrorResponse {
  def fromApiError(e: ApiError): ErrorResponse = ErrorResponse(
    error = e.message,
    errorType = e.errorType,
    status = e.status.code,
    details = None
  )

  import io.circe.generic.semiauto._
  implicit val encoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  implicit val decoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
}
