package org.bruchez.tessel

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global ⇒ g}
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.js.|

// jpeg-js
//
// A pure javascript JPEG encoder and decoder for node.js
//
// https://github.com/eugeneware/jpeg-js
//
object JpegJs {
    def apply() = g.require("jpeg-js").asInstanceOf[JpegJs]
}

@js.native
trait JpegJs extends js.Object {
  def decode(buffer: Buffer, asUint8Array: Boolean = false): JpegJsResult = js.native
}

@js.native
trait JpegJsResult extends js.Object {
  val width  : Int                 = js.native
  val height : Int                 = js.native
  val data   : Uint8Array | Buffer = js.native
}