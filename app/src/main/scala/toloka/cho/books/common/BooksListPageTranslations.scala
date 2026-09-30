package toloka.cho.books.common

import toloka.cho.books.common.Language._

object BooksListPageTranslations {

  private val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "books.title" -> "шокайте на здоровля",
      "books.subheader.all" -> "Усі книги",
      "books.subheader.category" -> "Категорія",
      "books.subheader.new" -> "Надходження",
      "books.subheader.author" -> "Автор",
      "books.subheader.name" -> "Назва",
      "books.sort" -> "Сортувати за",
      "books.search.noResults" -> "За вашим запитом нічого не знайдено",
      "books.load.more" -> "Завантажити більше",
      "books.all.loaded" -> "Всі книги завантажено",
      "books.taken" -> "Читають"
    ),
    French -> Map(
      "books.title" -> "Bonne lecture !",
      "books.subheader.all" -> "Tous les livres",
      "books.subheader.category" -> "Catégorie",
      "books.subheader.new" -> "Nouveautés",
      "books.subheader.author" -> "Auteur",
      "books.subheader.name" -> "Nom",
      "books.sort" -> "Trier par",
      "books.search.noResults" -> "Aucun livre trouvé pour cette recherche",
      "books.load.more" -> "Charger plus",
      "books.all.loaded" -> "Tous les livres sont chargés",
      "books.taken" -> "Emprunté"
    )
  )

  def get(key: String)(implicit lang: Language): String = {
    translations.get(lang).flatMap(_.get(key)).getOrElse(key)
  }
}
