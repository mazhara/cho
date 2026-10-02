package toloka.cho.books.common

import toloka.cho.books.common.Language.{French, Ukrainian}
import toloka.cho.books.common.Language.Language

object AdminPageTranslations {
  private val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "admin.dashboard" -> "Панель керування",
      "admin.catalog" -> "Каталог",
      "admin.books" -> "Книги",
      "admin.users" -> "Користувачі",
      "admin.logout" -> "Вийти",
      "admin.workspace" -> "Бібліотека Толока · Адміністрування",
      "admin.libraryName" -> "Бібліотека Толока",
      "admin.user.total" -> "Усього користувачів",
      "admin.books.total" -> "Усього примірників",
      "admin.overdue" -> "Боржники",
      "admin.next" -> "Далі",
      "admin.team" -> "Команда бібліотеки",
      "admin.borrowed" -> "Видано",
      "admin.returned" -> "Повернуто",
      "admin.role.admin" -> "Адміністратор",
      "admin.role.librarian" -> "Бібліотекар",
      "admin.active" -> "Активний",
      "admin.inactive" -> "Неактивний"
    ),
    French -> Map(
      "admin.dashboard" -> "Tableau de bord",
      "admin.catalog" -> "Catalogue",
      "admin.books" -> "Livres",
      "admin.users" -> "Utilisateurs",
      "admin.logout" -> "Déconnexion",
      "admin.workspace" -> "Bibliothèque Toloka · Administration",
      "admin.libraryName" -> "Bibliothèque Toloka",
      "admin.user.total" -> "Nombre d’utilisateurs",
      "admin.books.total" -> "Nombre d’exemplaires",
      "admin.overdue" -> "Emprunts en retard",
      "admin.next" -> "Suivant",
      "admin.team" -> "Équipe de la bibliothèque",
      "admin.borrowed" -> "Empruntés",
      "admin.returned" -> "Retournés",
      "admin.role.admin" -> "Administrateur",
      "admin.role.librarian" -> "Bibliothécaire",
      "admin.active" -> "Actif",
      "admin.inactive" -> "Inactif"
    )
  )

  def get(key: String)(implicit lang: Language): String =
    translations.get(lang).flatMap(_.get(key)).getOrElse(key)
}
