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

import com.loudflow.domain.model.ModelProperties
import play.api.libs.json.{Format, Json}
import com.loudflow.domain.simulation.SimulationProperties

final case class CreateSimulationRequest(simulation: SimulationProperties, model: ModelProperties)
object CreateSimulationRequest { implicit val format: Format[CreateSimulationRequest] = Json.format }
