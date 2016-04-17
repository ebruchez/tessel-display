package org.bruchez.tessel

import scala.async.Async._
import scala.concurrent.duration._
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global ⇒ g}
import scala.scalajs.js.timers.SetIntervalHandle

object Demo extends js.JSApp {

  val tessel    = Tessel()
  val v8        = V8()
  val os        = OS()
  val nodeFetch = NodeFetch()
  val jpeg      = JpegJs()

  object RectangleBlinker {

    private var state = false
    private var handle: Option[SetIntervalHandle] = None

    def start(): Unit = {
      if (handle.isEmpty)
        handle = Some(
          js.timers.setInterval(1.second) {

            val color =
              if (state)
                ST7735.Color.Cyan
              else
                ST7735.Color.Magenta

            ST7735.fillRectF(0, 0, 10, 10, color)

            state = ! state
          }
        )
    }
  }

  def main(): Unit = async {

    LEDBlinker.start()

    await(HT16K33.initialize())
    await(ST7735.initialize())

    val it = Iterator.from(0, 1)

    while (it.hasNext) {

      // Because placekitten.com always returns the same image for a given size, in order to get a set of different
      // images we get images that are progressively taller and truncate their bottom when writing them to the buffer.
      val digit  = it.next() % 30
      val height = ST7735.ScreenHeight + digit
      val url    = s"https://placekitten.com/${ST7735.ScreenWidth}/$height"

      // TODO: Handle failure of fetch and just continue with next one. Have this return a Future[Try[Response]].
      val response = await(nodeFetch(url, null).toFuture)

      println(s"got response buffer: ${response.status}, length: ${response.headers.get("Content-Length")}")

      val responseBuffer = await(response.asInstanceOf[NodeFetchResponse].buffer().toFuture)
      val rawImageData   = jpeg.decode(responseBuffer, asUint8Array = true)

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
        case _⇒
          throw new IllegalStateException // should not happen if `jpeg.decode` is honest
      }

      await(Util.delay(5.seconds))
    }

    RectangleBlinker.start()
  } onFailure { case t ⇒
    println(s"got failure: ${t.getMessage}")
  }
}
