package com.toloka.cho.admin.palyground

import java.util.Properties
import javax.mail.PasswordAuthentication
import javax.mail.Authenticator
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.Message
import javax.mail.Transport
import cats.effect.IOApp
import cats.effect.IO
import com.toloka.cho.admin.core.LiveEmails
import com.toloka.cho.admin.config.EmailServiceConfig

object EmailsPlayground {
  def main(args: Array[String]): Unit = {
    val host        = "smtp.ethereal.email"
    val port        = 587
    val user        = "brown.schamberger@ethereal.email"
    val password    = "wk2nuZXx3FaTN7euJH"
    val token       = "ABCD1234"
    val frontendUrl = "http://google.com"

    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", true)
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.ssl.trust", host)

    val auth = new Authenticator {
      override protected def getPasswordAuthentication(): PasswordAuthentication =
        new PasswordAuthentication(user, password)
    }

    val session = Session.getInstance(prop, auth)

    val subject = "Email from Toloka"
    val content = s"""
    <div style="
        border: 1px solid black;
        padding: 20px;
        font-family: sans-serif;
        line-height: 2;
        font-size: 20px;
    ">
    <h1>Email from Toloka</h1>
    <p>Your password recovery token is: $token</p>
    <p>
        Click <a href="$frontendUrl/login">here</a> to get back to the application
    </p>
    </div>
    """

    val message = new MimeMessage(session)
    message.setFrom("corem@corem.corp")
    message.setRecipients(Message.RecipientType.TO, "the.user@gmail.com")
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")

    Transport.send(message)
  }
}

object EmailsEffectPlayground extends IOApp.Simple {
  override def run: IO[Unit] = for {
    emails <- LiveEmails[IO](
      EmailServiceConfig(
        host = "smtp.ethereal.email",
        port = 587,
        user = "brown.schamberger@ethereal.email",
        pass = "wk2nuZXx3FaTN7euJH",
        frontendUrl = "http://google.com"
      )
    )
    _ <- emails.sendPasswordRecoveryEmail("someone@corem.corp", "Token123")
  } yield ()
}
