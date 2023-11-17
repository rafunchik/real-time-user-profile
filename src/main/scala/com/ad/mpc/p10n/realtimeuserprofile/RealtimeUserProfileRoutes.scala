package com.ad.mpc.p10n.realtimeuserprofile

import cats.effect.Sync
import cats.implicits._
import com.ad.mpc.p10n.realtimeuserprofile.framework.HealthService
import com.ad.mpc.p10n.realtimeuserprofile.framework.HealthService.{Healthy, Unhealthy}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl


object RealtimeUserProfileRoutes {

  def healthRoutes[F[_]: Sync](healthService: HealthService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    case class HealthStatus(status: String)

    val Alive = HealthStatus(status = "alive").asJson
    val Down = HealthStatus(status = "realtimeprofiles-down").asJson

    HttpRoutes.of[F] {
      case GET -> Root / "health" => healthService.checkHealth().flatMap {
        case Healthy   => Ok(Alive)
        case Unhealthy => InternalServerError(Down)
      }
    }
  }
}
