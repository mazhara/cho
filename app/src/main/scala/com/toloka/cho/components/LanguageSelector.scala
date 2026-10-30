package com.toloka.cho.components

import com.toloka.cho.App
import com.toloka.cho.App.{ChangeLanguage, ToggleLanguageSelector}
import com.toloka.cho.common.Language.Language
import com.toloka.cho.common.Language
import tyrian.Html._
import tyrian._

object LanguageSelector {

  def view(isOpen: Boolean, lang: Language): Html[App.Msg] = {
    div(cls := "language-selector-custom")(
      div(cls := "current-language-custom", onClick(ToggleLanguageSelector))(
        span(lang.flag)
      ),
      if (isOpen) {
        ul(cls := "language-options")(
          Language.languages.map(l =>
            li(onClick(ChangeLanguage(l)))(
              span(s"${l.flag}")
            )
          )
        )
      } else {
        div()
      }
    )
  }
}
