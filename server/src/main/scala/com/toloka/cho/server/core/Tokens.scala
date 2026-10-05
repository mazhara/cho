package com.toloka.cho.server.core

import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import doobie.util.transactor.Transactor

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import doobie.util.fragment.*
import java.security.SecureRandom

import com.toloka.cho.server.config.TokenConfig

trait Tokens[F[_]] {
  def getToken(email: String): F[Option[String]]
  def checkToken(email: String, token: String): F[Boolean]
  def invalidateToken(email: String): F[Unit]
}

class LiveTokens[F[_]: MonadCancelThrow: Sync: Logger](users: Users[F])(
    xa: Transactor[F],
    tokenConfig: TokenConfig
) extends Tokens[F] {
  override def getToken(email: String): F[Option[String]] =
   
    users.find(email).flatMap {
      case None    => None.pure[F]
      case Some(_) => 
        getFreshToken(email).map(Some(_))
    }

  override def checkToken(email: String, token: String): F[Boolean] =
    sql"""
        SELECT token
        FROM recoverytokens
        WHERE email=$email AND token=$token AND expiration > ${System.currentTimeMillis()}
    """
      .query[String]
      .option
      .transact(xa)
      .map(_.nonEmpty)

  override def invalidateToken(email: String): F[Unit] =
    sql"DELETE FROM recoverytokens WHERE email=$email".update.run.transact(xa).void

  val tokenDuration = tokenConfig.tokenDuration

  private val secureRandom = new SecureRandom()
  private val tokenAlphabet = (('A' to 'Z') ++ ('0' to '9')).toVector

  def randomToken(length: Int): F[String] =
    Sync[F].delay(Vector.fill(length)(tokenAlphabet(secureRandom.nextInt(tokenAlphabet.size))).mkString)

  def getFreshToken(email: String): F[String] =
    findToken(email).flatMap {
      case None    => generateToken(email)
      case Some(_) => updateToken(email)
    }

  def findToken(email: String): F[Option[String]] =
    sql"SELECT token FROM recoverytokens WHERE email=$email"
      .query[String]
      .option
      .transact(xa)

  def generateToken(email: String): F[String] =
    for {
      token <- randomToken(12)
      _ <- sql"""INSERT INTO recoverytokens (email, token, expiration) 
        VALUES ($email, $token, ${System.currentTimeMillis() + tokenDuration})
        """.update.run.transact(xa)
    } yield token

  def updateToken(email: String): F[String] =
    for {
      token <- randomToken(12)
      _ <- sql"""UPDATE recoverytokens
                 SET token=$token, expiration=${System.currentTimeMillis() + tokenDuration}
                 WHERE email=$email
        """.update.run.transact(xa)
    } yield token
}

object LiveTokens {
  def apply[F[_]: MonadCancelThrow: Sync: Logger](
      users: Users[F]
  )(xa: Transactor[F], tokenConfig: TokenConfig): F[LiveTokens[F]] =
    new LiveTokens[F](users)(xa, tokenConfig).pure[F]
}
