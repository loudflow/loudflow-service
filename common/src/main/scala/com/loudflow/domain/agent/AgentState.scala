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
import com.loudflow.util.JavaRandom
import play.api.libs.json._
import sangria.schema.{BooleanType, Field, LongType, ObjectType, fields}

final case class AgentState(properties: AgentProperties, seed: Long, model: ModelState, ticks: Long = 1L, isActive: Boolean = false) {
  require(ticks > 0L, "Invalid argument 'ticks' for AgentState.")
  val random: JavaRandom = new JavaRandom(seed)
  def time: Long = properties.interval * ticks
}
object AgentState {
  implicit val format: Format[AgentState] = Json.format
  val SchemaType =
    ObjectType (
      "AgentStateType",
      "Agent state.",
      fields[Unit, AgentState](
        Field("seed", LongType, description = Some("Agent seed."), resolve = _.value.seed),
        Field("properties", AgentProperties.SchemaType, description = Some("Agent properties."), resolve = _.value.properties),
        Field("model", ModelState.SchemaType, description = Some("Model properties."), resolve = _.value.model),
        Field("ticks", LongType, description = Some("Number of ticks."), resolve = _.value.ticks),
        Field("isActive", BooleanType, description = Some("Is active flag."), resolve = _.value.isActive)
      )
    )
}

