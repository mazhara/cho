package toloka.cho.books.pages

import cats.effect.IO
import toloka.cho.books.App
import toloka.cho.books.components.SubHeader
import tyrian.Html.*
import tyrian.{Attribute, Cmd, Html}

case class AboutPage() extends Page {
  import App.Msg

  override def subHeader: Option[Html[Msg]] = Some(
    SubHeader.view(
      items = List(
        SubHeader.MenuItem("Про нас", "#"),
        SubHeader.MenuItem("Правила", "#")
      ),
      activeItem = "Про нас"
    )
  )

  override def view(): Html[Msg] =
    div(cls := "page-content")(
      h2(cls := "page-title")("Шокайте на здоровля"),
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
          p("Widely recognized as the definitive collection of American writing, Library of America editions encompass all periods and genres—including acknowledged classics, neglected masterpieces, and historically important documents and texts—and showcase the vitality and variety of America’s literary legacy. Additional public programs, digital resources, and community partnerships help readers worldwide make meaningful connections with the nation’s written heritage.")
        ),
        div(cls := "logo-text-column")(
          h2(cls := "misto cho-text-logo")("CHO")
        )
      )
    )

  override def initCmd: Cmd[IO, Msg] = Cmd.None

  override def update(msg: Msg): (Page, Cmd[IO, Msg]) = ???
}