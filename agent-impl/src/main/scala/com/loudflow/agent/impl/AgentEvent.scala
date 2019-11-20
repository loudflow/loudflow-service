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
package com.loudflow.agent.impl

import play.api.libs.json.{Format, Json}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, AggregateEventShards, AggregateEvent}
import com.loudflow.domain.Message
import com.loudflow.domain.model.{ModelChange, BatchAction, ModelAction, ModelProperties}
import com.loudflow.domain.agent.AgentProperties

sealed trait AgentEvent extends AggregateEvent[AgentEvent] with Message {
  def aggregateTag: AggregateEventShards[AgentEvent] = AgentEvent.Tag
  def agentId: String
}
object AgentEvent {
  val shardCount: Int = 10
  val Tag: AggregateEventShards[AgentEvent] = AggregateEventTag.sharded[AgentEvent](shardCount)

  def toAction(event: AgentEvent): ModelAction = event match {
    case AgentStarted(_, traceId, actions) => if (actions.nonEmpty) BatchAction(actions.head.modelId, traceId, actions) else BatchAction("", "", List.empty)
    case AgentStopped(_, traceId, actions) => if (actions.nonEmpty) BatchAction(actions.head.modelId, traceId, actions) else BatchAction("", "", List.empty)
    case AgentAdvanced(_, traceId, actions) => if (actions.nonEmpty) BatchAction(actions.head.modelId, traceId, actions) else BatchAction("", "", List.empty)
    case _ => BatchAction("", "", List.empty)
  }
}

/* ************************************************************************
   CRUD Events
************************************************************************ */

final case class AgentCreated(agentId: String, traceId: String, agent: AgentProperties, model: ModelProperties) extends AgentEvent {
  val eventType = "agent-created"
}
object AgentCreated { implicit val format: Format[AgentCreated] = Json.format }

final case class AgentDestroyed(agentId: String, traceId: String) extends AgentEvent {
  val eventType = "agent-destroyed"
}
object AgentDestroyed { implicit val format: Format[AgentDestroyed] = Json.format }

/* ************************************************************************
   Control Events
************************************************************************ */

final case class AgentStarted(agentId: String, traceId: String, actions: List[ModelAction]) extends AgentEvent {
  val eventType = "agent-started"
}
object AgentStarted { implicit val format: Format[AgentStarted] = Json.format }

final case class AgentStopped(agentId: String, traceId: String, actions: List[ModelAction]) extends AgentEvent {
  val eventType = "agent-stopped"
}
object AgentStopped { implicit val format: Format[AgentStopped] = Json.format }

final case class AgentPaused(agentId: String, traceId: String) extends AgentEvent {
  val eventType = "agent-paused"
}
object AgentPaused { implicit val format: Format[AgentPaused] = Json.format }

final case class AgentResumed(agentId: String, traceId: String) extends AgentEvent {
  val eventType = "agent-resumed"
}
object AgentResumed { implicit val format: Format[AgentResumed] = Json.format }

final case class AgentAdvanced(agentId: String, traceId: String, actions: List[ModelAction]) extends AgentEvent {
  val eventType = "agent-advanced"
}
object AgentAdvanced { implicit val format: Format[AgentAdvanced] = Json.format }

final case class AgentUpdated(agentId: String, traceId: String, change: ModelChange) extends AgentEvent {
  val eventType = "agent-updated"
}
object AgentUpdated { implicit val format: Format[AgentUpdated] = Json.format }
