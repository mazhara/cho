package com.toloka.cho.server.config

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

import scala.concurrent.duration.*

class TokenConfigSpec extends AnyFreeSpec with Matchers {
  "TokenConfig" - {
    "loads the default lifetime with explicit units" in {
      ConfigSource.default.at("token-config").load[TokenConfig] shouldBe
        Right(TokenConfig(10.minutes))
    }

    "reads durations in different units" in {
      ConfigSource.string("""token-duration = "2 seconds"""").load[TokenConfig] shouldBe
        Right(TokenConfig(2.seconds))
    }

    "rejects infinite lifetimes" in {
      ConfigSource.string("""token-duration = "Inf"""").load[TokenConfig].isLeft shouldBe true
    }
  }
}
