package toloka.cho.books.components

import tyrian.*
import tyrian.Html.*

import toloka.cho.books.App

object Footer {
  def view(): Html[App.Msg] =
    div(`class` := "footer")(
      p(
        text("Written in "),
        a(href := "https://scala-lang.org", target := "blank")("Scala"),
        text(" with <3")
      )
    )
}
