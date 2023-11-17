package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.ad.mpc.p10n.realtimeuserprofile.model.sdrn.SDRN
import io.circe.{Encoder, Json}
import io.circe.generic.auto._

object PulseEventEncoder {
  import io.circe.syntax._

  implicit val encodeSDRN: Encoder[SDRN] = (s: SDRN) => Json.fromString(s.value)

  implicit val encodeZonedDateTime: Encoder[ZonedDateTime] = (z: ZonedDateTime) => {
    Json.fromString(
      z.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("Z", "+00:00")
    )
  }

  /**
    * Encoder for a [SimplifiedPulseEvent] and its subclasses.
    * Instead of using circe's automatic derivation, since the
    * type hierarchy has a field defined at the root, we need to manually
    * inject such field with a deepMerge operation
    */
  implicit val encodePulseEvent: Encoder[SimplifiedPulseEvent] = Encoder.instance(a => {
    val sub = a match {
      case call: SimplifiedPulseCall => call.asJson
      case view: SimplifiedPulseView => view.asJson
      case sms: SimplifiedPulseSMS => sms.asJson
      case save: SimplifiedPulseSave => save.asJson
      case show: SimplifiedPulseShow => show.asJson
      case send: SimplifiedPulseSend => send.asJson
      case unsave: SimplifiedPulseUnsave => unsave.asJson
    }
    // need to manually inject the type
    sub.deepMerge(Json.obj("@type" -> a.`@type`.asJson))
  })

}
