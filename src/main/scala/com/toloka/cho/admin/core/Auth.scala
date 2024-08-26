package com.toloka.cho.admin.core

import cats.*
import cats.implicits.*
import cats.effect.*
import org.typelevel.log4cats.Logger

import tsec.authentication.AugmentedJWT
import tsec.mac.jca.HMACSHA256
import com.toloka.cho.admin.domain.user.NewUserInfo
import com.toloka.cho.admin.domain.security.JwtToken
import com.toloka.cho.admin.domain.user.User
import com.toloka.cho.admin.domain.auth.NewPasswordInfo
import com.toloka.cho.admin.domain.security.Authenticator


trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]]
}

class LiveAuth[F[_]: MonadCancelThrow: Logger] private (
    users: Users[F],
    authenticator: Authenticator[F]
) extends Auth[F] {
  override def login(email: String, password: String): F[Option[JwtToken]] = ???

  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] = ???

  override def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]] = ???

}

object LiveAuth {
  def apply[F[_]: MonadCancelThrow: Logger](
      users: Users[F],
      authenticator: Authenticator[F]
  ): F[LiveAuth[F]] =
    new LiveAuth[F](users, authenticator).pure[F]
}