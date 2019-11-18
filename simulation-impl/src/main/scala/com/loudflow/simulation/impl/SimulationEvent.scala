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

import play.api.libs.json.{Format, Json}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, AggregateEventShards, AggregateEvent}
import com.loudflow.domain.Message
import com.loudflow.domain.model.{ModelChange, BatchAction, ModelAction, ModelProperties}
import com.loudflow.domain.simulation.SimulationProperties

sealed trait SimulationEvent extends AggregateEvent[SimulationEvent] with Message {
  def aggregateTag: AggregateEventShards[SimulationEvent] = SimulationEvent.Tag
  def simulationId: String
}
object SimulationEvent {
  val shardCount: Int = 10
  val Tag: AggregateEventShards[SimulationEvent] = AggregateEventTag.sharded[SimulationEvent](shardCount)

  def toAction(event: SimulationEvent): ModelAction = event match {
    case SimulationStarted(_, traceId, actions) => if (actions.nonEmpty) BatchAction(actions.head.modelId, traceId, actions) else BatchAction("", "", Seq.empty)
    case SimulationStopped(_, traceId, actions) => if (actions.nonEmpty) BatchAction(actions.head.modelId, traceId, actions) else BatchAction("", "", Seq.empty)
    case SimulationAdvanced(_, traceId, actions) => if (actions.nonEmpty) BatchAction(actions.head.modelId, traceId, actions) else BatchAction("", "", Seq.empty)
    case _ => BatchAction("", "", Seq.empty)
  }
}

/* ************************************************************************
   CRUD Events
************************************************************************ */

final case class SimulationCreated(simulationId: String, traceId: String, simulation: SimulationProperties, model: ModelProperties) extends SimulationEvent {
  val eventType = "simulation-created"
}
object SimulationCreated { implicit val format: Format[SimulationCreated] = Json.format }

final case class SimulationDestroyed(simulationId: String, traceId: String) extends SimulationEvent {
  val eventType = "simulation-destroyed"
}
object SimulationDestroyed { implicit val format: Format[SimulationDestroyed] = Json.format }

/* ************************************************************************
   Control Events
************************************************************************ */

final case class SimulationStarted(simulationId: String, traceId: String, actions: List[ModelAction]) extends SimulationEvent {
  val eventType = "simulation-started"
}
object SimulationStarted { implicit val format: Format[SimulationStarted] = Json.format }

final case class SimulationStopped(simulationId: String, traceId: String, actions: List[ModelAction]) extends SimulationEvent {
  val eventType = "simulation-stopped"
}
object SimulationStopped { implicit val format: Format[SimulationStopped] = Json.format }

final case class SimulationPaused(simulationId: String, traceId: String) extends SimulationEvent {
  val eventType = "simulation-paused"
}
object SimulationPaused { implicit val format: Format[SimulationPaused] = Json.format }

final case class SimulationResumed(simulationId: String, traceId: String) extends SimulationEvent {
  val eventType = "simulation-resumed"
}
object SimulationResumed { implicit val format: Format[SimulationResumed] = Json.format }

final case class SimulationAdvanced(simulationId: String, traceId: String, actions: List[ModelAction]) extends SimulationEvent {
  val eventType = "simulation-advanced"
}
object SimulationAdvanced { implicit val format: Format[SimulationAdvanced] = Json.format }

final case class SimulationUpdated(simulationId: String, traceId: String, change: ModelChange) extends SimulationEvent {
  val eventType = "simulation-updated"
}
object SimulationUpdated { implicit val format: Format[SimulationUpdated] = Json.format }
