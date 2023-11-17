package com.ad.mpc.p10n.realtimeuserprofile.framework.metrics

import io.prometheus.client.CollectorRegistry

trait MetricsRegistry[F[_]] {

  def registry: CollectorRegistry
}

object MetricsRegistry {
  def apply[F[_]](implicit metricsRegistry: MetricsRegistry[F]): MetricsRegistry[F] = metricsRegistry
}

object DefaultMetricsRegistry {
  def apply[F[_]]: DefaultMetricsRegistry[F] = {
    new DefaultMetricsRegistry(new CollectorRegistry)
  }
}

class DefaultMetricsRegistry[F[_]](collectorRegistry: CollectorRegistry) extends MetricsRegistry[F] {
  def registry: CollectorRegistry = collectorRegistry
}
