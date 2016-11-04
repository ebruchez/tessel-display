package org.bruchez.tessel

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.js.|

/**
  * Facade for a few jpeg-js, a pure javascript JPEG encoder and decoder for node.js.
  *
  * https://github.com/eugeneware/jpeg-js
  */

@js.native
@JSImport("jpeg-js", JSImport.Namespace)
object JpegJs extends js.Object {
  def decode(buffer: Buffer, asUint8Array: Boolean = false): JpegJsResult = js.native
}

@js.native
trait JpegJsResult extends js.Object {
  val width  : Int                 = js.native
  val height : Int                 = js.native
  val data   : Uint8Array | Buffer = js.native
}