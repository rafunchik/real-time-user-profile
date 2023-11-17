package com.ad.mpc.p10n.realtimeuserprofile.framework.config


import cats.effect.Sync
import cats.implicits._
import com.ad.mpc.p10n.realtimeuserprofile.framework.metrics.MetricsConfig
import com.ad.mpc.p10n.realtimeuserprofile.framework.secrets.SecretsConfig
import com.schibsted.mp.p10n.aws.AwsConfig
import com.schibsted.mp.p10n.kafka.streams.config.KafkaStreamsConfig
import com.typesafe.config.Config
import io.chrisdavenport.log4cats.Logger
import pureconfig.error.{ConfigReaderFailures, ThrowableFailure}
import pureconfig.{ConfigReader, Derivation}

import scala.reflect.runtime.universe.TypeTag


case class AppConfig(app: KafkaAppConfig,
                     kafkaStreams: KafkaStreamsConfig,
                     secrets: SecretsConfig,
                     aws: AwsConfig,
                     metrics: MetricsConfig)

case class PureConfigException(failures: ConfigReaderFailures) extends Exception({
  val messages = failures.toList
    .map(failure => s"- ${failure.description}${failure.location.fold("")(_.description)}")
    .mkString("\n")
  s"Error parsing configuration:\n$messages"
})

object AppConfig {

  implicit val awsReader: ConfigReader[AwsConfig] = ConfigReader.fromCursor[AwsConfig] { cur =>
    Either.catchNonFatal(new AwsConfig(cur.value.atKey("aws").getConfig("aws")))
      .leftMap(exc => ConfigReaderFailures(ThrowableFailure(exc, cur.location)))
  }

  def apply[F[_] : Sync: Logger](rawConfig: => Config): F[AppConfig] = {
    import pureconfig.generic.auto._

    for {
      _      <- Logger[F].info("Loading configuration ...")
      config <- Sync[F].fromEither(loadConfig[AppConfig](rawConfig))
      _      <- Logger[F].info(rawConfig.root().render)

    } yield {
      config
    }
  }

  private def loadConfig[T: TypeTag](config: => Config)(implicit reader: Derivation[ConfigReader[T]]): Either[Throwable, T] = {
    for {
      rawConfig    <- Either.catchNonFatal(config)
      loadedConfig <- pureconfig.loadConfig[T](rawConfig).leftMap(PureConfigException)
    } yield loadedConfig
  }
}
