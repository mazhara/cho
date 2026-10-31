package com.toloka.cho.common

import com.toloka.cho.common.Language.{French, Ukrainian}
import com.toloka.cho.common.Language.Language


object AdminPageTranslations extends Translations:
  override protected val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "admin.dashboard"      -> "Панель керування",
      "admin.catalog"        -> "Каталог",
      "admin.books"          -> "Книги",
      "admin.users"          -> "Користувачі",
      "admin.logout"         -> "Вийти",
      "admin.workspace"      -> "Бібліотека Толока · Адміністрування",
      "admin.libraryName"    -> "Бібліотека Толока",
      "admin.user.total"     -> "Усього користувачів",
      "admin.books.total"    -> "Усього примірників",
      "admin.stats.error"    -> "Не вдалося завантажити статистику",
      "admin.overdue"        -> "Боржники",
      "admin.next"           -> "Далі",
      "admin.team"           -> "Команда бібліотеки",
      "admin.borrowed"       -> "Видано",
      "admin.returned"       -> "Повернуто",
      "admin.role.admin"     -> "Адміністратор",
      "admin.role.librarian" -> "Бібліотекар",
      "admin.role.unknown"   -> "Невідома роль",
      "admin.user.unknown"   -> "Невідомий користувач",
      "admin.active"         -> "Активний",
      "admin.inactive"       -> "Неактивний"
    ),
    French -> Map(
      "admin.dashboard"      -> "Tableau de bord",
      "admin.catalog"        -> "Catalogue",
      "admin.books"          -> "Livres",
      "admin.users"          -> "Utilisateurs",
      "admin.logout"         -> "Déconnexion",
      "admin.workspace"      -> "Bibliothèque Toloka · Administration",
      "admin.libraryName"    -> "Bibliothèque Toloka",
      "admin.user.total"     -> "Nombre d’utilisateurs",
      "admin.books.total"    -> "Nombre d’exemplaires",
      "admin.stats.error"    -> "Impossible de charger les statistiques",
      "admin.overdue"        -> "Emprunts en retard",
      "admin.next"           -> "Suivant",
      "admin.team"           -> "Équipe de la bibliothèque",
      "admin.borrowed"       -> "Empruntés",
      "admin.returned"       -> "Retournés",
      "admin.role.admin"     -> "Administrateur",
      "admin.role.librarian" -> "Bibliothécaire",
      "admin.role.unknown"   -> "Rôle inconnu",
      "admin.user.unknown"   -> "Utilisateur inconnu",
      "admin.active"         -> "Actif",
      "admin.inactive"       -> "Inactif"
    )
  )
