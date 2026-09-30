package com.toloka.cho.admin.core

import cats.*
import cats.effect.*
import cats.implicits.*
import com.toloka.cho.domain.event.Event
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor

import java.util.UUID

trait Events[F[_]] {
  def all(): F[List[Event]]
}

final class LiveEvents[F[_]: MonadCancelThrow] private (xa: Transactor[F]) extends Events[F] {
  import LiveEvents.eventRead

  override def all(): F[List[Event]] =
    sql"""
      SELECT
        event_id,
        title,
        to_char(event_date AT TIME ZONE 'Europe/Paris', 'YYYY-MM-DD HH24:MI'),
        location,
        afishe_url,
        is_online,
        is_offline,
        language,
        description,
        offline_address,
        registration_url
      FROM events
      ORDER BY event_date ASC, event_id
    """.query[Event].to[List].transact(xa)
}

object LiveEvents {
  given eventRead: Read[Event] =
    Read[
      (
        UUID,
        String,
        String,
        String,
        Option[String],
        Boolean,
        Boolean,
        String,
        String,
        Option[String],
        Option[String]
      )
    ].map { case (id, title, date, location, afisheUrl, isOnline, isOffline, language, description, offlineAddress, registrationUrl) =>
      Event(
        id,
        title,
        date,
        location,
        afisheUrl,
        isOnline,
        language,
        description,
        offlineAddress,
        registrationUrl,
        isOffline
      )
    }

  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LiveEvents[F]] =
    new LiveEvents[F](xa).pure
}
