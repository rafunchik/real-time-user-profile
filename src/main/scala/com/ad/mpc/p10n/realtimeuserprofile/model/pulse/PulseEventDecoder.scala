package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import cats.effect.IO
import com.ad.mpc.p10n.realtimeuserprofile.model.sdrn.SDRN
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe._
import io.circe.generic.auto._
import io.circe.parser.{decode => decodeJson}

object PulseEventDecoder {
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]


  implicit val decodeSDRN: Decoder[SDRN] = (c: HCursor) => c.value.asString
    .flatMap(SDRN.Valid.unapply)
    .toRight(DecodingFailure(s"Failure decoding string as a valid SDRN: ${c.value.asString}", c.history))

  implicit val decodePulseEvent: Decoder[SimplifiedPulseEvent] = Decoder.instance(e => {
    val interactionType = e.downField("@type").as[InteractionType]
    val decoder = interactionType match {
      case Right(InteractionType.View) => Decoder[SimplifiedPulseView]
      case Right(InteractionType.Call) => Decoder[SimplifiedPulseCall]
      case Right(InteractionType.SMS) => Decoder[SimplifiedPulseSMS]
      case Right(InteractionType.Save) => Decoder[SimplifiedPulseSave]
      case Right(InteractionType.Show) => Decoder[SimplifiedPulseShow]
      case Right(InteractionType.Send) => Decoder[SimplifiedPulseSend]
      case Right(InteractionType.Unsave) => Decoder[SimplifiedPulseUnsave]
      case _ => throw DecodingFailure(s"Unsupported type: $interactionType", e.history)
    }
    decoder.apply(e).left.map(failure => {
      logger.error(s"Unable to decode event of type $interactionType: ${e.value.toString()}")
      failure
    })
  })

  def decode(json: String): Either[Error, SimplifiedPulseEvent] = decodeJson[SimplifiedPulseEvent](json)
}
