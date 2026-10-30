package com.toloka.cho.common

import com.toloka.cho.common.Language._

object NotFoundPageTranslations extends Translations {

  override protected val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "not.found.ouch" -> "Ой!",
      "not.found.text" -> "Ця сторінка не існує."
    ),
    French -> Map(
      "not.found.ouch" -> "Aïe!",
      "not.found.text" -> "Cette page n'existe pas."
    )
  )
}
