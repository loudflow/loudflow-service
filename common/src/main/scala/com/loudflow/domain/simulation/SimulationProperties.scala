/* ************************************************************************
    _                 _  __ _
   | |               | |/ _| |
   | | ___  _   _  __| | |_| | _____      __
   | |/ _ \| | | |/ _` |  _| |/ _ \ \ /\ / /
   | | (_) | |_| | (_| | | | | (_) \ V  V /
   |_|\___/ \__,_|\__,_|_| |_|\___/ \_/\_/

   a framework for building multi-agent systems
   copyright © 2019, farsimple - all rights reserved

   This file is subject to the terms and conditions defined in
   file 'LICENSE.txt', which is part of this source code package.

************************************************************************ */
package com.loudflow.domain.simulation

import play.api.libs.json._

import com.loudflow.util.randomSeed

final case class SimulationProperties(time: TimeSystem.Value = TimeSystem.Event, seed: Long = randomSeed, interval: Int = 100, step: Int = 1) {
  require(interval > 50, "Invalid argument 'interval' for SimulationProperties.")
  require(step > 0, "Invalid argument 'step' for SimulationProperties.")
}
object SimulationProperties { implicit val format: Format[SimulationProperties] = Json.format }

object TimeSystem {

  sealed trait Value

  final case object Clock extends Value {
    val demuxer = "clock"
  }

  final case object Event extends Value {
    val demuxer = "event"
  }

  final case object Turn extends Value {
    val demuxer = "turn"
  }

  val values: Set[Value] = Set(Clock, Event, Turn)

  implicit val reads: Reads[Value] = Reads { json =>
    (JsPath \ "demuxer").read[String].reads(json).flatMap {
      case "clock" => JsSuccess(Clock)
      case "event" => JsSuccess(Event)
      case "turn" => JsSuccess(Turn)
      case other => JsError(s"Read TemporalMode failed due to unknown enumeration value $other.")
    }
  }
  implicit val writes: Writes[Value] = Writes {
    case Clock => JsObject(Seq("demuxer" -> JsString("clock")))
    case Event => JsObject(Seq("demuxer" -> JsString("event")))
    case Turn => JsObject(Seq("demuxer" -> JsString("turn")))
  }
}
