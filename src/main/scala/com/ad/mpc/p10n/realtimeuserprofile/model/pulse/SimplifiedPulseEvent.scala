package com.ad.mpc.p10n.realtimeuserprofile.model.pulse

import java.time.ZonedDateTime

import com.ad.mpc.p10n.realtimeuserprofile.model.sdrn.SDRN

/**
  * Model the hierarchy of pulse events
  */
sealed trait SimplifiedPulseEvent {
  val `@type`: InteractionType
}


/**
  * Implements https://github.mpi-internal.com/spt-dataanalytics/routing/blob/master/src/main/resources/transforms/mp-personalisation-interactions.jstl2
  *
  * @param published
  * @param `object`
  * @param actor
  * @param device
  * @param provider
  * @param tracker
  */
case class SimplifiedPulseView(published: ZonedDateTime,
                               `object`: ViewObject,
                               actor: Option[Actor],
                               device: Device,
                               provider: Provider,
                               tracker: Tracker
                              ) extends SimplifiedPulseEvent{
  override final val `@type`: InteractionType = InteractionType.View
}

final case class SimplifiedPulseCall(published: ZonedDateTime,
                               `object`: CallObject,
                               actor: Option[Actor],
                               device: Device,
                               provider: Provider,
                               tracker: Tracker
                              ) extends SimplifiedPulseEvent {
  override val `@type`: InteractionType = InteractionType.Call
}

final case class SimplifiedPulseSMS(published: ZonedDateTime,
                                    `object`: SMSObject,
                                    actor: Option[Actor],
                                    device: Device,
                                    provider: Provider,
                                    tracker: Tracker) extends SimplifiedPulseEvent {
  override val `@type`: InteractionType = InteractionType.SMS
}

final case class SimplifiedPulseSave(published: ZonedDateTime,
                              `object`: SaveObject,
                              actor: Option[Actor],
                              device: Device,
                              provider: Provider,
                              tracker: Tracker
                             ) extends SimplifiedPulseEvent{
  override val `@type`: InteractionType = InteractionType.Save
}

final case class SimplifiedPulseUnsave(published: ZonedDateTime,
                                       `object`: SaveObject,
                                       actor: Option[Actor],
                                       device: Device,
                                       provider: Provider,
                                       tracker: Tracker
                                      ) extends SimplifiedPulseEvent {
  override val `@type`: InteractionType = InteractionType.Unsave
}

final case class SimplifiedPulseShow(published: ZonedDateTime,
                                     `object`: ShowObject,
                                     actor: Option[Actor],
                                     device: Device,
                                     provider: Provider,
                                     tracker: Tracker) extends SimplifiedPulseEvent {
  override val `@type`: InteractionType = InteractionType.Show
}

final case class SimplifiedPulseSend(published: ZonedDateTime,
                                     `object`: SendObject,
                                     actor: Option[Actor],
                                     device: Device,
                                     provider: Provider,
                                     tracker: Tracker) extends SimplifiedPulseEvent {
  override val `@type`: InteractionType = InteractionType.Send
}

final case class CallObject(`@id`: Option[String],
                            `@type`: ObjectType,
                            name: Option[String],
                            inReplyTo: Reply)

case class Location(
                     addressRegion: Option[String],
                     addressLocality: Option[String],
                     postalCode: Option[String]
                   )

final case class SaveObject(`@id`: String,
                            `@type`: ObjectType,
                            category: Option[String],
                            name: Option[String],
                            price: Option[Double],
                            currency: Option[String],
                            location: Option[Location])

case class ViewObject(
                   `@id`: String,
                   adId: Option[Long],
                   `@type`: ObjectType,
                   category: Option[String],
                   name: Option[String],
                   url: Option[String],
                   price: Option[Double],
                   location: Option[Location]
                 )

final case class SMSObject(`@id`: Option[String],
                     `@type`: ObjectType,
                     name: Option[String],
                     inReplyTo: Reply
                     )

final case class ShowObject(`@id`: Option[String],
                            `@type`: ObjectType,
                            name: Option[String],
                            url: Option[String],
                            inReplyTo: Option[Reply])

final case class SendObject(`@id`: Option[String],
                            `@type`: ObjectType,
                            name: Option[String],
                            inReplyTo: Reply)

case class Reply(`@id`: String,
                 adId: Option[Long])

case class Actor(`spt:userId`: String)

case class Device(environmentId: String)

case class Provider(`@id`: String)

case class Tracker(`type`: String, name: String)

object SimplifiedPulseView {
  implicit class RichSimplifiedPulseEvent(event: SimplifiedPulseView) {

    def site: String = event.provider.`@id`

    def userId: Option[SDRN] = event.actor
        .flatMap(a => SDRN.Valid.unapply(a.`spt:userId`))
        .orElse(SDRN.Valid.unapply(event.device.environmentId))

    def objectId: Option[SDRN] = SDRN.Valid.unapply(event.`object`.`@id`)
  }
}
