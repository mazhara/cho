package toloka.cho.books.common

import toloka.cho.books.common.Language._

object AboutPageTranslations {

  private val translations: Map[Language, Map[String, String]] = Map(
    Ukrainian -> Map(
      "about.title" -> "Шокайте на здоровля",
      "about.subheader.about" -> "Про нас",
      "about.subheader.rules" -> "Правила",
      "about.p" -> "Широко відома як остаточна збірка американської писемності, видання Бібліотеки Америки охоплюють усі періоди та жанри, включаючи визнані класики, знехтувані шедеври та історично важливі документи та тексти, і демонструють життєздатність та різноманітність літературної спадщини Америки. Додаткові громадські програми, цифрові ресурси та партнерства з громадами допомагають читачам у всьому світі встановлювати значущі зв’язки з письмовою спадщиною нації."
    ),
    French -> Map(
      "about.title" -> "Bonne lecture !",
      "about.subheader.about" -> "À propos de nous",
      "about.subheader.rules" -> "Règles",
      "about.p" -> "Largement reconnue comme la collection définitive d'écrits américains, les éditions de la Library of America couvrent toutes les périodes et tous les genres, y compris les classiques reconnus, les chefs-d'œuvre négligés et les documents et textes historiquement importants, et mettent en valeur la vitalité et la variété de l'héritage littéraire américain. Des programmes publics supplémentaires, des ressources numériques et des partenariats communautaires aident les lecteurs du monde entier à établir des liens significatifs avec le patrimoine écrit de la nation."
    )
  )

  def get(key: String)(implicit lang: Language): String = {
    translations.get(lang).flatMap(_.get(key)).getOrElse(key)
  }
}
