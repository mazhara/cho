package toloka.cho.books.common

import toloka.cho.books.common.Language._

object NotFoundPageTranslations {

  private val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "not.found.ouch" -> "Ой!",
      "not.found.text" -> "Ця сторінка не існує."
    ),
    French -> Map(
      "not.found.ouch" -> "Aïe!",
      "not.found.text" -> "Cette page n'existe pas."
    )
  )

  def get(key: String)(implicit lang: Language): String = {
    translations.get(lang).flatMap(_.get(key)).getOrElse(key)
  }
}
