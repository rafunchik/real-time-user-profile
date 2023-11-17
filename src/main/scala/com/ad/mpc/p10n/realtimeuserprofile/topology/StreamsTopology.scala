package com.ad.mpc.p10n.realtimeuserprofile.topology

import cats.effect.{Clock, Effect}
import cats.syntax.functor._
import com.ad.mpc.p10n.realtimeuserprofile.framework.config.KafkaAppConfig
import com.ad.mpc.p10n.realtimeuserprofile.model.history.{Interaction, InteractionKey, UserHistory}
import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.Implicits.ZonedDateTimeDateConverters
import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.{InteractionType, SimplifiedPulseEvent, SimplifiedPulseView}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.azhur.kafkaserdecirce.CirceSupport
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.StreamsBuilder
import UserInteractionsTopology._

trait StreamsTopology[F[_]] {
  def build(config: KafkaAppConfig): F[Topology]
}

object StreamsTopology {
  def apply[F[_]](implicit materialisationTopology: StreamsTopology[F]): StreamsTopology[F] = materialisationTopology
}

class UserInteractionsTopology[F[_] : Effect](implicit clock: Clock[F]) extends StreamsTopology[F] with CirceSupport {
  implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

  private def buildTopologyForSites(builder: StreamsBuilder, enabledSites: Seq[String]): Unit = {
    enabledSites.foreach { site =>
      logger.info(s"Building topology for: $site")
      buildTopology(builder, site)
    }
  }

  private def buildTopology(builder: StreamsBuilder, site: String): Unit = {
    import io.circe.generic.auto._
    import org.apache.kafka.streams.scala.Serdes._
    import org.apache.kafka.streams.scala.ImplicitConversions._
    import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.PulseEventDecoder._
    import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.PulseEventEncoder._

    builder
      .stream[String, SimplifiedPulseEvent](inputTopics(site))
      .filter(isViewEvent)
      .mapValues(e => e.asInstanceOf[SimplifiedPulseView])
      .flatMap(makeInteractionPair)
      .groupByKey
      .aggregate[UserHistory](UserHistory.Empty)(UserHistory.aggregate)
      .toStream
      .to(outputTopic(site))
  }

  def build(kafkaConfig: KafkaAppConfig): F[Topology] = {
    for {
      _       <- logger.info("Creating the topology for the realtime user profile service ...")
      builder  = new StreamsBuilder()
      _        = buildTopologyForSites(builder, kafkaConfig.enabledSites)
      topology = builder.build()
    } yield {
      logger.info(s"Topology:\n${topology.describe().toString}")
      topology
    }
  }
}

object UserInteractionsTopology {

  private[topology] def inputTopics(site: String): Set[String] = Set(s"pulse-interactions-yellow-$site", s"pulse-interactions-red-$site")

  private[topology] def outputTopic(site: String): String = s"user-profile-interactions-$site"

  def makeInteractionPair(k: String, v: SimplifiedPulseView): Option[(InteractionKey, Interaction)] = {
    for {
      uId <- v.userId
      oId <- v.objectId
    } yield (
      InteractionKey(v.provider.`@id`, uId.value),
      Interaction(v.published.asEpochMilli, oId, v.`@type`)
    )
  }

  private def isViewEvent(k: String, v: SimplifiedPulseEvent): Boolean = v.`@type` == InteractionType.View
}
