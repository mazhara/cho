package com.toloka.cho.server.http.validation

import com.toloka.cho.domain.CoverImage
import com.toloka.cho.server.fixtures.BookFixture
import com.toloka.cho.server.http.validation.validators.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

class CoverImageSpec extends AnyFreeSpec with Matchers with BookFixture {
  private def image(format: String): String = {
    val output = new ByteArrayOutputStream()
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), format, output)
    Base64.getEncoder.encodeToString(output.toByteArray)
  }

  "Cover images" - {
    "accept PNG and JPEG as raw base64 or data URLs and preserve their MIME types" in {
      List(("png", "image/png"), ("jpeg", "image/jpeg")).foreach { case (format, mime) =>
        val encoded = image(format)
        CoverImage.normalize(encoded) shouldBe Right(encoded)
        CoverImage.normalize(s"data:$mime;base64,$encoded") shouldBe Right(encoded)
        CoverImage.dataUrl(encoded) shouldBe s"data:$mime;base64,$encoded"
      }
    }

    "allow exactly 2 MB of decoded image data and reject one byte more" in {
      val signature = Base64.getDecoder.decode(image("png"))
      val atLimit = java.util.Arrays.copyOf(signature, CoverImage.maxBytes)
      val overLimit = java.util.Arrays.copyOf(signature, CoverImage.maxBytes + 1)
      val encoded = Base64.getEncoder.encodeToString(atLimit)
      CoverImage.normalize(encoded) shouldBe Right(encoded)
      summon[Validator[com.toloka.cho.domain.book.BookInfo]]
        .validate(AwesomeBook.bookInfo.copy(image = Some(encoded))).isValid shouldBe true
      CoverImage.normalize(Base64.getEncoder.encodeToString(overLimit)) shouldBe Left(CoverImage.sizeError)
      CoverImage.normalize("A" * (CoverImage.maxBase64Length + 1)) shouldBe Left(CoverImage.sizeError)
    }

    "reject invalid base64, other file types, empty data URLs and mismatched MIME types" in {
      val png = image("png")
      List("not base64!", "SGVsbG8=", "R0lGODlhAQABAIAAAAUEBA==",
        "data:image/svg+xml;base64,PHN2Zz4=",
        "data:image/png;base64,", s"data:image/jpeg;base64,$png").foreach { value =>
        CoverImage.normalize(value) shouldBe Left(CoverImage.formatError)
      }
    }

    "reject truncated images in shared normalization and backend validation" in {
      val validator = summon[Validator[com.toloka.cho.domain.book.BookInfo]]
      List(("png", 8), ("jpeg", 3)).foreach { case (format, headerLength) =>
        val bytes = Base64.getDecoder.decode(image(format))
        List(bytes.take(headerLength), bytes.take(bytes.length / 2), bytes.dropRight(4)).foreach { truncated =>
          val encoded = Base64.getEncoder.encodeToString(truncated)
          CoverImage.normalize(encoded) shouldBe Left(CoverImage.formatError)
          validator.validate(AwesomeBook.bookInfo.copy(image = Some(encoded))).isInvalid shouldBe true
        }
      }
    }

    "decode PNG and JPEG on the backend, rejecting corrupt image data with intact containers" in {
      val validator = summon[Validator[com.toloka.cho.domain.book.BookInfo]]
      List("png", "jpeg").foreach { format =>
        val encoded = image(format)
        validator.validate(AwesomeBook.bookInfo.copy(image = Some(encoded))).isValid shouldBe true
      }
      val png = Base64.getDecoder.decode(image("png"))
      val dataOffset = png.indices.find(i => i + 4 <= png.length &&
        png.slice(i, i + 4).sameElements("IDAT".getBytes("US-ASCII"))).get + 4
      png(dataOffset) = 0
      png(dataOffset + 1) = 0
      val corruptPng = Base64.getEncoder.encodeToString(png)
      val corruptJpeg = Base64.getEncoder.encodeToString(Array[Byte](-1, -40, -1, 0, -1, -39))
      List(corruptPng, corruptJpeg).foreach { encoded =>
        CoverImage.normalize(encoded) shouldBe Right(encoded)
        validator.validate(AwesomeBook.bookInfo.copy(image = Some(encoded)))
          .swap.toOption.get.toList should contain(InvalidCover(CoverImage.formatError))
      }
    }

    "normalize covers through backend book validation and reject oversized API payloads" in {
      val validator = summon[Validator[com.toloka.cho.domain.book.BookInfo]]
      val png = image("png")
      validator.validate(AwesomeBook.bookInfo.copy(image = Some(s"data:image/png;base64,$png")))
        .toOption.flatMap(_.image) shouldBe Some(png)
      validator.validate(AwesomeBook.bookInfo.copy(image = Some("not an image"))).isInvalid shouldBe true
      validator.validate(AwesomeBook.bookInfo.copy(image = Some("A" * (CoverImage.maxBase64Length + 1))))
        .swap.toOption.get.toList should contain(InvalidCover(CoverImage.sizeError))
    }

    "keep the cover optional" in {
      CoverImage.normalize("  ") shouldBe Right("")
      summon[Validator[com.toloka.cho.domain.book.BookInfo]]
        .validate(AwesomeBook.bookInfo.copy(image = Some(""))).toOption.flatMap(_.image) shouldBe None
    }
  }
}
