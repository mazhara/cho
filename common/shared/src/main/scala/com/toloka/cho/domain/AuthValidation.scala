package com.toloka.cho.domain

object AuthValidation {
  val passwordMinLength = 8
  val passwordMaxLength = 64

  def passwordIsValid(password: String): Boolean =
    password.length >= passwordMinLength &&
      password.length <= passwordMaxLength &&
      password.exists(c => c >= 'A' && c <= 'Z') &&
      password.exists(c => c >= 'a' && c <= 'z') &&
      password.exists(c => c >= '0' && c <= '9') &&
      password.exists(c => c >= '!' && c <= '~' && !c.isLetterOrDigit)

  def normalizePhone(phone: String): String =
    phone.trim.replaceAll("[ ()-]", "")

  def phoneFormatIsValid(phone: String): Boolean =
    phone.trim.matches("""\+[1-9][0-9 ()-]*""") &&
      phone.trim.length <= 40 &&
      normalizePhone(phone).matches("""\+[1-9][0-9]{7,14}""")
}
