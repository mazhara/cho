package toloka.cho.books.common

import toloka.cho.books.common.Language._

object HeaderTranslations {

  private val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "header.books" -> "Книги",
      "header.events" -> "Події",
      "header.about" -> "Про нас",
      "header.search" -> "Пошук"
    ),
    French -> Map(
      "header.books" -> "Livres",
      "header.events" -> "Événements",
      "header.about" -> "À propos de nous",
      "header.search" -> "Rechercher"
    )
  )

  def get(key: String)(implicit lang: Language): String = {
    translations.get(lang).flatMap(_.get(key)).getOrElse(key)
  }
}
