package com.ad.mpc.p10n.realtimeuserprofile.topology

import java.nio.file.Files
import java.util.Properties

import com.ad.mpc.p10n.realtimeuserprofile.topology.KafkaStreamsFixtures.{InputPipe, OutputPipe}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.kafka.streams.{StreamsConfig, Topology, TopologyTestDriver}
import org.apache.kafka.streams.test.ConsumerRecordFactory


object KafkaStreamsFixtures {
  class InputPipe(testDriver: TopologyTestDriver) {
    private var offset = 0L

    def send[K, V](topic: String, key: K, value: V)
                  (implicit keySer: Serializer[K], valueSer: Serializer[V]): Unit = {

      val consumerRecordFactory = new ConsumerRecordFactory(topic, keySer, valueSer)

      testDriver.pipeInput(consumerRecordFactory.create(topic, key, value))

      offset += 1
    }

    def send[K, V](topic: String, values: List[(K,V)])
                  (implicit keySer: Serializer[K], valueSer: Serializer[V]): Unit = {
      values.foreach { case (k, v) => send(topic, k, v) }
    }
  }

  class OutputPipe(testDriver: TopologyTestDriver) {
    def readOption[K, V](topic: String)
                        (implicit keySer: Deserializer[K], valueSer: Deserializer[V]): Option[ProducerRecord[K, V]] = {
      Option(read(topic))
    }

    def read[K, V](topic: String)
                  (implicit keySer: Deserializer[K], valueSer: Deserializer[V]): ProducerRecord[K, V] = {

      testDriver.readOutput(topic, keySer, valueSer)
    }

    def readN[K, V](topic: String, n: Int)
                   (implicit keySer: Deserializer[K], valueSer: Deserializer[V]): List[ProducerRecord[K, V]] = {
      (1 to n).map(_ => read[K, V](topic)).toList
    }
  }
}

trait KafkaStreamsFixtures {

  def withTopologyDriver(topology: Topology)(test: TopologyTestDriver => Any): Any = {
    val tempDir = Files.createTempDirectory("kafka-streams-test-")

    val properties = new Properties()
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "test")
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234")
    properties.put(StreamsConfig.STATE_DIR_CONFIG, tempDir.toString)

    val testDriver = new TopologyTestDriver(topology, properties)

    try {
      test(testDriver)
    }
    finally {
      tempDir.toFile.delete()
    }
  }

  def withInputPipe(testDriver: TopologyTestDriver)(test: InputPipe => Any): Any = {
    val pipeInput = new InputPipe(testDriver)
    test(pipeInput)
  }

  def withOutputPipe(testDriver: TopologyTestDriver)(test: OutputPipe => Any): Any = {
    val pipeOutput = new OutputPipe(testDriver)
    test(pipeOutput)
  }

  def withInputAndOutputPipes(testDriver: TopologyTestDriver)(test: (InputPipe, OutputPipe) => Any): Any = {
    val pipeInput = new InputPipe(testDriver)
    val pipeOutput = new OutputPipe(testDriver)
    test(pipeInput, pipeOutput)
  }
}
