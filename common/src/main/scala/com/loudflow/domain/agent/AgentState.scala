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
package com.loudflow.domain.agent

import com.loudflow.domain.model.ModelState
import com.loudflow.domain.simulation.{GraphSimulationState, SimulationState}
import com.wix.accord.dsl._
import com.wix.accord.transform.ValidationTransform
import play.api.libs.json._

import scala.util.Random

trait AgentState {
  val random: Random = {
    val r = new Random(properties.seed)
    (1 to calls).foreach(_ => r.nextInt())
    r
  }
  def demuxer: String
  def properties: AgentProperties
  def model: ModelState
  def calls: Int
  def ticks: Long
  def isActive: Boolean
  def time: Long = properties.interval * ticks
}
object AgentState {
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[AgentState] = validator { properties =>
    properties.calls should be >= 0
    properties.ticks should be > 0L
    properties.model is valid[ModelState]
  }
  implicit val reads: Reads[AgentState] = {
    (JsPath \ "demuxer").read[String].flatMap {
      case "graph" => implicitly[Reads[GraphAgentState]].map(identity)
      case other => Reads(_ => JsError(s"Read Simulation.State failed due to unknown type $other."))
    }
  }
  implicit val writes: Writes[AgentState] = Writes { obj =>
    val (jsValue, demuxer) = obj match {
      case command: GraphAgentState => (Json.toJson(command)(GraphAgentState.format), "graph")
    }
    jsValue.transform(JsPath.json.update((JsPath \ 'demuxer).json.put(JsString(demuxer)))).get
  }
}

