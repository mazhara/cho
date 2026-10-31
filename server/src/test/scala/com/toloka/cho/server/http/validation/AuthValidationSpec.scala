package com.toloka.cho.server.http.validation

import com.toloka.cho.domain.AuthValidation
import com.toloka.cho.domain.auth.*
import com.toloka.cho.server.http.validation.validators.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class AuthValidationSpec extends AnyFreeSpec with Matchers {
  "Password policy" - {
    "rejects reusing the current password for a password change" in {
      val result = summon[Validator[NewPasswordInfo]].validate(NewPasswordInfo("Secure123!", "Secure123!"))
      result.swap.toOption.get.toList should contain(PasswordUnchanged)
    }

    "accepts the exact length boundaries and punctuation" in {
      List("Abcdef1!", "Ab1!" + "a" * 60, "A long passphrase 1!").foreach { password =>
        AuthValidation.passwordIsValid(password) shouldBe true
      }
    }

    "rejects missing character classes and out-of-range lengths" in {
      List("", "Abcd1!x", "abcdefgh1!", "ABCDEFGH1!", "Abcdefgh!", "Abcdefgh1",
        "Abcdefg1 ", "Ab1!" + "a" * 61).foreach { password =>
        AuthValidation.passwordIsValid(password) shouldBe false
      }
    }

    "does not apply the new policy to login or the old password" in {
      summon[Validator[LoginInfo]].validate(LoginInfo("reader@example.test", "legacy")).isValid shouldBe true
      summon[Validator[NewPasswordInfo]].validate(NewPasswordInfo("legacy", "Secure123!")).isValid shouldBe true
    }
  }

  "International phone validation" - {
    "accepts valid Ukrainian and French numbers and normalizes formatting" in {
      validatePhone("+380 (50) 123-45-67").toOption shouldBe Some("+380501234567")
      validatePhone(" +33 6 12 34 56 78 ").toOption shouldBe Some("+33612345678")
    }

    "rejects invalid countries, lengths, local numbers, letters, and extensions" in {
      List("", "123", "0612345678", "+11111111111", "+999123456789",
        "+380123456789", "+336123456789", "+0123456789", "+33612345678abc",
        "+33612345678 ext 1", "+33612345678\njunk", "+1234567890123456").foreach { phone =>
        withClue(phone) { validatePhone(phone).isInvalid shouldBe true }
      }
    }

    "preserves the optional phone field for API clients" in {
      val info = NewUserInfo("reader@example.test", "Secure123!", None, None, None,
        dataProcessingConsent = true)
      summon[Validator[NewUserInfo]].validate(info).toOption shouldBe Some(info)
    }

    "returns normalized numbers in the validated registration payload" in {
      val info = NewUserInfo("reader@example.test", "Secure123!", None, None, None,
        phone = Some("+33 (6) 12-34-56-78"), dataProcessingConsent = true)
      summon[Validator[NewUserInfo]].validate(info).toOption shouldBe
        Some(info.copy(phone = Some("+33612345678")))
    }
  }
}
