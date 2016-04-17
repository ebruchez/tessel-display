package org.bruchez.tessel

import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.timers.SetIntervalHandle

object LEDBlinker {

  val tessel = Tessel()

  private var ledOn = false
  private var handle: Option[SetIntervalHandle] = None

  def start(): Unit = {
    if (handle.isEmpty)
      handle = Some(
        js.timers.setInterval(1.second) {
          if (ledOn)
            tessel.led(3).off()
          else
            tessel.led(3).on()

          ledOn = ! ledOn
        }
      )
  }

  def stop(): Unit = {
    handle foreach js.timers.clearInterval
    handle = None
  }
}