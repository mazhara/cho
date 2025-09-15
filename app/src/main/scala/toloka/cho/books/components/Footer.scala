package toloka.cho.books.components

import toloka.cho.books.App
import toloka.cho.books.common.Language.Language
import toloka.cho.books.common.{Constants, FooterTranslations}
import tyrian.Html.{`class`, *}
import tyrian._

object Footer {

  def view(lang: Language): Html[App.Msg] = {
    implicit val language: Language = lang

    footer(`class` := "footer")(
      div(`class` := "w-full flex items-center justify-between px-6 py-2")(
        // LEFT: Follow us + icons
        div(`class` := "flex flex-col items-start")(
          h2(`class` := "mb-2 text-md calibry text-violet-gray")(FooterTranslations.get("footer.follow")),
          div(`class` := "flex space-x-2")(
            a(href := "https://www.instagram.com/chobiblioteque/", `class` := "icon-instagram")(),
            a(href := "https://www.youtube.com/@ShoToloka", `class` := "icon-youtube")()
          )
        ),
        // CENTER: CHO text/logo stays centered
        div(`class` := "text-center")(
          h2(`class` := "text-lg misto text-violet-gray uppercase")("CHO")
        ),
        // RIGHT: Toloka logo
        div(`class` := "flex justify-end")(
          a(href := "https://toloka.fr/")(
            img(src := Constants.tolokaLogo, `class` := "footer-logo", alt := "Toloka Logo")
          )
        )
      )
    )
  }
}
