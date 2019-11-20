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
package com.loudflow.simulation.api

import com.loudflow.domain.model.GraphState
import play.api.libs.json._
import com.loudflow.domain.simulation.SimulationState

final case class ReadSimulationResponse private(data: ReadSimulationResponse.Data)
object ReadSimulationResponse {
  implicit val format: Format[ReadSimulationResponse] = Json.format
  def apply(id: String, state: SimulationState): ReadSimulationResponse = state.model match {
    case _: GraphState => new ReadSimulationResponse(Data(id, state))
  }
  final case class Data(id: String, attributes: SimulationState) {
    val `type`: String = "simulation"
  }
  object Data { implicit val format: Format[Data] = Json.format }
}
