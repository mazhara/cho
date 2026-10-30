package com.toloka.cho.common

object Language {

  sealed trait Language(val code: String, val name: String, val flag: String)
  case object Ukrainian extends Language("uk", "Українська", "🇺🇦")
  case object French extends Language("fr", "Français", "🇫🇷")

  val languages: List[Language] = List(Ukrainian, French)
}
