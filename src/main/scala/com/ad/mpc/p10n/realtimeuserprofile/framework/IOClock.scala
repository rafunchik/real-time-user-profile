package com.ad.mpc.p10n.realtimeuserprofile.framework

import cats.effect.{Clock, IO}

import scala.concurrent.duration.{MILLISECONDS, NANOSECONDS, TimeUnit}

class IOClock extends Clock[IO] {

  final def realTime(unit: TimeUnit): IO[Long] =
    IO(unit.convert(System.currentTimeMillis(), MILLISECONDS))

  final def monotonic(unit: TimeUnit): IO[Long] =
    IO(unit.convert(System.nanoTime(), NANOSECONDS))

}
