package com.toloka.cho.domain

final case class PublisherInfo(
    name: String,
    country: Option[String],
    address: Option[String],
    website: Option[String],
    contactEmail: Option[String]
)

final case class Publisher(id: Int, info: PublisherInfo)
