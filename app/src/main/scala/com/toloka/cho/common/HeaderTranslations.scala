package com.toloka.cho.common

import com.toloka.cho.common.Language._

object HeaderTranslations extends Translations {

  override protected val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "header.books" -> "Книги",
      "header.events" -> "Події",
      "header.about" -> "Про нас",
      "header.admin" -> "Адміністрування",
      "header.login" -> "Увійти",
      "header.signup" -> "Зареєструватися",
      "header.logout" -> "Вийти",
      "header.search" -> "Пошук книги або автора"
    ),
    French -> Map(
      "header.books" -> "Livres",
      "header.events" -> "Événements",
      "header.about" -> "À propos de nous",
      "header.admin" -> "Administration",
      "header.login" -> "Connexion",
      "header.signup" -> "Inscription",
      "header.logout" -> "Déconnexion",
      "header.search" -> "Rechercher un livre ou un auteur"
    )
  )
}
