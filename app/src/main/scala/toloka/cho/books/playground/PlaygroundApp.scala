package toloka.cho.books.playground

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.concurrent.duration.*
import org.scalajs.dom.{document, console}
import scala.scalajs.js.annotation.JSExport
import cats.effect.IO

import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger

import toloka.cho.books.playground.PlaygroundApp.Model
import toloka.cho.books.playground.PlaygroundApp.Msg
import toloka.cho.books.playground.PlaygroundApp.Increment

object PlaygroundApp {
  trait Msg
  case class Increment(amount: Int) extends Msg

  case class Model(count: Int)
}

// @JSExportTopLevel("TolokaApp")

// ^^message, ^^model = "state"
class PlaygroundApp extends TyrianApp[PlaygroundApp.Msg, PlaygroundApp.Model] {

    /* we can send message by 
    -trigger command (Cmd.Emit)
    - create a subscription (ex: Sub.every[IO](1.second))
    - listening for an event (ex: Button onClick)
   */

  // 
    //potentialy endless stream of messages
  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(0), Cmd.None)

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None
    // Sub.every[IO](1.second).map(_ => Increment(1))


      // model can change by recieving messages
  // model => message => (new model, ____)
  // update triggered whenever we get a new message
  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Increment(amount) =>
      (model.copy(count = model.count + amount), Logger.consoleLog[IO](s"Changing count by $amount"))
  }
 //view triggered whenever model changed render html with model
  override def view(model: Model): Html[Msg] =
    div(
      button(onClick(Increment(1)))("Increase !"),
      button(onClick(Increment(-1)))("Decrease !"),
      div(s"Tyrian running: ${model.count}")
    )
}