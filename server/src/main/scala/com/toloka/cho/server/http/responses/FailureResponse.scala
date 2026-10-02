package com.toloka.cho.server.http.responses

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}

final case class FailureResponse(error: String)

object FailureResponse {
  implicit val encoder: Encoder[FailureResponse] = deriveEncoder[FailureResponse]
  implicit val decoder: Decoder[FailureResponse] = deriveDecoder[FailureResponse]
}
