package com.toloka.cho.admin.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class PostgressConfig(nThreads: Int, url: String, user: String, pass: String) derives ConfigReader {

}
