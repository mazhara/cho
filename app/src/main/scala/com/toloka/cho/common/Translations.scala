package com.toloka.cho.common

import com.toloka.cho.common.Language.Language

/** Shared lookup behaviour for the per-page translation maps (one map per supported
  * Language, falling back to the key itself when a translation is missing).
  */
trait Translations {
  protected def translations: Map[Language, Map[String, String]]

  def get(key: String)(implicit lang: Language): String =
    translations.get(lang).flatMap(_.get(key)).getOrElse(key)
}
