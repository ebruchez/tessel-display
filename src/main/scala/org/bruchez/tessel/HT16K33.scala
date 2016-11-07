package org.bruchez.tessel

import scala.async.Async._
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object HT16K33 {

  val port = Tessel.port.B

  val DisplayI2CAddress     = 0x70
  val i2c                   =  port.I2C(DisplayI2CAddress)

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
    await(i2c.sendF(new Buffer(js.Array[Int](HT16K33.SystemSetCmd      | HT16K33.SystemSetNormalBits))))
    await(i2c.sendF(new Buffer(js.Array[Int](HT16K33.DisplayDimmingCmd | 0x01))))
    await(i2c.sendF(new Buffer(js.Array[Int](HT16K33.DisplaySetCmd     | HT16K33.DisplayOnBit))))
  }

  val Smile = js.Array(
    0x3c,
    0x42,
    0xa5,
    0x81,
    0xa5,
    0x99,
    0x42,
    0x3c
  )

  val Frown = js.Array(
    0x3c,
    0x42,
    0xa5,
    0x81,
    0x99,
    0xa5,
    0x42,
    0x3c
  )

  val Neutral = js.Array(
    0x3c,
    0x42,
    0xa5,
    0x81,
    0xbd,
    0x81,
    0x42,
    0x3c
  )

  private val displayBuffer = new Buffer(new js.Array[Int](8))

  def clear(): Unit =
    for (i ← 0 until displayBuffer.length)
      displayBuffer(i) = 0

  def  drawPixel(_x: Int, _y: Int, color: Int): Unit = {

    if ((_y < 0) || (_y >= 8)) return
    if ((_x < 0) || (_x >= 8)) return

    var x = _x
    var y = _y

    def swapXY() = {
      val oldX = x
      x = y
      y = oldX
    }

    val rotation = 1

    rotation match {
      case 1 ⇒
        swapXY()
        x = 8 - x - 1
      case 2 ⇒
        x = 8 - x - 1
        y = 8 - y - 1
      case 3 ⇒
        swapXY()
        y = 8 - y - 1
      case _ ⇒
    }

    x += 7
    x %= 8

    if (color != 0) {
      displayBuffer(y) |= 1 << x
    } else {
      displayBuffer(y) &= ~(1 << x)
    }
  }

  def drawBitmap(x: Int, y: Int, bitmap: js.Array[Int], w: Int, h: Int, color: Int) = {

      val byteWidth = (w + 7) / 8
      var byte = 0

      for (j ← 0 until h) {
        for (i ← 0 until w) {

          byte =
            if ((i & 7) != 0)
              byte << 1
            else
              bitmap(j * byteWidth + i / 8)

          if ((byte & 0x80) != 0) {

            drawPixel(x + i, y + j, color)
          }
        }
      }
    }

  def sendFrame(): Future[Unit] = {

    val sendBuffer = new Buffer(new js.Array[Int](displayBuffer.length * 2 + 1))

    sendBuffer(0) = DisplayDataCmd
    var j = 1
    for (i ← 0 until displayBuffer.length) {
      sendBuffer(j)     = displayBuffer(i)
      sendBuffer(j + 1) = 0
      j += 2
    }

    i2c.sendF(sendBuffer)
  }
}