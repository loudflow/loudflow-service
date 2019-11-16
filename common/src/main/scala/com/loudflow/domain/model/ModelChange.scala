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
package com.loudflow.domain.model

import play.api.libs.json._
import com.loudflow.domain.Message

sealed trait ModelChange extends Message {
  def modelId: String
}

/* ************************************************************************
   Action Requests
************************************************************************ */

final case class ModelCreatedChange(modelId: String, traceId: String, properties: ModelProperties) extends ModelChange {
  val demuxer = "model-created"
}
object ModelCreatedChange { implicit val format: Format[ModelCreatedChange] = Json.format }

final case class ModelDestroyedChange(modelId: String, traceId: String) extends ModelChange {
  val demuxer = "model-destroyed"
}
object ModelDestroyedChange { implicit val format: Format[ModelDestroyedChange] = Json.format }

final case class EntityAddedChange(modelId: String, traceId: String, entityType: String, kind: String, options: EntityOptions) extends ModelChange {
  val demuxer = "entity-added"
}
object EntityAddedChange { implicit val format: Format[EntityAddedChange] = Json.format }

final case class EntityRemovedChange(modelId: String, traceId: String, entityId: String) extends ModelChange {
  val demuxer = "entity-removed"
}
object EntityRemovedChange { implicit val format: Format[EntityRemovedChange] = Json.format }

final case class EntityMovedChange(modelId: String, traceId: String, entityId: String, position: Position) extends ModelChange {
  val demuxer = "entity-moved"
}
object EntityMovedChange { implicit val format: Format[EntityMovedChange] = Json.format }

final case class EntityPickedChange(modelId: String, traceId: String, entityId: String, targetId: String) extends ModelChange {
  val demuxer = "entity-picked"
}
object EntityPickedChange { implicit val format: Format[EntityPickedChange] = Json.format }

final case class EntityDroppedChange(modelId: String, traceId: String, entityId: String, targetId: String) extends ModelChange {
  val demuxer = "entity-dropped"
}
object EntityDroppedChange { implicit val format: Format[EntityDroppedChange] = Json.format }

/* ************************************************************************
   JSON Serialization
************************************************************************ */

object ModelChange {
  implicit val reads: Reads[ModelChange] = {
    (JsPath \ "demuxer").read[String].flatMap {
      case "model-created" => implicitly[Reads[ModelCreatedChange]].map(identity)
      case "model-destroyed" => implicitly[Reads[ModelDestroyedChange]].map(identity)
      case "entity-added" => implicitly[Reads[EntityAddedChange]].map(identity)
      case "entity-removed" => implicitly[Reads[EntityRemovedChange]].map(identity)
      case "entity-moved" => implicitly[Reads[EntityMovedChange]].map(identity)
      case "entity-picked" => implicitly[Reads[EntityPickedChange]].map(identity)
      case "entity-dropped" => implicitly[Reads[EntityDroppedChange]].map(identity)
      case other => Reads(_ => JsError(s"Read ModelChangedChange failed due to unknown type $other."))
    }
  }
  implicit val writes: Writes[ModelChange] = Writes { obj =>
    val (jsValue, demuxer) = obj match {
      case change: ModelCreatedChange   => (Json.toJson(change)(ModelCreatedChange.format), "model-created")
      case change: ModelDestroyedChange   => (Json.toJson(change)(ModelDestroyedChange.format), "model-destroyed")
      case change: EntityAddedChange   => (Json.toJson(change)(EntityAddedChange.format), "entity-added")
      case change: EntityRemovedChange   => (Json.toJson(change)(EntityRemovedChange.format), "entity-removed")
      case change: EntityMovedChange   => (Json.toJson(change)(EntityMovedChange.format), "entity-moved")
      case change: EntityPickedChange   => (Json.toJson(change)(EntityPickedChange.format), "entity-picked")
      case change: EntityDroppedChange => (Json.toJson(change)(EntityDroppedChange.format), "entity-dropped")
    }
    jsValue.transform(JsPath.json.update((JsPath \ 'demuxer).json.put(JsString(demuxer)))).get
  }
}
