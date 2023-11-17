package com.ad.mpc.p10n.realtimeuserprofile.framework

import cats.effect.Sync
import com.ad.mpc.p10n.realtimeuserprofile.framework.HealthService.{HealthStatus, Healthy, Unhealthy}
import cats.syntax.applicativeError._
import com.schibsted.mp.p10n.kafka.streams.KafkaStreamsState

object HealthService {
  sealed trait HealthStatus

  final case object Healthy extends HealthStatus
  final case object Unhealthy extends HealthStatus
}

trait HealthService[F[_]] {
  def checkHealth(): F[HealthStatus]
}

class KafkaStreamsHealthService[F[_]: Sync](state: KafkaStreamsState) extends HealthService[F] {

  def checkHealth(): F[HealthStatus] = {
    Sync[F].delay[HealthStatus] {
      if (state.isAlive) {
        Healthy
      } else {
        Unhealthy
      }
    }.handleErrorWith(_ => Sync[F].pure(Unhealthy))
  }
}
