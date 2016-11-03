package org.bruchez.tessel

import scala.async.Async._
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object HT16K33 {

  val port = Tessel.port.B

  val DisplayI2CAddress = 0x70
  val i2c  =  port.I2C(DisplayI2CAddress)

  val SystemSetCmd          = 0x20

  val SystemSetStandbyBits  = 0x00
  val SystemSetNormalBits   = 0x01

  val DisplaySetCmd         = 0x80

  val DisplayOnBit          = 0x01
  val DisplayBlinking2Hz    = 0x01 << 1
  val DisplayBlinking1Hz    = 0x02 << 1
  val DisplayBlinkingHalfHz = 0x03 << 1

  val DisplayDimmingCmd     = 0xE0

  val DisplayDataCmd        = 0x00

  def initialize(): Future[Unit] = async {

    val Smiley = List(
      0x3c,
      0x42,
      0xa5,
      0x81,
      0xa5,
      0x99,
      0x42,
      0x3c
    )

    await(i2c.sendF(new Buffer(js.Array[Int](HT16K33.SystemSetCmd      | HT16K33.SystemSetNormalBits))))
    await(i2c.sendF(new Buffer(js.Array[Int](HT16K33.DisplayDimmingCmd | 0x01))))
    await(i2c.sendF(new Buffer(js.Array[Int](HT16K33.DisplaySetCmd     | HT16K33.DisplayOnBit))))
    await(i2c.sendF(new Buffer(js.Array[Int](HT16K33.DisplayDataCmd) ++ Smiley.iterator flatMap (v ⇒ Iterator(0x00, v)))))
  }
}