package org.bruchez.tessel

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global ⇒ g}

@js.native
trait NodeSchedule extends js.Object {
  def scheduleJob(schedule: js.Any, cb: js.Function): Unit = js.native
}

object NodeSchedule {
  def apply() = g.require("node-schedule").asInstanceOf[NodeSchedule]

//  implicit class NodeScheduleOps(val s: NodeSchedule) extends AnyVal {
//    def scheduleJob(schedule: js.Object, cb: () ⇒ Any): Unit = s.scheduleJob(schedule, cb)
//  }
}