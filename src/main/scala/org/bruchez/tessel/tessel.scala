package org.bruchez.tessel

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global ⇒ g}
import scala.scalajs.js.annotation.JSImport

/**
 * Facades for a few Tessel 2 APIs.
 */

// "tessel"
// https://github.com/tessel/t2-firmware/blob/master/node/tessel-export.js

@js.native
trait Led extends js.Object {
  def on(): Unit = js.native
  def off(): Unit = js.native
}

@js.native
trait Pin extends js.Object with EventEmitter {
  def output(value: Int): Unit = js.native
  def read(callback: js.Function2[Error, Int, _]): Unit = js.native

  def low() : Unit = js.native
  def high(): Unit = js.native

  def analogWrite(value: Double): js.native
  def analogRead(callback: js.Function2[Error, Double, _]): Unit = js.native
}

@js.native
trait PWMPin extends js.Object with EventEmitter {
  def pwmDutyCycle(value: Double): js.native
}

@js.native
trait I2C extends js.Object {
  def transfer(buffer: Buffer, callback: js.Function2[Error, Buffer, _] = null): js.native
  def send(buffer: Buffer, callback: js.Function2[Error, Buffer, _] = null): js.native
}

object I2C {
  implicit class I2COps(val i2c: I2C) extends AnyVal {
    def sendF(data: Buffer): Future[Unit] = {
      val p = Promise[Unit]()
      i2c.send(data, (err: Error, rx: Buffer) ⇒ {
        if (! js.isUndefined(err) && (err ne null)) {
          println(s"failure $err")
          p.failure(new RuntimeException(err.message))
        } else {
//          println(s"success")
          p.success(())
        }
      })
      p.future
    }
  }
}

@js.native
trait SPI extends js.Object {
  def transfer(buffer: Buffer, callback: js.Function2[Error, Buffer, _] = null): js.native
  def send(buffer: Buffer, callback: js.Function2[Error, Buffer, _] = null): js.native
}

object SPI {
  implicit class I2COps(val spi: SPI) extends AnyVal {
    def sendF(data: Buffer): Future[Unit] = {
      val p = Promise[Unit]()
      spi.send(data, (err: Error, rx: Buffer) ⇒ {
        if (! js.isUndefined(err) && (err ne null)) {
          println(s"failure $err")
          p.failure(new RuntimeException(err.message))
        } else {
//          println(s"success")
          p.success(())
        }
      })
      p.future
    }
  }
}

@js.native
trait Port extends js.Object {
  def pin: js.Array[Pin] = js.native
  def pwm: js.Array[PWMPin] = js.native

  // This doesn't compile, see https://github.com/scala-js/scala-js/issues/2398
//  @js.native
//  class I2C(address: Int) extends js.Object
}

object Port {
  implicit class PortOps(val port: Port) extends AnyVal {
    def I2C(address: Int) =
      js.Dynamic.newInstance(port.asInstanceOf[js.Dynamic].I2C)(address).asInstanceOf[I2C]

    def SPI(clockSpeed: Int, cpol: Int, cpha: Int, chipSelect: Pin = port.pin(5)) =
      js.Dynamic.newInstance(port.asInstanceOf[js.Dynamic].SPI)(
        js.Dynamic.literal(
          clockSpeed = clockSpeed,
          cpol       = cpol,
          cpha       = cpha,
          chipSelect = chipSelect
        )
      ).asInstanceOf[SPI]
  }
}

@js.native
trait Ports extends js.Object {
  def A: Port = js.native
  def B: Port = js.native
}

//@js.native
//trait Tessel extends js.Object {
//  def led: js.Array[Led] = js.native
//  def port: Ports = js.native
//
//  // pwmFrequency is capable of frequencies from 1Hz to 5kHz
//  def pwmFrequency(value: Int): js.native
//}

@js.native
@JSImport("tessel", JSImport.Namespace)
object Tessel extends js.Object {

  def led: js.Array[Led] = js.native
  def port: Ports        = js.native

  // pwmFrequency is capable of frequencies from 1Hz to 5kHz
//  def pwmFrequency(value: Int): js.native
}

// Anything that can be on or off
sealed trait OnOff { val name: String; val value: Boolean }
case object On  extends { val name = "on" ; val value = true  } with OnOff
case object Off extends { val name = "off"; val value = false } with OnOff

object OnOff {
  def fromBoolean(value: Boolean) = if (value) On else Off
}

// "relay-mono"
// https://github.com/tessel/relay-mono/blob/master/index.js

@js.native
trait Relay extends js.Object with EventEmitter {
  def setState(channel: Int, state: Boolean, callback: js.Function2[Error, Boolean, _] = null): Unit = js.native
  def turnOn  (channel: Int, delay: Int,     callback: js.Function2[Error, Boolean, _] = null): Unit = js.native
  def turnOff (channel: Int, delay: Int,     callback: js.Function2[Error, Boolean, _] = null): Unit = js.native
  def toggle  (channel: Int,                 callback: js.Function2[Error, Boolean, _] = null): Unit = js.native
  def getState(channel: Int,                 callback: js.Function2[Error, Boolean, _] = null): Unit = js.native
}

object Relay {

  sealed trait Channel { val value: Int }
  case object Channel1 extends { val value = 1 } with Channel
  case object Channel2 extends { val value = 2 } with Channel

  implicit class RelayOps(val relay: Relay) extends AnyVal {

    def onReady(listener: ⇒ Any)                = relay.on("ready", () ⇒ listener)
    def onLatch(listener: (Int, Boolean) ⇒ Any) = relay.on("latch", listener)

    def setState(channel: Channel, state: OnOff): Future[OnOff] = {

      val p = Promise[OnOff]()

      relay.setState(channel.value, state.value, (err: Error, state: Boolean) ⇒ {
        if (! js.isUndefined(err))
          p.failure(new RuntimeException(err.message))
        else
          p.success(OnOff.fromBoolean(state))
      })

      p.future
    }

    def state(channel: Channel): Future[OnOff] = {

      // TODO: remove code duplication with setState
      val p = Promise[OnOff]()

      relay.getState(channel.value, (err: Error, state: Boolean) ⇒ {
        if (! js.isUndefined(err))
          p.failure(new RuntimeException(err.message))
        else
          p.success(OnOff.fromBoolean(state))
      })

      p.future
    }
  }
}

@js.native
trait RelayMono extends js.Object {
  def use(port: Port, callback: js.Function2[Error, Relay, _] = null): Relay = js.native
}

object RelayMono {
  def apply() = g.require("relay-mono").asInstanceOf[RelayMono]
}