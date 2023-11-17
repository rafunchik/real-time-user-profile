package com.ad.mpc.p10n.realtimeuserprofile

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.ad.mpc.p10n.realtimeuserprofile.framework.config.KafkaAppConfig
import com.ad.mpc.p10n.realtimeuserprofile.framework.secrets.AppSecrets
import com.ad.mpc.p10n.realtimeuserprofile.topology.StreamsTopology
import com.schibsted.mp.p10n.kafka.config.{KafkaCredentials, KafkaProperties}
import com.schibsted.mp.p10n.kafka.streams.config.KafkaStreamsConfig
import com.schibsted.mp.p10n.kafka.streams.{KafkaStreamsState, KafkaStreamsSystem}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.streams.Topology

trait StreamsRunner[F[_]] {

  def run(appConfig: KafkaAppConfig, streamsConfig: KafkaStreamsConfig, secrets: AppSecrets[F])
         (implicit clock: Clock[F]): F[KafkaStreamsState]
}

object StreamsRunner {
  def apply[F[_]](implicit streamsRunner: StreamsRunner[F]): StreamsRunner[F] = streamsRunner
}

class DefaultStreamsRunner[F[_] : Sync : StreamsTopology] extends StreamsRunner[F] {

  private def runKafkaStreams(secrets: AppSecrets[F],
                              config: KafkaStreamsConfig,
                              topology: Topology): F[KafkaStreamsState] = {
    Sync[F].bracket {
      createKafkaSystem(secrets, config, topology)
    } {
      kafkaStreamsSystem => {
        for {
          state <- Sync[F].fromEither(kafkaStreamsSystem.start())
          _     <- Sync[F].tailRecM(state)(waitForKafkaStreams)
        } yield state
      }
    } {
      kafkaStreamsSystem => Sync[F].delay(kafkaStreamsSystem.stop())
    }
  }

  private def createKafkaSystem(secrets: AppSecrets[F],
                                kafkaStreamsConfig: KafkaStreamsConfig,
                                topology: Topology): F[KafkaStreamsSystem] = {
    for {
      credentialsEither   <- secrets.kafkaCredentials
      credentials         <- Sync[F].fromEither(credentialsEither)
      config               = configWithCredentials(kafkaStreamsConfig, credentials)
      system              <- Sync[F].fromEither(KafkaStreamsSystem(config, topology))
    } yield system
  }

  private def configWithCredentials(config: KafkaStreamsConfig, credentials: KafkaCredentials): KafkaStreamsConfig = {
    val jaasConfig = "org.apache.kafka.common.security.scram.ScramLoginModule" +
      s""" required username="${credentials.user}" password="${credentials.password}";"""

    val credentialProperties = Map(
      SaslConfigs.SASL_JAAS_CONFIG -> jaasConfig,
      SaslConfigs.SASL_MECHANISM -> "SCRAM-SHA-256",
      CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> SecurityProtocol.SASL_SSL.name
    )

    KafkaStreamsConfig(
      numThreads = config.numThreads,
      properties = KafkaProperties(config.properties.values ++ credentialProperties),
      collectedMetrics = config.collectedMetrics
    )
  }

  override def run(appConfig: KafkaAppConfig, streamsConfig: KafkaStreamsConfig, secrets: AppSecrets[F])
                  (implicit clock: Clock[F]): F[KafkaStreamsState] = {
    for {
      topology           <- StreamsTopology[F].build(appConfig)
      kafkaStreamsState  <- runKafkaStreams(secrets, streamsConfig, topology)
    } yield kafkaStreamsState
  }

  private def waitForKafkaStreams(state: KafkaStreamsState): F[Either[KafkaStreamsState, Unit]] = {
    Sync[F].suspend {
      if (state.isAlive) {
        Sync[F].delay {
          Thread.sleep(1000)
          Left(state)
        }
      }
      else {
        Sync[F].pure(Right(()))
      }
    }
  }
}
