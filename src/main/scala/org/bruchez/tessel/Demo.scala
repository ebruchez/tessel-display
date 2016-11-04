package org.bruchez.tessel

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.util.{Failure, Success}
import Util._

object Demo extends js.JSApp {

  val UpdateDelay = 5.seconds
  val RetryDelay  = 5.seconds

  def kittens(): Future[Unit] = async {

    val it = Iterator.from(0, 1)

    while (it.hasNext) {

      // Because placekitten.com always returns the same image for a given size, in order to get a set of different
      // images we get images that are progressively taller and truncate their bottom when writing them to the buffer.
      val increment = it.next() % 30
      val height    = ST7735.ScreenHeight + increment
      val url       = s"https://placekitten.com/${ST7735.ScreenWidth}/$height"

      // TODO: Handle failure of fetch and just continue with next one. Have this return a Future[Try[Response]].
      val response = await(NodeFetch(url, null).toFuture)

      println(s"got response buffer: ${response.status}, length: ${response.headers.get("Content-Length")}")

      val responseBuffer = await(response.asInstanceOf[NodeFetchResponse].buffer().toFuture)
      val rawImageData   = JpegJs.decode(responseBuffer, asUint8Array = true)

      println(s"decoded image data, w = ${rawImageData.width}, h = ${rawImageData.height}")

      rawImageData.data match {
        case data: Uint8Array ⇒
          await(
            ST7735.sendFrame(
              data,
              rawImageData.width  min ST7735.ScreenWidth,
              rawImageData.height min ST7735.ScreenHeight
            )
          )
        case _: Buffer ⇒
          throw new IllegalStateException // should not happen if `jpeg.decode` is honest
      }

      await(delay(UpdateDelay))
    }
  }

  def main(): Unit = async {

    LEDBlinker.start()

    await(HT16K33.initialize())
    await(ST7735.initialize())

    var done = false
    while (! done) {
      await(kittens().toTry) match {
        case Failure(t) ⇒
          println(s"Error showing kittens: ${t.getMessage}. Retrying in ${RetryDelay.toString}.")
          await(delay(RetryDelay))
        case Success(_) ⇒
          // Shouldn't happen as kittens() doesn't normally return
          done = true
      }
    }

  } onFailure { case t ⇒
    println(s"Error during initialization: ${t.getMessage}")
  }
}
