package org.bruchez.tessel

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.Uint8Array
import scalaxy.streams.optimize

// A lot of the following is converted from:
// https://github.com/adafruit/Adafruit-ST7735-Library/blob/master/Adafruit_ST7735.cpp
//
// Original license and copyright:
//
//     This is a library for the Adafruit 1.8" SPI display.
//   This library works with the Adafruit 1.8" TFT Breakout w/SD card
//     ----> http://www.adafruit.com/products/358
//   The 1.8" TFT shield
//     ----> https://www.adafruit.com/product/802
//   The 1.44" TFT breakout
//     ----> https://www.adafruit.com/product/2088
//   as well as Adafruit raw 1.8" TFT display
//     ----> http://www.adafruit.com/products/618
//     Check out the links above for our tutorials and wiring diagrams
//     These displays use SPI to communicate, 4 or 5 pins are required to
//     interface (RST is optional)
//     Adafruit invests time and resources providing this open source code,
//     please support Adafruit and open-source hardware by purchasing
//     products from Adafruit!
//     Written by Limor Fried/Ladyada for Adafruit Industries.
//     MIT license, all text above must be included in any redistribution
//
object ST7735 {

  val displayPort            = Tessel.port.A

  private val SpiFrequency   = 6 * 1000 * 1000 // managed to make it work up to 6 MHz

  private val chipSelectPin  = displayPort.pin(5)
  private val resetPin       = displayPort.pin(6)
  private val dataCommandPin = displayPort.pin(7)

  private val spi            = displayPort.SPI(SpiFrequency, 0, 0, chipSelectPin) // SPI mode 0

  private val TFTWidth     = 128
  private val TFTHeight144 = 128 // for 1.44" display
  private val TFTHeight18  = 160 // for 1.8" display

  private val ColStart = 0//2
  private val RowStart = 0//1

  val ScreenWidth  = TFTWidth
  val ScreenHeight = TFTHeight18

  private case class SPICommand(cmd: CommandCode, args: js.Array[Int], delay: FiniteDuration = 0.nanos)

  private sealed trait CommandCode { def code: Int }

  private object CommandCode {
    case object NOP     extends { val code = 0x00 } with CommandCode
    case object SWRESET extends { val code = 0x01 } with CommandCode
    case object RDDID   extends { val code = 0x04 } with CommandCode
    case object RDDST   extends { val code = 0x09 } with CommandCode

    case object SLPIN   extends { val code = 0x10 } with CommandCode
    case object SLPOUT  extends { val code = 0x11 } with CommandCode
    case object PTLON   extends { val code = 0x12 } with CommandCode
    case object NORON   extends { val code = 0x13 } with CommandCode

    case object INVOFF  extends { val code = 0x20 } with CommandCode
    case object INVON   extends { val code = 0x21 } with CommandCode
    case object DISPOFF extends { val code = 0x28 } with CommandCode
    case object DISPON  extends { val code = 0x29 } with CommandCode
    case object CASET   extends { val code = 0x2A } with CommandCode
    case object RASET   extends { val code = 0x2B } with CommandCode
    case object RAMWR   extends { val code = 0x2C } with CommandCode
    case object RAMRD   extends { val code = 0x2E } with CommandCode

    case object PTLAR   extends { val code = 0x30 } with CommandCode
    case object COLMOD  extends { val code = 0x3A } with CommandCode
    case object MADCTL  extends { val code = 0x36 } with CommandCode

    case object FRMCTR1 extends { val code = 0xB1 } with CommandCode
    case object FRMCTR2 extends { val code = 0xB2 } with CommandCode
    case object FRMCTR3 extends { val code = 0xB3 } with CommandCode
    case object INVCTR  extends { val code = 0xB4 } with CommandCode
    case object DISSET5 extends { val code = 0xB6 } with CommandCode

    case object PWCTR1  extends { val code = 0xC0 } with CommandCode
    case object PWCTR2  extends { val code = 0xC1 } with CommandCode
    case object PWCTR3  extends { val code = 0xC2 } with CommandCode
    case object PWCTR4  extends { val code = 0xC3 } with CommandCode
    case object PWCTR5  extends { val code = 0xC4 } with CommandCode
    case object VMCTR1  extends { val code = 0xC5 } with CommandCode

    case object RDID1   extends { val code = 0xDA } with CommandCode
    case object RDID2   extends { val code = 0xDB } with CommandCode
    case object RDID3   extends { val code = 0xDC } with CommandCode
    case object RDID4   extends { val code = 0xDD } with CommandCode

    case object PWCTR6  extends { val code = 0xFC } with CommandCode

    case object GMCTRP1 extends { val code = 0xE0 } with CommandCode
    case object GMCTRN1 extends { val code = 0xE1 } with CommandCode
  }

  case class Color(underlying: Int) extends AnyVal
  object Color {
    val Black   = Color(rgbTo16(0x00, 0x00, 0x00))
    val Blue    = Color(rgbTo16(0x00, 0x00, 0xff))
    val Red     = Color(rgbTo16(0xff, 0x00, 0x00))
    val Green   = Color(rgbTo16(0x00, 0xff, 0x00))
    val Cyan    = Color(rgbTo16(0x00, 0xff, 0xff))
    val Magenta = Color(rgbTo16(0xff, 0x00, 0xff))
    val Yellow  = Color(rgbTo16(0xff, 0xff, 0x00))
    val White   = Color(rgbTo16(0xff, 0xff, 0xff))
  }

  private val InitializationCommands = List(
    SPICommand(CommandCode.SWRESET, js.Array(), delay = 150.millis), //  1: Software reset
    SPICommand(CommandCode.SLPOUT , js.Array(), delay = 500.millis), //  2: Out of sleep mode
    SPICommand(CommandCode.FRMCTR1, js.Array(                        //  3: Frame rate ctrl - normal mode
      0x01, 0x2C, 0x2D                                               //     Rate = fosc/(1x2+40) * (LINE+2C+2D)
    )),
    SPICommand(CommandCode.FRMCTR2, js.Array(                        //  4: Frame rate control - idle mode
      0x01, 0x2C, 0x2D                                               //     Rate = fosc/(1x2+40) * (LINE+2C+2D)
    )),
    SPICommand(CommandCode.FRMCTR3, js.Array(                        //  5: Frame rate ctrl - partial mode
      0x01, 0x2C, 0x2D,                                              //     Dot inversion mode
      0x01, 0x2C, 0x2D                                               //     Line inversion mode
    )),
    SPICommand(CommandCode.INVCTR , js.Array(                        //  6: Display inversion ctrl
      0x07                                                           //     No inversion
    )),
    SPICommand(CommandCode.PWCTR1 , js.Array(                        //  7: Power control
      0xA2,
      0x02,                                                          //     -4.6V
      0x84                                                           //     AUTO mode
    )),
    SPICommand(CommandCode.PWCTR2 , js.Array(                        //  8: Power control
      0xC5                                                           //     VGH25 = 2.4C VGSEL = -10 VGH = 3 * AVDD
    )),
    SPICommand(CommandCode.PWCTR3 , js.Array(                        //  9: Power control
      0x0A,                                                          //     Opamp current small
      0x00                                                           //     Boost frequency
    )),
    SPICommand(CommandCode.PWCTR4 , js.Array(                        // 10: Power control
      0x8A,                                                          //     BCLK/2, Opamp current small & Medium low
      0x2A
    )),
    SPICommand(CommandCode.PWCTR5 , js.Array(                        // 11: Power control
      0x8A, 0xEE
    )),
    SPICommand(CommandCode.VMCTR1 , js.Array(                        // 12: Power control
      0x0E
    )),
    SPICommand(CommandCode.INVOFF , js.Array()),                     // 13: Don't invert display
    SPICommand(CommandCode.MADCTL , js.Array(                        // 14: Memory access control (directions)
      0xC8                                                           //     row addr/col addr, bottom to top refresh
    )),
    SPICommand(CommandCode.COLMOD , js.Array(                        // 15: set color mode
      0x05                                                           //     16-bit color
    )),
    // Init for 7735R, part 2 (green tab only)
    SPICommand(CommandCode.CASET,   js.Array(                        //  1: Column addr set
      0x00, 0x02,                                                    //     XSTART = 0
      0x00, 0x7F+0x02                                                //     XEND = 127
    )),
    SPICommand(CommandCode.RASET,   js.Array(                        //  2: Row addr set
      0x00, 0x01,                                                    //     XSTART = 0
      0x00, 0x9F+0x01                                                //     XEND = 159
    )),
    // Init for 7735R, part 3 (red or green tab)
    SPICommand(CommandCode.GMCTRP1, js.Array(                        //  1: Magical unicorn dust, 16 args, no delay:
      0x02, 0x1c, 0x07, 0x12,
      0x37, 0x32, 0x29, 0x2d,
      0x29, 0x25, 0x2B, 0x39,
      0x00, 0x01, 0x03, 0x10
    )),
    SPICommand(CommandCode.GMCTRN1, js.Array(                        //  2: Sparkles and rainbows, 16 args, no delay:
      0x03, 0x1d, 0x07, 0x06,
      0x2E, 0x2C, 0x29, 0x2D,
      0x2E, 0x2E, 0x37, 0x3F,
      0x00, 0x00, 0x02, 0x10
    )),
    SPICommand(CommandCode.NORON,   js.Array(), 10.millis),          //  3: Normal display on, no args, w/delay
    SPICommand(CommandCode.DISPON,  js.Array(), 100.millis)          //  4: Main screen turn on, no args w/delay
  )

  def writeCommandF(cmd: SPICommand): Future[Unit] = async {
//    println(s"  - about to run command ${cmd.cmd.code}")
    dataCommandPin.low() // -D/CX=’0’: command data.
    await(spi.sendF(new Buffer(js.Array(cmd.cmd.code))))
    if (cmd.args.nonEmpty)
      await(writeDataF(cmd.args.toJSArray))
  }

  def writeDataF(data: js.Array[Int]): Future[Unit] =
      writeDataF(new Buffer(data))

  def writeDataF(data: Buffer): Future[Unit] = async {
    dataCommandPin.high() // -D/CX=’1’: display data or parameter
    await(spi.sendF(data))
  }

  def fillScreenF(color: Color) =
    fillRectF(0, 0,  ScreenWidth, ScreenHeight, color)

  def fillRectF(x: Int, y: Int, w: Int, h: Int, color: Color) = async {

    // rudimentary clipping (drawChar w/big text requires this)
    if(! (x >= ScreenWidth) || (y >= ScreenHeight)) {

      val clippedW = if ((x + w - 1) >= ScreenWidth)  ScreenWidth  - x else w
      val clippedH = if ((y + h - 1) >= ScreenHeight) ScreenHeight - y else h

      await(setAddrWindowF(x, y, x + clippedW - 1, y + clippedH - 1))

      val hi = color.underlying >> 8
      val lo = color.underlying & 0xff

      val totalPixels = clippedH * clippedW

      // TODO: Tessel SPI API only handles 255 bytes buffers anyway, so use 255.
      val MaxBufferSize = 10 * 1024

      val fullBuffers     = totalPixels / MaxBufferSize
      val remainingPixels = totalPixels % MaxBufferSize

      println(s"  - about to write $fullBuffers full buffers and partial $remainingPixels")

      val jsArray = {
        val reusableFullBufferSize = if (fullBuffers > 0) MaxBufferSize else remainingPixels
        val fullBuffer = Array.ofDim[Int](reusableFullBufferSize * 2)
        for (p ← 0 until (reusableFullBufferSize * 2, 2)) {
          fullBuffer(p)     = hi
          fullBuffer(p + 1) = lo
        }
        fullBuffer.toJSArray
      }

      optimize {
        for (_ ← 1 to fullBuffers)
          await(writeDataF(jsArray))
      }

      if (remainingPixels > 0)
        await(writeDataF(jsArray.take(remainingPixels * 2)))
    }
  }

  def setAddrWindowF(x0: Int, y0: Int, x1: Int, y1: Int) = async {

    val commands = List(
      SPICommand(CommandCode.CASET, js.Array(  // Column addr set
        0x00,
        x0 + ColStart,                         // XSTART
        0x00,
        x1 + ColStart                          // XEND
      )),
      SPICommand(CommandCode.RASET, js.Array(  // Row addr set
        0x00,
        y0 + RowStart,                         // YSTART
        0x00,
        y1 + RowStart                          // YEND
      )),
      SPICommand(CommandCode.RAMWR, js.Array())// write to RAM)
    )

    val it = commands.iterator
    while (it.hasNext)
      await(writeCommandF(it.next()))
  }

  def initialize() = async {

    displayPort.pin(5).high()
    dataCommandPin.low()
    resetPin.high()

    Util.delay(500.millis)
    resetPin.low()
    Util.delay(500.millis)
    resetPin.high()
    Util.delay(500.millis)

    val it = InitializationCommands.iterator
    while (it.hasNext) {
      val cmd = it.next()
      await(writeCommandF(cmd))
      if (cmd.delay.toNanos > 0)
        Util.delay(cmd.delay)
    }
    await(fillScreenF(Color.Black))
  }

  def rgbTo16(r: Int, g: Int, b: Int) =
    ((b & 0xf8) << 8) | ((g & 0xfc) << 3) | (r >> 3)

  private val MaxSpiBufferSize = 254 // actually 255 but we write 16-bit values so don't split pixels

  def sendFrame(data: Uint8Array, width: Int, height: Int): Future[Unit] = async {

//    println(s"  - sending frame, width = $width, height= $height")

    val totalPixels     = width * height
    val pixelsPerBuffer = MaxSpiBufferSize / 2
    val fullBuffers     = totalPixels / pixelsPerBuffer
    val remainingPixels = totalPixels % pixelsPerBuffer

    val outputBuffer = new Buffer(new js.Array[Int](MaxSpiBufferSize))

    await(setAddrWindowF(0, 0, width, height))

    var inputIndex = 0

    def fillBuffer(index: Int) = {
      val rgb16 = rgbTo16(data(inputIndex), data(inputIndex + 1), data(inputIndex + 2))

      outputBuffer(index)     = rgb16 >> 8
      outputBuffer(index + 1) = rgb16

      inputIndex += 4
    }

    optimize {
      for (_ ← 1 to fullBuffers) {
        for (i ← 0 until (MaxSpiBufferSize, 2))
          fillBuffer(i)

        await(writeDataF(outputBuffer))
      }
    }

    optimize {
      for (i ← 0 until (remainingPixels, 2))
        fillBuffer(i)

      await(writeDataF(outputBuffer.slice(0, remainingPixels * 2)))
    }
  }
}