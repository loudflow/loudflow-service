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
package com.loudflow.agent.api

import com.loudflow.domain.agent.AgentState
import com.loudflow.domain.model.graph.GraphModelState
import play.api.libs.json.{Format, Json}

final case class ReadAgentResponse private(data: ReadAgentResponse.Data)
object ReadAgentResponse {
  implicit val format: Format[ReadAgentResponse] = Json.format
  def apply(id: String, state: AgentState): ReadAgentResponse = state.model match {
    case _: GraphModelState => new ReadAgentResponse(Data(id, state))
  }
  final case class Data(id: String, attributes: AgentState) {
    val `type`: String = "simulation"
  }
  object Data { implicit val format: Format[Data] = Json.format }
}
