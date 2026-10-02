package toloka.cho.books.common

import toloka.cho.books.common.Language._

object EventListPageTranslations {

  private val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "events.title" -> "Наші події",
      "events.loading" -> "Завантаження подій...",
      "events.empty" -> "Найближчих подій поки немає",
      "events.image.placeholder" -> "Ілюстрація події",
      "events.register" -> "Зареєструватися",
      "events.registration.unavailable" -> "Посилання на реєстрацію буде додано пізніше",
      "events.subheader.all" -> "Всі події",
      "events.subheader.online" -> "Онлайн",
      "events.subheader.offline" -> "Офлайн",
      "events.read.more" -> "Читати далі",
      "events.offline.address" -> "Офлайн адреса",
      "events.online" -> "Онлайн",
      "events.offline" -> "Офлайн",
      "events.ukrainian" -> "Українська",
      "events.french" -> "Французька"
    ),
    French -> Map(
      "events.title" -> "Nos événements",
      "events.loading" -> "Chargement des événements...",
      "events.empty" -> "Aucun événement à venir pour le moment",
      "events.image.placeholder" -> "Illustration de l’événement",
      "events.register" -> "S’inscrire",
      "events.registration.unavailable" -> "Le lien d’inscription sera ajouté ultérieurement",
      "events.subheader.all" -> "Tous les événements",
      "events.subheader.online" -> "En ligne",
      "events.subheader.offline" -> "Hors ligne",
      "events.read.more" -> "Lire la suite",
      "events.offline.address" -> "Adresse hors ligne",
      "events.online" -> "En ligne",
      "events.offline" -> "Hors ligne",
      "events.ukrainian" -> "Ukrainien",
      "events.french" -> "Français"
    )
  )

  def get(key: String)(implicit lang: Language): String = {
    translations.get(lang).flatMap(_.get(key)).getOrElse(key)
  }
}
