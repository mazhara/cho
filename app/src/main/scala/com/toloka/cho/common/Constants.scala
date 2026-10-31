package com.toloka.cho.common

import scala.scalajs.{LinkingInfo, js}
import scala.scalajs.js.annotation.JSImport
import org.scalajs.dom.window

object Constants {
  @js.native
  @JSImport("/static/img/toloka.png", JSImport.Default)
  val tolokaLogo: String = js.native

  @js.native
  @JSImport("/static/img/cho.png", JSImport.Default)
  val choLogo: String = js.native

  val defaultPageSize = 20

  object endpoints {
    val root =
      if (LinkingInfo.developmentMode) "http://localhost:4041"
      else window.location.origin

    val books = s"$root/api/books"
    val events = s"$root/api/events"
    val getFilters = s"$root/api/books/filters"
    val login = s"$root/api/auth/login"
    val signup = s"$root/api/auth/users"
    val changePassword = s"$root/api/auth/users/password"
    val forgotPassword = s"$root/api/auth/reset"
    val recoverPassword = s"$root/api/auth/recover"
    val logout = s"$root/api/auth/logout"
    val adminStats = s"$root/api/admin/stats"
  }
}
