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
package com.loudflow.model.impl

import play.api.libs.json.{Format, Json}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, AggregateEventShards, AggregateEvent}
import com.loudflow.domain.Message
import com.loudflow.domain.model._

sealed trait ModelEvent extends AggregateEvent[ModelEvent] with Message {
  def aggregateTag: AggregateEventShards[ModelEvent] = ModelEvent.Tag
  def modelId: String
}
object ModelEvent {
  val shardCount: Int = 10
  val Tag: AggregateEventShards[ModelEvent] = AggregateEventTag.sharded[ModelEvent](shardCount)

  def toChange(event: ModelEvent): ModelChange = event match {
    case ModelCreated(modelId, traceId, properties) => ModelCreatedChange(modelId, traceId, properties)
    case ModelDestroyed(modelId, traceId) => ModelDestroyedChange(modelId, traceId)
    case EntityAdded(modelId, traceId, kind, options, position) => EntityAddedChange(modelId, traceId, kind, options, position)
    case EntityRemoved(modelId, traceId, entityId) => EntityRemovedChange(modelId, traceId, entityId)
    case EntityMoved(modelId, traceId, entityId, position) => EntityMovedChange(modelId, traceId, entityId, position)
    case EntityPicked(modelId, traceId, entityId, targetId) => EntityPickedChange(modelId, traceId, entityId, targetId)
    case EntityDropped(modelId, traceId, entityId, targetId) => EntityDroppedChange(modelId, traceId, entityId, targetId)
  }
}

/* ************************************************************************
   CRUD Events
************************************************************************ */

final case class ModelCreated(modelId: String, traceId: String, properties: ModelProperties) extends ModelEvent {
  val eventType = "model-created"
}
object ModelCreated { implicit val format: Format[ModelCreated] = Json.format }

final case class ModelDestroyed(modelId: String, traceId: String) extends ModelEvent {
  val eventType = "model-destroyed"
}
object ModelDestroyed { implicit val format: Format[ModelDestroyed] = Json.format }

/* ************************************************************************
   Change Events
************************************************************************ */

final case class EntityAdded(modelId: String, traceId: String, kind: String, options: Option[EntityOptions], position: Option[Position]) extends ModelEvent {
  val eventType = "entity-added"
}
object EntityAdded { implicit val format: Format[EntityAdded] = Json.format }

final case class EntityRemoved(modelId: String, traceId: String, entityId: String) extends ModelEvent {
  val eventType = "entity-removed"
}
object EntityRemoved { implicit val format: Format[EntityRemoved] = Json.format }

final case class EntityMoved(modelId: String, traceId: String, entityId: String, position: Option[Position]) extends ModelEvent {
  val eventType = "entity-moved"
}
object EntityMoved { implicit val format: Format[EntityMoved] = Json.format }

final case class EntityPicked(modelId: String, traceId: String, entityId: String, targetId: String) extends ModelEvent {
  val eventType = "entity-picked"
}
object EntityPicked { implicit val format: Format[EntityPicked] = Json.format }

final case class EntityDropped(modelId: String, traceId: String, entityId: String, targetId: String) extends ModelEvent {
  val eventType = "entity-dropped"
}
object EntityDropped { implicit val format: Format[EntityDropped] = Json.format }
