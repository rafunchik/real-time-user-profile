package com.ad.mpc.p10n.realtimeuserprofile.framework.config

import cats.Monad
import com.typesafe.config.{Config, ConfigFactory}

trait ConfigLoader[F[_]] {
  def load: F[Config]
}

object ConfigLoader {
  def apply[F[_]](implicit configLoader: ConfigLoader[F]): ConfigLoader[F] = configLoader
}

class DefaultConfigLoader[F[_] : Monad] extends ConfigLoader[F] {
  override def load: F[Config] = Monad[F].pure(ConfigFactory.load())
}
