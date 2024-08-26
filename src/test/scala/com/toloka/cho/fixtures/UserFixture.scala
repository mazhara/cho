package com.toloka.cho.fixtures

import cats.effect.IO
import com.toloka.cho.admin.domain.user.User
import com.toloka.cho.admin.domain.user.Role
import com.toloka.cho.admin.core.Users

trait UserFixture {

  val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] =
      if (email == remiEmail) IO.pure(Some(Remi))
      else IO.pure(None)
    override def create(user: User): IO[String]       = IO.pure(user.email)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean]   = IO.pure(true)
  }

  val Remi = User(
    "dawid@dlakomy.github.io",
    "$2a$10$k7SC5Wz54II9QMrB7.FhEeYWApxNQH28tWGKcbtbkXTDE02yYq2Ba",
    Some("Dawid"),
    Some("Hungry"),
    Some("DL corp."),
    Role.ADMIN
  )
  val remiEmail    = Remi.email
  val remiPassword = "hashedpassword"

  val Gaston = User(
    "john@lakomy.github.io",
    "$2a$10$yuC4.08NGHHkgAfuSE0ORee1uBQMqn5W5F5srhvWZMy9TnQH39kZS",
    Some("John"),
    Some("Hungrytoo"),
    Some("DL corp."),
    Role.LIBRARIAN
  )
  val gastonEmail    = Gaston.email
  val gastonPassword = "hashedpassword"

  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$6LQt4xy4LzqQihZiRZGG0eeeDwDCvyvthICXzPKQDQA3C47LtrQFy",
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.LIBRARIAN
  )

  val UpdatedGaston = User(
    "john@lakomy.github.io",
    "$2a$10$yuC4.08NGHHkgAfuSE0ORee1uBQMqn5W5F5srhvWZMy9TnQH39kZS",
    Some("GASTON"),
    Some("EL GATO"),
    Some("Adobe"),
    Role.LIBRARIAN
  )
}
