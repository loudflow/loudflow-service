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

import com.loudflow.domain.simulation
import play.api.libs.json._
import com.loudflow.util.randomSeed
import sangria.schema.{EnumType, EnumValue, Field, InputField, InputObjectType, IntType, LongType, ObjectType, fields}

final case class SimulationProperties(time: TimeSystem.Value = TimeSystem.EVENT, seed: Long = randomSeed, interval: Int = 100, step: Int = 1) {
  require(interval > 50, "Invalid argument 'interval' for SimulationProperties.")
  require(step > 0, "Invalid argument 'step' for SimulationProperties.")
}
object SimulationProperties {
  implicit val format: Format[SimulationProperties] = Json.format
  val SchemaType: ObjectType[Unit, SimulationProperties] =
    ObjectType (
      "SimulationPropertiesType",
      "Simulation properties.",
      fields[Unit, SimulationProperties](
        Field("time", TimeSystem.SchemaType, description = Some("Type of model."), resolve = _.value.time),
        Field("seed", LongType, description = Some("Random number generator seed for the simulation."), resolve = _.value.seed),
        Field("interval", IntType, description = Some("Simulation interval."), resolve = _.value.interval),
        Field("step", IntType, description = Some("Simulation step."), resolve = _.value.step)
      )
    )
  val SchemaInputType: InputObjectType[SimulationProperties] =
    InputObjectType[SimulationProperties] (
      "SimulationPropertiesInputType",
      "Simulation properties input.",
      List(
        InputField("time", TimeSystem.SchemaType, "Type of model."),
        InputField("seed", LongType, "Random number generator seed for the simulation."),
        InputField("interval", IntType, "Simulation interval."),
        InputField("step", IntType, "Simulation step.")
      )
    )
}

object TimeSystem extends Enumeration {
  type TimeSystem = Value
  val CLOCK: TimeSystem.Value = Value
  val EVENT: TimeSystem.Value = Value
  val TURN: TimeSystem.Value = Value
  implicit val format: Format[TimeSystem.Value] = Json.formatEnum(this)
  val SchemaType: EnumType[simulation.TimeSystem.Value] =
    EnumType (
      "TimeSystemEnum",
      Some("Time system."),
      List (
        EnumValue("CLOCK", value = TimeSystem.CLOCK, description = Some("Clock-based time.")),
        EnumValue("EVENT", value = TimeSystem.EVENT, description = Some("Event-based time.")),
        EnumValue("TURN", value = TimeSystem.TURN, description = Some("Turn-based time."))
      )
    )
}
