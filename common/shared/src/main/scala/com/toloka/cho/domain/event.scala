package com.toloka.cho.domain

import java.util.UUID

object event {
  case class Event(
    id: UUID,
    title: String,
    date: String,
    location: String,
    afisheUrl: Option[String],
    isOnline: Boolean,
    language: String,
    description: String,
    offlineAddress: Option[String] // New field
  )
}
