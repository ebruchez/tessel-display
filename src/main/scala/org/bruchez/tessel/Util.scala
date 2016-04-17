package org.bruchez.tessel

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.timers._

object Util {

  def delay(delay: FiniteDuration): Future[Unit] = {
    val p = Promise[Unit]()
    setTimeout(delay) {
      println(s"  - done with delay $delay")
      p.success(())
    }
    p.future
  }

}
