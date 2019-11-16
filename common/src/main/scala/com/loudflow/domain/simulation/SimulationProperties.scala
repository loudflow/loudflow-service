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

import com.wix.accord.transform.ValidationTransform
import java.util.UUID

import play.api.libs.json._
import com.wix.accord.dsl._

final case class SimulationProperties(time: TimeSystem.Value = TimeSystem.Event, seed: Long = UUID.randomUUID().getMostSignificantBits & Long.MaxValue, interval: Int = 100, step: Int = 1)
object SimulationProperties {
  implicit val format: Format[SimulationProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[SimulationProperties] = validator { properties =>
    properties.interval should be > 50
    properties.step should be > 0
  }
}

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
