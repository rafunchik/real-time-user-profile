package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import io.circe.DecodingFailure
import org.scalatest.{FlatSpec, Inside, Matchers}
import scala.io.Source

class PulseEventDecoderSpec extends FlatSpec with Matchers with Inside {
  val validEvent1 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-interaction-identified-willhaben.json"
  val validEvent2 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-interaction-anon-avitoma.json"
  val validEvent3 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-interaction-avitoma-missing-actor.json"
  val validEvent4 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-call-avito-bogus-object-id.json"
  val validEvent5 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-call-avito-no-object-id.json"
  val validEvent6 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-sms-avito-bogus-object-id.json"
  val validEvent7 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-save-avito.json"
  val validEvent8 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-show-avito.json"
  val validEvent9 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-send-avito.json"
  val validEvent10 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-unsave-avito.json"
  val validEvent11 = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-show-avito-no-in-reply-to.json"

  "PulseEventParser.decode" should "parse a valid event" in {
    val json = Source.fromResource(validEvent1).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidIdentifiedPulseInteractionWillhaben
    inside(interaction) {
      case Right(p: SimplifiedPulseView) => p shouldBe expected
    }
  }

  it should "parse a valid identified event from avito" in {
    val json = Source.fromResource(validEvent2).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected =  ValidPulseAnonInteractionAvito
    inside(interaction) {
      case Right(p: SimplifiedPulseView) => p shouldBe expected
    }
  }

  it should "parse a valid identified event from avito 2" in {
    val json = Source.fromResource(validEvent3).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidPulseAnonInteractionAvitoMissingActor
    inside(interaction) {
      case Right(p: SimplifiedPulseView) => p shouldBe expected
    }
  }

  it should "parse a call event with a bogus id" in {
    val json = Source.fromResource(validEvent4).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidPulseCallAvitoBogusObjectId
    inside(interaction) {
      case Right(p: SimplifiedPulseCall) => p shouldBe expected
    }
  }

  it should "parse a call event with no id" in {
    val json = Source.fromResource(validEvent5).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidPulseCallAvitoNoObjectId
    inside(interaction) {
      case Right(p: SimplifiedPulseCall) => p shouldBe expected
    }
  }

  it should "parse a SMS event with a bogus id" in {
    val json = Source.fromResource(validEvent6).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidPulseSMSAvitoBogusObjectId
    inside(interaction) {
      case Right(p: SimplifiedPulseSMS) => p shouldBe expected
    }
  }

  it should "parse a Save event" in {
    val json = Source.fromResource(validEvent7).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidPulseSaveAvito
    inside(interaction) {
      case Right(p: SimplifiedPulseSave) => p shouldBe expected
    }
  }

  it should "parse a Show event" in {
    val json = Source.fromResource(validEvent8).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidPulseShowAvito
    inside(interaction) {
      case Right(p: SimplifiedPulseShow) => p shouldBe expected
    }
  }

  it should "parse a Send event" in {
    val json = Source.fromResource(validEvent9).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidPulseSendAvito
    inside(interaction) {
      case Right(p: SimplifiedPulseSend) => p shouldBe expected
    }
  }

  it should "parse a Unsave event" in {
    val json = Source.fromResource(validEvent10).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidPulseUnsaveAvito
    inside(interaction) {
      case Right(p: SimplifiedPulseUnsave) => p shouldBe expected
    }
  }

  it should "parse a show event without the inReplyTo field" in {
    val json = Source.fromResource(validEvent11).mkString

    val interaction = PulseEventDecoder.decode(json)
    val expected = ValidPulseShowAvitoNoInReplyTo
    inside(interaction) {
      case Right(p: SimplifiedPulseShow) => p shouldBe expected
    }
  }

  it should "not parse an invalid event" in {
    val resource = "com/ad/mpc/p10n/realtimeuserprofile/model/pulse/pulse-interaction-missing-published.json"
    val json = Source.fromResource(resource).mkString

    val interaction = PulseEventDecoder.decode(json)

    inside(interaction) {
      case Left(_: DecodingFailure) =>
    }
  }

}
