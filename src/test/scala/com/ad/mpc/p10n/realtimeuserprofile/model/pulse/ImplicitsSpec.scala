package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import java.time.ZonedDateTime

import org.scalatest.{FlatSpec, Matchers}
import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.Implicits.ZonedDateTimeDateConverters

class ImplicitsSpec extends FlatSpec with Matchers {

  "ZonedDateTimeDateConverters" should "convert a ZDT to epoch milliseconds" in {
    ZonedDateTime.parse("2011-12-03T10:15:30+01:00[Europe/Paris]").asEpochMilli shouldBe 1322903730000L
  }
}
