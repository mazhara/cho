package com.toloka.cho.common

import com.toloka.cho.common.Language._

object FooterTranslations extends Translations {

  override protected val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "footer.follow" -> "Підписуйтесь на нас"
    ),
    French -> Map(
      "footer.follow" -> "Suivez-nous"
    )
  )
}
