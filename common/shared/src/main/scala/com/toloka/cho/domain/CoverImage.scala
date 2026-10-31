package com.toloka.cho.domain

import java.util.Base64

object CoverImage {
  val maxBytes: Int = 2 * 1024 * 1024
  val maxBase64Length: Int = ((maxBytes + 2) / 3) * 4
  val sizeError = "Cover image must be 2 MB or smaller."
  val formatError = "Choose a PNG or JPEG image, or paste valid PNG/JPEG base64 data."

  def normalize(input: String): Either[String, String] = {
    val value = input.trim
    val (declaredType, payload) =
      if (value.startsWith("data:image/png;base64,"))
        (Some("image/png"), value.drop("data:image/png;base64,".length))
      else if (value.startsWith("data:image/jpeg;base64,"))
        (Some("image/jpeg"), value.drop("data:image/jpeg;base64,".length))
      else (None, value)

    if (payload.isEmpty && declaredType.nonEmpty) Left(formatError)
    else if (payload.isEmpty) Right("")
    else if (payload.length > maxBase64Length) Left(sizeError)
    else {
      val decoded =
        try Right(Base64.getDecoder.decode(payload))
        catch { case _: IllegalArgumentException => Left(formatError) }

      decoded.flatMap { bytes =>
        if (bytes.length > maxBytes) Left(sizeError)
        else mimeType(bytes).filter(_ => completeImage(bytes)) match {
          case Some(actualType) if declaredType.forall(_ == actualType) =>
            Right(Base64.getEncoder.encodeToString(bytes))
          case _ => Left(formatError)
        }
      }
    }
  }

  private def mimeType(bytes: Array[Byte]): Option[String] =
    if (bytes.length >= 8 &&
        bytes.take(8).map(_ & 0xff).toList == List(137, 80, 78, 71, 13, 10, 26, 10))
      Some("image/png")
    else if (bytes.length >= 3 &&
        (bytes(0) & 0xff) == 255 && (bytes(1) & 0xff) == 216 && (bytes(2) & 0xff) == 255)
      Some("image/jpeg")
    else None

  private def completeImage(bytes: Array[Byte]): Boolean =
    if ((bytes(0) & 0xff) == 255) {
      bytes.length >= 4 && (bytes(bytes.length - 2) & 0xff) == 255 &&
        (bytes.last & 0xff) == 217
    } else {
      var offset = 8
      var hasHeader = false
      var hasData = false
      while (offset <= bytes.length - 12) {
        val length = (0 until 4).foldLeft(0L)((n, i) => (n << 8) | (bytes(offset + i) & 0xff).toLong)
        if (length > bytes.length - offset - 12) return false
        val chunkType = bytes.slice(offset + 4, offset + 8).map(_.toChar).mkString
        if (!hasHeader && (chunkType != "IHDR" || length != 13)) return false
        if (chunkType == "IHDR") hasHeader = true
        if (chunkType == "IDAT") hasData = true
        if (chunkType == "IEND") return hasHeader && hasData && length == 0
        offset += length.toInt + 12
      }
      false
    }

  def dataUrl(base64: String): String = {
    val mime = if (base64.startsWith("/9j/")) "image/jpeg" else "image/png"
    s"data:$mime;base64,$base64"
  }
}
