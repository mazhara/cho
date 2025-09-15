package toloka.cho.books.pages

import cats.effect.IO
import toloka.cho.books.App
import toloka.cho.books.common.AboutPageTranslations
import toloka.cho.books.common.Language.Language
import toloka.cho.books.components.SubHeader
import tyrian.Html.*
import tyrian.{Attribute, Cmd, Html}

case class AboutPage(lang: Language) extends Page {
  import App.Msg

  private implicit val language: Language = lang

  override def subHeader: Option[Html[Msg]] = Some(
    SubHeader.view(
      items = List(
        SubHeader.MenuItem(AboutPageTranslations.get("about.subheader.about"), ""),
        SubHeader.MenuItem(AboutPageTranslations.get("about.subheader.rules"), "")
      ),
      activeItem = AboutPageTranslations.get("about.subheader.about")
    )
  )

  override def view(): Html[Msg] =
    div(cls := "page-content")(
      h2(cls := "page-title")(AboutPageTranslations.get("about.title")),
      hr(cls := "title-hr"),
      div(cls := "video-container")(
        iframe(
          width := "100%",
          height := "500", // Adjust height as needed
          src := "https://www.youtube.com/embed/tsM71MXqRQY",
          title := "YouTube video player",
          Attribute("frameborder", "0"),
          Attribute("allow", "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"),
          Attribute("allowfullscreen", "true")
        )()
      ),
      div(cls := "two-column-section")(
        div(cls := "text-column")(
          p(AboutPageTranslations.get("about.p"))
        ),
        div(cls := "logo-text-column")(
          h2(cls := "misto cho-text-logo")("CHO")
        )
      )
    )

  override def initCmd: Cmd[IO, Msg] = Cmd.None

  override def update(msg: Msg): (Page, Cmd[IO, Msg]) = (this, Cmd.None)
}
