package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import io.circe.{DecodingFailure, Json, Printer}
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.io.Source
import io.circe.parser._
import PulseEventEncoder._
import io.circe.syntax._

class PulseEventEncoderSpec extends FlatSpec with Matchers with Inside {
  val pulseViewResource = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-interaction-identified-willhaben.json"
  val pulseCallResource = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-call-avito-bogus-object-id.json"
  val pulseSMSResource = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-sms-avito-bogus-object-id.json"
  val pulseSaveResource = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-save-avito.json"
  val pulseShowResource = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-show-avito.json"
  val pulseSendResource = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-send-avito.json"
  val pulseUnsaveResource = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-unsave-avito.json"

  val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  "encode" should "properly encode a valid SimplifiedPulseView" in {
    val json = ValidIdentifiedPulseInteractionWillhaben.asInstanceOf[SimplifiedPulseEvent].asJson

    val expected = prettifyResource(pulseViewResource)

    printer.pretty(json) shouldBe expected
  }

  it should "properly encode a SimplifiedPulseCall" in {
    val json = ValidPulseCallAvitoBogusObjectId.asInstanceOf[SimplifiedPulseEvent].asJson

    val expected = prettifyResource(pulseCallResource)

    printer.pretty(json) shouldBe expected
  }


  it should "properly encode a SimplifiedPulseSMS" in {
    val json = ValidPulseSMSAvitoBogusObjectId.asInstanceOf[SimplifiedPulseEvent].asJson

    val expected = prettifyResource(pulseSMSResource)

    printer.pretty(json) shouldBe expected
  }

  it should "properly encode a SimplifiedPulseSave" in {
    val json = ValidPulseSaveAvito.asInstanceOf[SimplifiedPulseEvent].asJson

    val expected = prettifyResource(pulseSaveResource)

    printer.pretty(json) shouldBe expected
  }

  it should "properly encode a SimplifiedPulseShow" in {
    val json = ValidPulseShowAvito.asInstanceOf[SimplifiedPulseEvent].asJson

    val expected = prettifyResource(pulseShowResource)

    printer.pretty(json) shouldBe expected
  }

  it should "properly encode a SimplifiedPulseSend" in {
    val json = ValidPulseSendAvito.asInstanceOf[SimplifiedPulseEvent].asJson

    val expected = prettifyResource(pulseSendResource)

    printer.pretty(json) shouldBe expected
  }

  it should "properly encode a SimplifiedPulseUnsave" in {
    val json = ValidPulseUnsaveAvito.asInstanceOf[SimplifiedPulseEvent].asJson

    val expected = prettifyResource(pulseUnsaveResource)

    printer.pretty(json) shouldBe expected
  }

  private def prettifyResource(resource: String): String = {
    val jsonFromResource = Source.fromResource(resource).mkString
    parse(jsonFromResource).getOrElse(Json.Null).spaces2
  }
}


