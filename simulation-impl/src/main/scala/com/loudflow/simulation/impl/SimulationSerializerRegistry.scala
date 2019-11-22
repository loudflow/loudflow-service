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
package com.loudflow.simulation.impl

import scala.collection.immutable.Seq
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.loudflow.simulation.api.ReadSimulationResponse

object SimulationSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[ReadSimulationResponse],
    JsonSerializer[CreateSimulation],
    JsonSerializer[DestroySimulation],
    JsonSerializer[ReadSimulation],
    JsonSerializer[StartSimulation],
    JsonSerializer[StopSimulation],
    JsonSerializer[PauseSimulation],
    JsonSerializer[ResumeSimulation],
    JsonSerializer[AdvanceSimulation],
    JsonSerializer[UpdateSimulation],
    JsonSerializer[SimulationCreated],
    JsonSerializer[SimulationDestroyed],
    JsonSerializer[SimulationStarted],
    JsonSerializer[SimulationStopped],
    JsonSerializer[SimulationPaused],
    JsonSerializer[SimulationResumed],
    JsonSerializer[SimulationAdvanced],
    JsonSerializer[SimulationUpdated]
  )
}
