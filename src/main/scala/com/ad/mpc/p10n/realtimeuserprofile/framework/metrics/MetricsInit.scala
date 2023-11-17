package com.ad.mpc.p10n.realtimeuserprofile.framework.metrics

import java.time.Duration

import cats.effect.Sync
import cats.syntax.functor._
import com.github.chris_zen.prometheus.bridge.datadog.{DatadogBridge, DatadogBridgeConfig}
import io.prometheus.client.hotspot._
import org.slf4j.{Logger, LoggerFactory}


trait MetricsInit[F[_]] {

  def start(metricsConfig: MetricsConfig): F[Unit]
}

object MetricsInit {
  def apply[F[_]](implicit metrics: MetricsInit[F]): MetricsInit[F] = metrics
}

class DefaultMetricsInit[F[_] : Sync : MetricsRegistry] extends MetricsInit[F] {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getName.replace("$", ""))

  override def start(metricsConfig: MetricsConfig): F[Unit] = {
    startDatadogBridge(metricsConfig.datadog)
      .map(bridge => sys.addShutdownHook(bridge.stop()))
  }

  private[metrics] def startDatadogBridge(datadogConfig: MetricsDatadogConfig): F[DatadogBridge] = Sync[F].delay {
    logger.info("Initialising the Prometheus Datadog bridge ...")
    // $COVERAGE-OFF$Disabling test coverage as the following code is hardly testable
    val registry = MetricsRegistry[F].registry

    val bridgeConfig = DatadogBridgeConfig()
      .withRegistry(registry)
      .withPrefix(datadogConfig.prefix)
      .withPeriod(Duration.ofMillis(datadogConfig.period.toMillis))

    logger.info(s"DatadogBridge configuration: $bridgeConfig")

    new StandardExports().register(registry)
    new MemoryPoolsExports().register(registry)
    new MemoryAllocationExports().register(registry)
    new BufferPoolsExports().register(registry)
    new GarbageCollectorExports().register(registry)
    new ThreadExports().register(registry)
    new ClassLoadingExports().register(registry)
    new VersionInfoExports().register(registry)

    DatadogBridge(bridgeConfig)
    // $COVERAGE-ON$
  }
}

