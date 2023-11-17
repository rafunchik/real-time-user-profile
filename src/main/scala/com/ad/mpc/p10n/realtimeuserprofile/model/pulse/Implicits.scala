package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import java.time.{Instant, ZonedDateTime}

object Implicits {

  implicit class DateConverters(date: String) {
    def asZonedDateTime: ZonedDateTime = ZonedDateTime.parse(date)
  }

  implicit class ZonedDateTimeDateConverters(date: ZonedDateTime) {
    def asEpochMilli: Long = date.toInstant.toEpochMilli
  }
}
