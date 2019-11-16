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

import com.loudflow.domain.model.ModelState
import com.wix.accord.transform.ValidationTransform

import scala.math.{floor, round}
import play.api.libs.json._
import com.wix.accord.dsl._

import scala.util.Random

trait SimulationState {
  val random: Random = {
    val r = new Random(properties.seed)
    (1 to calls).foreach(_ => r.nextInt())
    r
  }
  def demuxer: String
  def properties: SimulationProperties
  def model: ModelState
  def calls: Int
  def ticks: Long
  def isRunning: Boolean
  def time: Long = properties.interval * ticks
  def steps: Long = round(floor(ticks / properties.step))
  def isStep: Boolean = (ticks % properties.step) == 0
}
object SimulationState {
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[SimulationState] = validator { properties =>
    properties.calls should be >= 0
    properties.ticks should be > 0L
    properties.model is valid[ModelState]
  }
  implicit val reads: Reads[SimulationState] = {
    (JsPath \ "demuxer").read[String].flatMap {
      case "graph" => implicitly[Reads[GraphSimulationState]].map(identity)
      case other => Reads(_ => JsError(s"Read Simulation.State failed due to unknown type $other."))
    }
  }
  implicit val writes: Writes[SimulationState] = Writes { obj =>
    val (jsValue, demuxer) = obj match {
      case command: GraphSimulationState => (Json.toJson(command)(GraphSimulationState.format), "graph")
    }
    jsValue.transform(JsPath.json.update((JsPath \ 'demuxer).json.put(JsString(demuxer)))).get
  }
}
