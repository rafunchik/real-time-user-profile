package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import org.scalatest.{FlatSpec, Matchers}
import .ValidIdentifiedPulseInteractionWillhaben
import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.SimplifiedPulseView.RichSimplifiedPulseEvent
import com.ad.mpc.p10n.realtimeuserprofile.model.sdrn.SDRN

class SimplifiedPulseViewSpec extends FlatSpec with Matchers {

  val validUserIdSdrn = "sdrn:a:valid:id"
  val validIdentifiedActor = Some(Actor(validUserIdSdrn))
  val unidentifiedActor = Some(Actor("random-string"))

  ".site" should "extract site correctly" in {
    val interaction = ValidIdentifiedPulseInteractionWillhaben.copy(provider = Provider("testSite"))
    interaction.site shouldBe "testSite"
  }

  ".objectId" should "extract object id correctly" in {
    val interaction = ValidIdentifiedPulseInteractionWillhaben.copy(`object` =
      ValidIdentifiedPulseInteractionWillhaben.`object`.copy(`@id` = "sdrn:b:c:d")
    )

    interaction.objectId shouldBe Some(SDRN("sdrn:b:c:d"))
  }

  ".userId" should "extract userId string if available" in {
    val interaction = ValidIdentifiedPulseInteractionWillhaben.copy(actor = validIdentifiedActor)

    interaction.userId shouldBe Some(SDRN(validUserIdSdrn))
  }

  it should "extract env id if user id is not available" in {
    val interaction = ValidIdentifiedPulseInteractionWillhaben.copy(
      actor = unidentifiedActor,
      device = ValidIdentifiedPulseInteractionWillhaben.device.copy(validUserIdSdrn)
    )

    interaction.userId shouldBe Some(SDRN(validUserIdSdrn))
  }

  it should "return null if none is available " in {
    val interaction = ValidIdentifiedPulseInteractionWillhaben.copy(
      actor = unidentifiedActor,
      device = ValidIdentifiedPulseInteractionWillhaben.device.copy("random-string")
    )

    interaction.userId shouldBe None
  }

}
