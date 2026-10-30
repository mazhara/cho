package com.toloka.cho.server.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class PostgresConfig(nThreads: Int, url: String, user: String, pass: String) derives ConfigReader {

}
