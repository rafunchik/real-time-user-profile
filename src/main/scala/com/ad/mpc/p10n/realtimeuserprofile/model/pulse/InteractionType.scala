package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import enumeratum._

import scala.collection.immutable

sealed trait InteractionType extends EnumEntry

case object InteractionType extends Enum[InteractionType] with CirceEnum[InteractionType] {
  // Events on Classified Ads
  case object View extends InteractionType
  case object Save extends InteractionType
  case object Unsave extends InteractionType
  case object Create extends InteractionType

  // Events on Messages
  case object Send extends InteractionType

  // Event on Phone Contacts
  case object Show extends InteractionType
  case object Call extends InteractionType
  case object SMS extends InteractionType

  // Events on Listings
  case object Engagement extends InteractionType

  val values: immutable.IndexedSeq[InteractionType] = findValues
}
