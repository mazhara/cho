package com.toloka.cho.pages

import cats.effect.IO
import com.toloka.cho.App
import com.toloka.cho.common.Language.Language
import com.toloka.cho.common.NotFoundPageTranslations
import tyrian.Html.*
import tyrian.{Cmd, Html}

case class NotFoundPage(lang: Language) extends Page {

  private implicit val language: Language = lang

  override def initCmd: Cmd[IO, App.Msg] =
    Cmd.None
  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) =
    (this, Cmd.None)
  override def view(): Html[App.Msg] =
    div(`class` := "row")(
      div(`class` := "col-md-7")(
        div(`class` := "form-section")(
          div(`class` := "top-section")(
            h1(span(NotFoundPageTranslations.get("not.found.ouch"))),
            div(NotFoundPageTranslations.get("not.found.text"))
          )
        )
      )
    )
}
