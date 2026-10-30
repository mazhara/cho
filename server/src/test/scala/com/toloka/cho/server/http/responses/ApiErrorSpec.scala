package com.toloka.cho.server.http.responses

import io.circe.Json
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ApiErrorSpec extends AnyFreeSpec with Matchers {
  "ApiError codec" - {
    val errors: List[ApiError] = List(
      ApiError.NotFound("Book", "an 'identifier' with quotes"),
      ApiError.NotFound("", ""),
      ApiError.ValidationError("Invalid input"),
      ApiError.AuthenticationError("Sign in required"),
      ApiError.AuthorizationError("Access denied"),
      ApiError.InternalServerError("Unavailable"),
      ApiError.ConflictError("Already exists")
    )

    errors.zipWithIndex.foreach { case (error, index) =>
      s"round-trips error $index without losing its fields or message" in {
        val json = ApiError.encoder(error)
        val decoded = ApiError.decoder.decodeJson(json)

        decoded shouldBe Right(error)
        decoded.map(_.message) shouldBe Right(error.message)
        json.hcursor.get[String]("error") shouldBe Right(error.message)
        json.hcursor.get[Int]("status") shouldBe Right(error.status.code)
        json.as[ErrorResponse] shouldBe Right(ErrorResponse.fromApiError(error))
      }
    }

    "rejects a not_found payload with missing resource metadata instead of inventing fields" in {
      val json = Json.obj(
        "error" -> Json.fromString("Book '123' not found"),
        "errorType" -> Json.fromString("not_found"),
        "status" -> Json.fromInt(404)
      )

      ApiError.decoder.decodeJson(json).isLeft shouldBe true
    }
  }
}
