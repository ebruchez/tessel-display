package org.bruchez.tessel

import org.scalajs.dom.experimental.{RequestInit, Response, _}

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global ⇒ g}

object NodeFetch {
  def apply() = g.require("node-fetch").asInstanceOf[js.Function2[RequestInfo, RequestInit, js.Promise[Response]]]
}

@js.native
trait NodeFetchResponse extends Response {
  def buffer(): js.Promise[Buffer] // "buffer() is a node-fetch only API"
}