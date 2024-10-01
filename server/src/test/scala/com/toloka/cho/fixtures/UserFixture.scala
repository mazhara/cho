package com.toloka.cho.fixtures

import cats.effect.IO
import com.toloka.cho.admin.core.Users
import com.toloka.cho.domain.user.User
import com.toloka.cho.domain.user.Role
import com.toloka.cho.domain.auth.NewUserInfo

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
    "$2a$10$0l3cq8mOClq3ppBzkVLr0OdssC0BOv0rJrwqcf0JxAeydvHeT1Xhi",
    Some("Dawid"),
    Some("Hungry"),
    Some("DL corp."),
    Role.ADMIN
  )
  val remiEmail    = Remi.email
  val remiPassword = "remimypassword"

  val Gaston = User(
    "john@lakomy.github.io",
    "$2a$10$3AN4sSQmbWXlkog6OIJjuesZ0cbi9uWd34j9Lx22Izv9faYD.H6qy",
    Some("John"),
    Some("Hungrytoo"),
    Some("DL corp."),
    Role.LIBRARIAN
  )
  val gastonEmail    = Gaston.email
  val gastonPassword = "gastonmypassword"

  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$bZ8qBo60FGd0eSEMLzmpCuhSc3xIkh/wEorpEfOWqkkGT.svAB0.G",
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.LIBRARIAN
  )

  val UpdatedGaston = User(
    "john@lakomy.github.io",
    "$2a$10$P3TUBvKE5miIj/qrHDMqEeoxwIESTs7HCDhYjppbikDaNqp3HXdWu",
    Some("GASTON"),
    Some("EL GATO"),
    Some("Adobe"),
    Role.LIBRARIAN
  )

  val NewUserRemi = NewUserInfo(
    remiEmail,
    remiPassword,
    Some("Remi"),
    Some("Cornet"),
    Some("Corem Corp")
  )

  val NewUserGaston = NewUserInfo(
    gastonEmail,
    gastonPassword,
    Some("Gaston"),
    Some("El Gato"),
    Some("Corem Corp")
  )
}
