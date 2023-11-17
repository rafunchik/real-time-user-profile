package com.ad.mpc.p10n.realtimeuserprofile

import cats.effect.IO
import com.ad.mpc.p10n.realtimeuserprofile.framework.KafkaStreamsHealthService
import org.apache.kafka.streams.KafkaStreams
import org.http4s._
import org.http4s.implicits._
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when


class RealtimeUserProfileRoutesSpec extends FlatSpec with Matchers with MockitoSugar {

  behavior of RealtimeUserProfileRoutes.getClass.getSimpleName

  it should "return 200 if streams are rebalancing" in {
    withRunningStatus(KafkaStreams.State.REBALANCING).status shouldBe Status.Ok
  }

  it should "return 500 upon exception checking the streams state" in {
    withExceptionRunningStatus(new NullPointerException("error getting streams state")).status shouldBe Status.InternalServerError
  }

  private[this] def withRunningStatus(status: KafkaStreams.State): Response[IO] = {
    val getHealth = Request[IO](Method.GET, uri"/health")
    val mockStreams = mock[KafkaStreams]

    val healthService = new KafkaStreamsHealthService[IO](new KafkaStreamsState(mockStreams))

    when(mockStreams.state()).thenReturn(status)

    RealtimeUserProfileRoutes.healthRoutes(healthService).orNotFound(getHealth).unsafeRunSync()
  }

  private[this] def withExceptionRunningStatus(exception: Exception): Response[IO] = {
    val getHealth = Request[IO](Method.GET, uri"/health")
    val mockStreams = mock[KafkaStreams]

    val healthService = new KafkaStreamsHealthService[IO](new KafkaStreamsState(mockStreams))

    when(mockStreams.state()).thenThrow(exception)

    RealtimeUserProfileRoutes.healthRoutes(healthService).orNotFound(getHealth).unsafeRunSync()
  }
}
