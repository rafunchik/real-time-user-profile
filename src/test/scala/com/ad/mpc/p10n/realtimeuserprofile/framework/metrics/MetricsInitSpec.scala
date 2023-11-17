package com.ad.mpc.p10n.realtimeuserprofile.framework.metrics

import cats.effect.IO
import com.github.chris_zen.prometheus.bridge.datadog.DatadogBridge
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{spy, when}
import org.scalatest.{FlatSpec, Inside, Matchers}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration._

class MetricsInitSpec extends FlatSpec with Inside with MetricsModuleSpecFixture {

  behavior of MetricsInit.getClass.getSimpleName

  it should "start metrics and return Stream" in {
    withMetricsInit { metricsInit =>
      val dataDog: DatadogBridge = mock[DatadogBridge]
      when(metricsInit.startDatadogBridge(any())).thenReturn(IO.pure(dataDog))
      metricsInit.start(config).unsafeRunSync()
    }
  }
}

trait MetricsModuleSpecFixture extends MockitoSugar with Matchers {

  val config: MetricsConfig = MetricsConfig(MetricsDatadogConfig(prefix = "test-metrics", 1 second))

  def withMetricsInit(test: DefaultMetricsInit[IO] => Unit): Unit = {
    implicit val metricsRegistry: MetricsRegistry[IO] = DefaultMetricsRegistry[IO]
    test(spy(new DefaultMetricsInit[IO]))
  }

}
