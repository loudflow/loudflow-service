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

import java.util.UUID

import play.api.libs.json.{Format, Json}
import com.loudflow.domain.model.{GraphState, ModelProperties}

final case class GraphSimulationState(properties: SimulationProperties, model: GraphState, calls: Int = 0, ticks: Long = 1L, isRunning: Boolean = false) extends SimulationState {
  val demuxer = "graph"
}
object GraphSimulationState {
  implicit val format: Format[GraphSimulationState] = Json.format
  def apply(simulation: SimulationProperties, model: ModelProperties): GraphSimulationState = new GraphSimulationState(simulation, GraphState(UUID.randomUUID().toString, model))
}

