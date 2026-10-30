package toloka.cho.books.core

import cats.effect.IO
import toloka.cho.books.App
import toloka.cho.books.common.{Constants, Endpoint}
import toloka.cho.books.pages.{AuthSession, Page}
import tyrian.*
import tyrian.http.*

// The auth token lives in AuthSession (sessionStorage); this object only models session messages.
object Session {
  trait Msg                 extends App.Msg
  case object Logout        extends Msg
  case object LogoutSuccess extends Msg
  case object LogoutFailure extends Msg

  def isActive: Boolean = getUserToken().nonEmpty

  def getUserToken(): Option[String] = AuthSession.authorization

  object Endpoints {
    val logout = new Endpoint[Msg] {
      val location                    = Constants.endpoints.logout
      val method                      = Method.Post
      val onResponse: Response => Msg = _ => LogoutSuccess
      val onError: HttpError => Msg   = _ => LogoutFailure
    }
  }

  // Tell the server, then always drop the local session (even if the call fails).
  def logout: Cmd[IO, App.Msg] =
    if isActive then Endpoints.logout.callAuthorized() else clearLocalSession

  def clearLocalSession: Cmd[IO, App.Msg] =
    Cmd.Run[IO, Unit, App.Msg](AuthSession.clear)(_ => Router.ChangeLocation(Page.Urls.HOME))
}
