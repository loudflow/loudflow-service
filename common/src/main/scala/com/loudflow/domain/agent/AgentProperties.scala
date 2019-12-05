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

import com.loudflow.domain.agent
import play.api.libs.json._
import com.loudflow.util.randomSeed
import sangria.schema.{EnumType, EnumValue, Field, InputField, InputObjectType, IntType, LongType, ObjectType, fields}

final case class AgentProperties(agentType: AgentType.Value, seed: Long = randomSeed, interval: Int = 100) {
  require(interval > 50, "Invalid argument 'interval' for AgentProperties.")
}
object AgentProperties {
  implicit val format: Format[AgentProperties] = Json.format
  val SchemaType: ObjectType[Unit, AgentProperties] =
    ObjectType (
      "AgentPropertiesType",
      "Agent properties.",
      fields[Unit, AgentProperties](
        Field("agentType", AgentType.SchemaType, description = Some("Type of agent."), resolve = _.value.agentType),
        Field("seed", LongType, description = Some("Random number generator seed for the simulation."), resolve = _.value.seed),
        Field("interval", IntType, description = Some("Simulation interval."), resolve = _.value.interval)
      )
    )
  val SchemaInputType: InputObjectType[AgentProperties] =
    InputObjectType[AgentProperties] (
      "AgentPropertiesInputType",
      "Agent properties input.",
      List(
        InputField("agentType", AgentType.SchemaType, "Type of agent."),
        InputField("seed", LongType, "Random number generator seed for the simulation."),
        InputField("interval", IntType, "Simulation interval.")
      )
    )
}

object AgentType extends Enumeration {
  type AgentType = Value
  val RANDOM: AgentType.Value = Value
  implicit val format: Format[AgentType.Value] = Json.formatEnum(this)
  val SchemaType: EnumType[agent.AgentType.Value] =
    EnumType (
      "AgentTypeEnum",
      Some("Agent type."),
      List (
        EnumValue("RANDOM", value = AgentType.RANDOM, description = Some("Agent which acts randomly."))
      )
    )
}
