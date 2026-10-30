package com.toloka.cho.domain

import doobie.util.meta.Meta
import tsec.authorization.SimpleAuthEnum
import tsec.authorization.AuthGroup

import java.time.OffsetDateTime

object user {
  final case class User(
    email: String, 
    hashedPassword: String, 
    firstName: Option[String],
    lastName: Option[String],
    company: Option[String],
    role: Role,
    phone: Option[String] = None,
    dataProcessingConsentAt: Option[OffsetDateTime] = None
  ){
    def isAdmin: Boolean = role == Role.ADMIN
    def isLibrarian: Boolean = role == Role.LIBRARIAN
    def isReader: Boolean = role == Role.READER
  }

  enum Role {
    case ADMIN, LIBRARIAN, READER
  }

  object Role {
    given metaRole: Meta[Role] =
      Meta[String].timap[Role](Role.valueOf)(_.toString)
  }

    given roleAuthEnum: SimpleAuthEnum[Role, String] with {
      override val values: AuthGroup[Role] = AuthGroup(Role.ADMIN, Role.LIBRARIAN, Role.READER)
      override def getRepr(role: Role): String = role.toString
  }
}