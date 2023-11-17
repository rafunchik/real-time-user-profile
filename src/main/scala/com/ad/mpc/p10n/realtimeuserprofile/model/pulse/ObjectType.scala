package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import enumeratum._

import scala.collection.immutable

sealed trait ObjectType extends EnumEntry

case object ObjectType extends Enum[ObjectType] with CirceEnum[ObjectType] {

  case object ClassifiedAd extends ObjectType
  case object Message extends ObjectType
  case object PhoneContact extends ObjectType

  val values: immutable.IndexedSeq[ObjectType] = findValues
}


