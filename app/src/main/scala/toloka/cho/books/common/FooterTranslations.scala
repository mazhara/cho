package toloka.cho.books.common

import toloka.cho.books.common.Language._

object FooterTranslations {

  private val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "footer.follow" -> "Підписуйтесь на нас"
    ),
    French -> Map(
      "footer.follow" -> "Suivez-nous"
    )
  )

  def get(key: String)(implicit lang: Language): String = {
    translations.get(lang).flatMap(_.get(key)).getOrElse(key)
  }
}
