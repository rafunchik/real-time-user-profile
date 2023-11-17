package com.ad.mpc.p10n.realtimeuserprofile.model.history

import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.InteractionType
import com.ad.mpc.p10n.realtimeuserprofile.model.sdrn.SDRN
import org.scalatest.{FlatSpec, Matchers}

class UserHistorySpec extends FlatSpec with Matchers {

  val interactionKey = InteractionKey("aProvider", "aUser")
  val itemId = SDRN("sdrn:willhabenat:classified:332852942")
  val baseInteraction = Interaction(123L, itemId, InteractionType.View)

  "aggregate" should "add an interaction to a empty user history" in {
    UserHistory.aggregate(interactionKey, baseInteraction, UserHistory.Empty) shouldBe UserHistory(
      123L,
      Vector(baseInteraction)
    )
  }

  it should "evict oldest when adding newer" in {
    val interactions = timestampsToInteractions(100 to 109)
    val history = UserHistory(interactions.last.timestamp, interactions)

    UserHistory.aggregate(interactionKey, baseInteraction.copy(timestamp = 110L), history) shouldBe UserHistory(
      110L,
      timestampsToInteractions(101 to 110)
    )
  }

  it should "evict oldest even when adding in the middle" in {
    val interactions = timestampsToInteractions(100 to 104) ++ timestampsToInteractions(106 to 110)
    val history = UserHistory(109L, interactions)

    UserHistory.aggregate(interactionKey, baseInteraction.copy(timestamp = 105L), history) shouldBe UserHistory(
      110L,
      timestampsToInteractions(101 to 110)
    )
  }

  def timestampsToInteractions(range: Range): Vector[Interaction] = range
    .map(t => baseInteraction.copy(timestamp = t))
    .toVector
}
