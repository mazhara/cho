package com.toloka.cho.server.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.toloka.cho.server.config.EmailServiceConfig
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{InetAddress, ServerSocket}
import java.nio.charset.StandardCharsets.UTF_8
import javax.mail.internet.AddressException
import javax.mail.MessagingException
import scala.concurrent.duration.*

class EmailsSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  private def config(port: Int): EmailServiceConfig =
    EmailServiceConfig(
      host = "127.0.0.1",
      port = port,
      user = "smtp-user",
      pass = "smtp-password",
      frontendUrl = "http://localhost:1234",
      from = "library@example.com"
    )

  private val smtpServer = Resource.make(
    IO.blocking {
      val server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress)
      server.setSoTimeout(5000)
      server
    }
  )(server => IO.blocking(server.close()))

  private def receiveEmail(server: ServerSocket): IO[String] =
    Resource.make(IO.blocking(server.accept()))(socket => IO.blocking(socket.close())).use { socket =>
      IO.blocking {
        socket.setSoTimeout(5000)
        val reader = new BufferedReader(new InputStreamReader(socket.getInputStream, UTF_8))
        val writer = new PrintWriter(socket.getOutputStream, true)
        val received = new StringBuilder
        def reply(line: String): Unit = {
          writer.print(s"$line\r\n")
          writer.flush()
        }

        reply("220 localhost SMTP")
        var finished = false
        while (!finished) {
          val line = reader.readLine()
          if (line == null) finished = true
          else if (line.startsWith("EHLO") || line.startsWith("HELO")) reply("250 localhost")
          else if (line.startsWith("MAIL FROM:") || line.startsWith("RCPT TO:")) {
            received.append(line).append("\n")
            reply("250 OK")
          } else if (line == "DATA") {
            reply("354 Send message")
            var bodyLine = reader.readLine()
            while (bodyLine != null && bodyLine != ".") {
              received.append(bodyLine).append("\n")
              bodyLine = reader.readLine()
            }
            if (bodyLine == null) throw new IllegalStateException("Incomplete SMTP message")
            reply("250 Accepted")
          } else if (line == "QUIT") {
            reply("221 Bye")
            finished = true
          } else throw new IllegalStateException(s"Unexpected SMTP command: $line")
        }
        received.toString
      }
    }

  "Emails" - {
    "rejects a server without STARTTLS before sending credentials or mail, on every attempt" in {
      smtpServer.use { server =>
        for {
          emails <- LiveEmails[IO](config(server.getLocalPort))
          send = emails.sendEmail("reader@example.com", "Library update", "<p>Hello</p>")
          first <- IO.both(receiveEmail(server), send.attempt)
          second <- IO.both(receiveEmail(server), send.attempt)
        } yield {
          List(first, second).foreach { case (message, result) =>
            message shouldBe empty
            result.left.toOption.get shouldBe a[MessagingException]
          }
        }
      }.timeout(15.seconds)
    }

    "requires TLS and hostname verification without overriding certificate trust" in {
      LiveEmails[IO](config(1)).map { emails =>
        emails.properties.getProperty("mail.smtp.starttls.enable") shouldBe "true"
        emails.properties.getProperty("mail.smtp.starttls.required") shouldBe "true"
        emails.properties.getProperty("mail.smtp.ssl.checkserveridentity") shouldBe "true"
        emails.properties.containsKey("mail.smtp.ssl.trust") shouldBe false
      }
    }

    "includes the recovery code and page link in the email" in {
      for {
        sent <- Ref.of[IO, String]("")
        emails = new LiveEmails[IO](config(1)) {
          override def sendEmail(to: String, subject: String, content: String): IO[Unit] =
            sent.set(content)
        }
        _ <- emails.sendPasswordRecoveryEmail("reader@example.com", "recovery-code-123")
        content <- sent.get
      } yield {
        content should include("<code>recovery-code-123</code>")
        content should include("http://localhost:1234/recoverpassword")
      }
    }

    "defers message construction and captures invalid addresses in the effect" in {
      for {
        emails <- LiveEmails[IO](config(1))
        send <- IO(emails.sendEmail("invalid@@example.com", "Subject", "Body"))
        result <- send.attempt
      } yield result.left.toOption.get shouldBe a[AddressException]
    }
  }
}
