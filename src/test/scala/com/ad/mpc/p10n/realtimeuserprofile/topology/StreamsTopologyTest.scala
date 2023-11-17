package com.ad.mpc.p10n.realtimeuserprofile.topology

import cats.effect.{Clock, IO}
import com.ad.mpc.p10n.realtimeuserprofile.framework.IOClock
import com.ad.mpc.p10n.realtimeuserprofile.framework.config.KafkaAppConfig
import com.ad.mpc.p10n.realtimeuserprofile.model.history.{Interaction, InteractionKey, UserHistory}
import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.InteractionType.View
import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.{InteractionType, SimplifiedPulseEvent, SimplifiedPulseView}
import com.ad.mpc.p10n.realtimeuserprofile.model.sdrn.SDRN
import io.github.azhur.kafkaserdecirce.CirceSupport
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.apache.kafka.streams.TopologyDescription
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar

class StreamsTopologyTest extends FlatSpec with Matchers with MockitoSugar with KafkaStreamsFixtures with CirceSupport {

  behavior of classOf[UserInteractionsTopology[IO]].getSimpleName

  import StreamsTopologyTest._

  import collection.JavaConverters._

  "Stream topology" should "build the expected subtopologies" in {
    val topology = new UserInteractionsTopology[IO].build(Config).unsafeRunSync

    val siteTopologies = topology.describe().subtopologies()

    siteTopologies should have size 2
    val inputNodes = siteTopologies.asScala.map(_.nodes())
    val nodes = inputNodes.flatMap(_.asScala)
    val sources = nodes.collect({ case s: TopologyDescription.Source => s })
    val sinks = nodes.collect({ case s: TopologyDescription.Sink => s})
    val srcTopics = sources.flatMap(_.topics().replaceAll("[\\[\\]\"]", "").split(", "))
    val sinkTopics = sinks.map(_.topic())

    srcTopics should contain allElementsOf InputTopics
    sinkTopics should contain allElementsOf Seq(OutputTopic)
  }

  it should "route a message to the right topic" in {
    import io.circe.generic.auto._
    import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.PulseEventEncoder._
    import com.ad.mpc.p10n.realtimeuserprofile.model.pulse.PulseEventDecoder._

    val topology = new UserInteractionsTopology[IO].build(Config).unsafeRunSync()

    val expectedKey = InteractionKey("sdrn:schibsted:client:willhabenat",
      "sdrn:iad.willhaben.at:user:f0839076-e249-4932-94f9")
    val expectedValue1 = UserHistory(1569228624000L, Vector(
      Interaction(1569228624000L, ValidIdentifiedPulseInteractionWillhaben.objectId.get, View)
    ))
    val expectedValue2 = UserHistory(1569228624000L, Vector(
      Interaction(1569228624000L, ValidIdentifiedPulseInteractionWillhaben.objectId.get, View),
      Interaction(1569228624000L, ValidIdentifiedPulseInteractionWillhaben.objectId.get, View)
    ))

    withTopologyDriver(topology) { testDriver =>
      withInputAndOutputPipes(testDriver) { (inputPipe, outputPipe) =>
        InputTopics.foreach { topic =>
          inputPipe.send(topic, Key, Value)
        }
        val records = outputPipe.readN[InteractionKey, UserHistory](OutputTopic, 2)

        records.map(_.key()) should contain theSameElementsAs Seq(expectedKey, expectedKey)
        records.map(_.value()) should contain theSameElementsAs Seq(expectedValue1, expectedValue2)

        outputPipe.readOption(OutputTopic) shouldBe None

      }
    }
  }

  "makeInteractionPair" should "generate an interaction pair when values are defined, and user is from actor" in {
    val interaction = ValidIdentifiedPulseInteractionWillhaben

    UserInteractionsTopology.makeInteractionPair("aKey", interaction) shouldBe Some(
      InteractionKey("sdrn:schibsted:client:willhabenat", "sdrn:iad.willhaben.at:user:f0839076-e249-4932-94f9"),
      Interaction(1569228624000L, SDRN("sdrn:willhabenat:classified:332852942"), InteractionType.View)
    )
  }

  it should "generate an interaction pair when values are defined, and user is from environment" in {
    val interaction = ValidPulseAnonInteractionAvitoMissingActor

    UserInteractionsTopology.makeInteractionPair("aKey", interaction) shouldBe Some(
      InteractionKey("sdrn:schibsted:client:avitoma", "sdrn:schibsted:environment:8f9bf37c-278f-4f30-ab77-d9a503d8f3b6"),
      Interaction(1570443742000L, SDRN("sdrn:avitoma:classified:36061855"), InteractionType.View)
    )
  }

  it should "not generate an interaction when neither actor or device are defined" in {
    // The following interaction already has a device id that is not a valid SDRN
    val interaction = ValidIdentifiedPulseInteractionWillhaben.copy(actor = None)

    UserInteractionsTopology.makeInteractionPair("aKey", interaction) shouldBe None
  }

  it should "not generate an interaction when objectId is not defined" in {
    // The following interaction already has a device id that is not a valid SDRN
    val interaction = ValidIdentifiedPulseInteractionWillhaben.copy(
      `object` = ValidIdentifiedPulseInteractionWillhaben.`object`.copy(`@id` = "invalid-sdrn")
    )

    UserInteractionsTopology.makeInteractionPair("aKey", interaction) shouldBe None
  }
}

object StreamsTopologyTest extends MockitoSugar {
  private implicit val clock: Clock[IO] = new IOClock
  private val Site = "willhabenat"
  val InputTopics: Set[String] = UserInteractionsTopology.inputTopics(Site)
  val OutputTopic: String = UserInteractionsTopology.outputTopic(Site)
  private val Config = KafkaAppConfig(enabledSites = Seq(Site))

  implicit val serializer: StringSerializer = new StringSerializer()
  implicit val deserializer: StringDeserializer = new StringDeserializer()
  val Key = "lbc"
  val Value: SimplifiedPulseEvent = ValidIdentifiedPulseInteractionWillhaben
}
