package com.ad.mpc.p10n.realtimeuserprofile.model.history

import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.InteractionType
import com.ad.mpc.p10n.realtimeuserprofile.model.sdrn.SDRN

case class UserHistory(lastEpoch: Long,
                       interactions: Vector[Interaction],
                       maxSize: Int = 10) {
  def size: Int = interactions.size

  require(interactions.length <= maxSize)
}

case class Interaction(timestamp: Long,
                       itemId: SDRN,
                       interactionType: InteractionType)

case class InteractionKey(providerId: String,
                          userId: String)

object UserHistory {
  var Empty = UserHistory(0L, Vector.empty)

  def aggregate(key: InteractionKey, value: Interaction, history: UserHistory): UserHistory = {
    val (interactionsBefore, interactionsAfter) = history.interactions.partition(_.timestamp <= value.timestamp)
    val updatedInteractions = ((interactionsBefore :+ value) ++ interactionsAfter).takeRight(history.maxSize)
    UserHistory(updatedInteractions.last.timestamp, updatedInteractions, history.maxSize)
  }
}
