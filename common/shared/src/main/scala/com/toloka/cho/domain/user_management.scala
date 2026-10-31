package com.toloka.cho.domain

final case class ManagedUser(
    id: Long,
    name: String,
    email: String,
    username: String,
    role: String,
    firstName: Option[String],
    lastName: Option[String],
    phone: Option[String],
    company: Option[String]
)

final case class ManagedUserForm(
    email: String,
    username: String,
    firstName: Option[String],
    lastName: Option[String],
    phone: Option[String],
    company: Option[String],
    role: String,
    password: Option[String]
)

object ManagedUserForm {
  def usernameIsValid(username: String): Boolean =
    username.matches("^[a-zA-Z0-9._-]{3,32}$")
}
