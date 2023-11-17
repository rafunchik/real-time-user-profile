package com.ad.mpc.p10n.realtimeuserprofile.framework.config

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest.{FlatSpec, Inside, Matchers}

class AppConfigSpec extends FlatSpec with Matchers with Inside {

  behavior of AppConfig.getClass.getName

  import AppConfigSpec.logger

  it should "load correctly" in {
    inside(AppConfig[IO](ConfigFactory.load()).attempt.unsafeRunSync()) {
      case Right(_) =>
    }
  }

  it should "fail to load when loading the config throws a runtime exception" in {
    inside(AppConfig[IO](throw new RuntimeException("load-failed")).attempt.unsafeRunSync()) {
      case Left(exc: RuntimeException) =>
        exc.getMessage shouldBe "load-failed"
    }
  }

  it should "fail to load when the aws config section doesn't exist" in {
    val config = ConfigFactory.load().withoutPath("aws")
    inside(AppConfig[IO](config).attempt.unsafeRunSync()) {
      case Left(_: PureConfigException) =>
    }
  }

  it should "fail to load when the metrics config section doesn't exist" in {
    val config = ConfigFactory.load().withoutPath("metrics")
    inside(AppConfig[IO](config).attempt.unsafeRunSync()) {
      case Left(_: PureConfigException) =>
    }
  }

  it should "fail to load when the secrets config section doesn't exist" in {
    val config = ConfigFactory.load().withoutPath("secrets")
    inside(AppConfig[IO](config).attempt.unsafeRunSync()) {
      case Left(_: PureConfigException) =>
    }
  }

  it should "fail to load when the app config section doesn't exist" in {
    val config = ConfigFactory.load().withoutPath("app")
    inside(AppConfig[IO](config).attempt.unsafeRunSync()) {
      case Left(_: PureConfigException) =>
    }
  }

  it should "fail to load when the kafka-streams config section doesn't exist" in {
    val config = ConfigFactory.load().withoutPath("kafka-streams")
    inside(AppConfig[IO](config).attempt.unsafeRunSync()) {
      case Left(_: PureConfigException) =>
    }
  }
}

object AppConfigSpec {
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
}
