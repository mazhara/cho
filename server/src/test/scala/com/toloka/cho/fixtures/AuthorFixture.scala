package com.toloka.cho.fixtures

import java.util.UUID
import com.toloka.cho.domain.Author
import com.toloka.cho.domain.AuthorInfo
import com.toloka.cho.domain.AuthorType

trait AuthorFixture {

  val existingAuthorUuid: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
  val notFoundAuthorUuid: UUID = UUID.fromString("99999999-9999-9999-9999-999999999999")

 
  val existingAuthor: Author = Author(
    id = existingAuthorUuid,
    AuthorInfo (
        firstName = Some("Jane"),
        lastName =  "Doe",
        authorType = AuthorType.Author)
  )

  val newAuthor: AuthorInfo = AuthorInfo(
    firstName = Some("John"),
    lastName =  "Smith",
    authorType = AuthorType.Editor
  )

  val updatedAuthor: AuthorInfo = 
    AuthorInfo (
        firstName = Some("Jane"),
        lastName = "Doe Updated",
        authorType = AuthorType.Illustrator
    )
  
}
