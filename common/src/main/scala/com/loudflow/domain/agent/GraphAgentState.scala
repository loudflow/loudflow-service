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

import java.util.UUID

import com.loudflow.domain.model.{GraphState, ModelProperties}
import play.api.libs.json.{Format, Json}

final case class GraphAgentState(properties: AgentProperties, model: GraphState, calls: Int = 0, ticks: Long = 1L, isActive: Boolean = false) extends AgentState {
  val demuxer = "graph"
}
object GraphAgentState {
  implicit val format: Format[GraphAgentState] = Json.format
  def apply(agent: AgentProperties, model: ModelProperties): GraphAgentState = new GraphAgentState(agent, GraphState(UUID.randomUUID().toString, model))
}
