package com.toloka.cho.server.core

import cats.*
import cats.implicits.*
import cats.effect.*
import org.typelevel.log4cats.Logger

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import doobie.util.fragment.*
import com.toloka.cho.domain.user.User
import com.toloka.cho.domain.{ManagedUser, ManagedUserForm}

trait Users[F[_]] {
  def count(): F[Long]
  def find(email: String): F[Option[User]]
  def create(user: User): F[String]
  def update(user: User): F[Option[User]]
  def delete(email: String): F[Boolean]
  def allManaged(search: String, limit: Int, skip: Int): F[List[ManagedUser]]
  def createManaged(form: ManagedUserForm, hashedPassword: String): F[Option[ManagedUser]]
  def updateManaged(form: ManagedUserForm): F[Option[ManagedUser]]
}

final class LiveUsers[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Users[F] {
  import LiveUsers.managedUserRead

  override def count(): F[Long] =
    sql"SELECT COUNT(*) FROM users"
      .query[Long]
      .unique
      .transact(xa)

  override def find(email: String): F[Option[User]] =
    sql"""
      SELECT email, hashedPassword, firstName, lastName, company, role,
             phone, data_processing_consent_at
      FROM users WHERE email = $email
    """
      .query[User]
      .option
      .transact(xa)

  override def create(user: User): F[String] =
    sql"""
        INSERT INTO users(
            email,
            hashedPassword, 
            firstName,
            lastName,
            company,
            role,
            phone,
            data_processing_consent_at,
            username
        ) VALUES (
            ${user.email},
            ${user.hashedPassword},
            ${user.firstName},
            ${user.lastName},
            ${user.company},
            ${user.role},
            ${user.phone},
            ${user.dataProcessingConsentAt},
            CASE
              WHEN EXISTS (SELECT 1 FROM users WHERE LOWER(username) = LOWER(SPLIT_PART(${user.email}, '@', 1)))
              THEN LEFT(SPLIT_PART(${user.email}, '@', 1), 23) || '_' || SUBSTRING(MD5(${user.email}), 1, 8)
              ELSE SPLIT_PART(${user.email}, '@', 1)
            END
        )
      """
      .update
      .run
      .transact(xa)
      .map(_ => user.email)

  override def update(user: User): F[Option[User]] =
    for {
      _ <- sql"""
            UPDATE users SET
                hashedPassword = ${user.hashedPassword},
                firstName = ${user.firstName},
                lastName = ${user.lastName},
                company = ${user.company},
                role = ${user.role},
                phone = ${user.phone}
            WHERE email = ${user.email}
            """.update.run.transact(xa)
      maybeUser <- find(user.email)
    } yield maybeUser

  override def delete(email: String): F[Boolean]   =
    sql"DELETE FROM users WHERE email = ${email}".update.run.transact(xa).map(_ > 0)

  override def allManaged(search: String, limit: Int, skip: Int): F[List[ManagedUser]] =
    val escapedSearch = search.trim.toLowerCase.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    val pattern = s"%$escapedSearch%"
    sql"""
      SELECT user_id, COALESCE(firstName, '') || ' ' || COALESCE(lastName, ''),
             email, username, role, firstName, lastName, phone, company
      FROM users
      WHERE $search = ''
         OR LOWER(COALESCE(firstName, '') || ' ' || COALESCE(lastName, '')) LIKE $pattern ESCAPE E'\\'
         OR LOWER(email) LIKE $pattern ESCAPE E'\\'
         OR LOWER(username) LIKE $pattern ESCAPE E'\\'
         OR CAST(user_id AS TEXT) LIKE $pattern ESCAPE E'\\'
      ORDER BY user_id
      LIMIT $limit OFFSET $skip
    """.query[ManagedUser].to[List].transact(xa)

  override def createManaged(form: ManagedUserForm, hashedPassword: String): F[Option[ManagedUser]] =
    sql"""
      INSERT INTO users (
        email, username, hashedPassword, firstName, lastName, company, phone, role,
        data_processing_consent_at
      )
      VALUES (
        ${form.email}, ${form.username}, $hashedPassword, ${form.firstName}, ${form.lastName},
        ${form.company}, ${form.phone}, ${form.role}, NULL
      )
      ON CONFLICT DO NOTHING
      RETURNING user_id, COALESCE(firstName, '') || ' ' || COALESCE(lastName, ''),
                email, username, role, firstName, lastName, phone, company
    """.query[ManagedUser].option.transact(xa)

  override def updateManaged(form: ManagedUserForm): F[Option[ManagedUser]] =
    sql"""
      UPDATE users
      SET username = ${form.username},
          firstName = ${form.firstName},
          lastName = ${form.lastName},
          company = ${form.company},
          phone = ${form.phone},
          role = ${form.role},
          hashedPassword = COALESCE(${form.password}, hashedPassword)
      WHERE email = ${form.email}
      RETURNING user_id, COALESCE(firstName, '') || ' ' || COALESCE(lastName, ''),
                email, username, role, firstName, lastName, phone, company
    """.query[ManagedUser].option.transact(xa)
}

object LiveUsers {
  given managedUserRead: Read[ManagedUser] =
    Read[(Long, String, String, String, String, Option[String], Option[String], Option[String], Option[String])]
      .map { case (id, name, email, username, role, firstName, lastName, phone, company) =>
        ManagedUser(id, name.trim, email, username, role, firstName, lastName, phone, company)
      }

  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveUsers[F]] =
    new LiveUsers[F](xa).pure[F]
}