package com.ad.mpc.p10n.realtimeuserprofile

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import cats.implicits._
import com.ad.mpc.p10n.realtimeuserprofile.framework.KafkaStreamsHealthService
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object RealtimeUserProfileServer {

  def stream[F[_]: ConcurrentEffect](healthService: KafkaStreamsHealthService[F])(implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {

    for {
      client <- BlazeClientBuilder[F](global).stream

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = RealtimeUserProfileRoutes.healthRoutes[F](healthService).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
