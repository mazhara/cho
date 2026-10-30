package com.toloka.cho.server.core

import cats.effect.*
import cats.implicits.*
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.Message
import javax.mail.Transport

import com.toloka.cho.server.config.EmailServiceConfig

trait Emails[F[_]] {
  def sendEmail(to: String, subject: String, content: String): F[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): F[Unit]
}

class LiveEmails[F[_]: Sync] private (emailServiceConfig: EmailServiceConfig)
    extends Emails[F] {

  val host        = emailServiceConfig.host
  val port        = emailServiceConfig.port
  val user        = emailServiceConfig.user
  val pass        = emailServiceConfig.pass
  val frontendUrl = emailServiceConfig.frontendUrl

  def sendEmail(to: String, subject: String, content: String): F[Unit] = Sync[F].blocking {
    val session = Session.getInstance(properties, authenticator)
    val message = new MimeMessage(session)
    message.setFrom(emailServiceConfig.from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    Transport.send(message)
  }

  def sendPasswordRecoveryEmail(to: String, token: String): F[Unit] = {
    val subject = "Toloka: Password Recovery"
    val content = s"""
    <div style="
        border: 1px solid black;
        padding: 20px;
        font-family: sans-serif;
        line-height: 2;
        font-size: 20px;
    ">
    <h1>Email from Toloka</h1>
     <p>
        Enter this token on the <a href="$frontendUrl/recoverpassword">password recovery page</a>
    </p>
    </div>
    """
    
    sendEmail(to, subject, content)
  }

  private def properties: Properties = {
    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", true)
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.ssl.trust", host)

    prop
  }

  private def authenticator: Authenticator = new Authenticator {
    override protected def getPasswordAuthentication(): PasswordAuthentication =
      new PasswordAuthentication(user, pass)
  }
}

object LiveEmails {
  def apply[F[_]: Sync](emailServiceConfig: EmailServiceConfig): F[LiveEmails[F]] =
    Sync[F].delay(new LiveEmails[F](emailServiceConfig))
}
