package com.ad.mpc.p10n.realtimeuserprofile

import cats.effect.{Clock, ConcurrentEffect, ContextShift, Effect, ExitCode, IO, IOApp, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.ad.mpc.p10n.realtimeuserprofile.framework.{IOClock, KafkaStreamsHealthService}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.prometheus.client.CollectorRegistry
import com.ad.mpc.p10n.realtimeuserprofile.framework.config.{AppConfig, ConfigLoader, DefaultConfigLoader}
import com.ad.mpc.p10n.realtimeuserprofile.framework.metrics.{DefaultMetricsInit, DefaultMetricsRegistry, MetricsInit, MetricsRegistry}
import com.ad.mpc.p10n.realtimeuserprofile.framework.secrets.AppSecrets
import com.ad.mpc.p10n.realtimeuserprofile.topology.{UserInteractionsTopology, StreamsTopology}
import com.schibsted.mp.p10n.aws.secrets.program.SecretsManager
import com.schibsted.mp.p10n.kafka.streams.KafkaStreamsState


object StreamsApp extends IOApp {

  private implicit val configLoader: ConfigLoader[IO] = new DefaultConfigLoader[IO]
  private implicit val clock: Clock[IO] = new IOClock
  private implicit val metricsRegistry: MetricsRegistry[IO] = new DefaultMetricsRegistry[IO](CollectorRegistry.defaultRegistry)

  def run(args: List[String]): IO[ExitCode] = runApp[IO]

  private def runApp[F[_] : Effect : ConfigLoader : MetricsRegistry : Clock : ConcurrentEffect : Timer : ContextShift]: F[ExitCode] = {
    implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

    for {
      raw        <- ConfigLoader[F].load
      appConfig  <- AppConfig(raw)
      state      <- init(appConfig)
      exitCode   <- initHttpServer(new KafkaStreamsHealthService[F](state))
      _          <- logger.info("Application running ...")
    } yield exitCode
  }

  private def initHttpServer[F[_] : Effect : ConfigLoader : MetricsRegistry : Clock : ConcurrentEffect : Timer :
    ContextShift](healthService: KafkaStreamsHealthService[F]) = {
      RealtimeUserProfileServer.stream[F](healthService).compile.drain.as(ExitCode.Success)
  }

  private def init[F[_] : Effect : MetricsRegistry: Clock](appConfig: AppConfig): F[KafkaStreamsState] = {
    implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]
    implicit val secretsManager: SecretsManager[F] = SecretsManager.sdk[F](appConfig.aws)
    implicit val userInteractionTopology: StreamsTopology[F] = new UserInteractionsTopology[F]
    implicit val streamsRunner: StreamsRunner[F] = new DefaultStreamsRunner[F]
    implicit val metricsInit: MetricsInit[F] = new DefaultMetricsInit[F]

    for {
      _           <- logger.info("Initialising secrets ...")
      _           <- MetricsInit[F].start(appConfig.metrics)
      appSecrets  <- Sync[F].delay(AppSecrets[F](appConfig.secrets))
      state       <- StreamsRunner[F].run(appConfig.app, appConfig.kafkaStreams, appSecrets)
    } yield state
  }
}

