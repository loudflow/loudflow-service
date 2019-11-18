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

sealed trait ModelAction extends Message {
  def modelId: String
}

/* ************************************************************************
   Action Actions
************************************************************************ */

final case class CreateModelAction(modelId: String, traceId: String, properties: ModelProperties) extends ModelAction {
  val demuxer = "create-model"
}
object CreateModelAction { implicit val format: Format[CreateModelAction] = Json.format }

final case class DestroyModelAction(modelId: String, traceId: String) extends ModelAction {
  val demuxer = "destroy-model"
}
object DestroyModelAction { implicit val format: Format[DestroyModelAction] = Json.format }

final case class AddEntityAction(modelId: String, traceId: String, entityType: String, kind: String, options: Option[EntityOptions] = None) extends ModelAction {
  val demuxer = "add-entity"
}
object AddEntityAction { implicit val format: Format[AddEntityAction] = Json.format }

final case class RemoveEntityAction(modelId: String, traceId: String, entityId: String) extends ModelAction {
  val demuxer = "remove-entity"
}
object RemoveEntityAction { implicit val format: Format[RemoveEntityAction] = Json.format }

final case class MoveEntityAction(modelId: String, traceId: String, entityId: String, position: Position) extends ModelAction {
  val demuxer = "move-entity"
}
object MoveEntityAction { implicit val format: Format[MoveEntityAction] = Json.format }

final case class PickEntityAction(modelId: String, traceId: String, entityId: String, targetId: String) extends ModelAction {
  val demuxer = "pick-entity"
}
object PickEntityAction { implicit val format: Format[PickEntityAction] = Json.format }

final case class DropEntityAction(modelId: String, traceId: String, entityId: String, targetId: String) extends ModelAction {
  val demuxer = "drop-entity"
}
object DropEntityAction { implicit val format: Format[DropEntityAction] = Json.format }

final case class BatchAction(modelId: String, traceId: String, actions: Seq[ModelAction]) extends ModelAction {
  val demuxer = "batch"
  require(actions.forall(!_.isInstanceOf[BatchAction])) // TODO: Find nicer way to prevent nested batches
}
object BatchAction { implicit val format: Format[BatchAction] = Json.format }

/* ************************************************************************
   JSON Serialization
************************************************************************ */

object ModelAction {
  implicit val reads: Reads[ModelAction] = {
    (JsPath \ "demuxer").read[String].flatMap {
      case "create-model" => implicitly[Reads[CreateModelAction]].map(identity)
      case "destroy-model" => implicitly[Reads[DestroyModelAction]].map(identity)
      case "add-entity" => implicitly[Reads[AddEntityAction]].map(identity)
      case "remove-entity" => implicitly[Reads[RemoveEntityAction]].map(identity)
      case "move-entity" => implicitly[Reads[MoveEntityAction]].map(identity)
      case "pick-entity" => implicitly[Reads[PickEntityAction]].map(identity)
      case "drop-entity" => implicitly[Reads[DropEntityAction]].map(identity)
      case "batch" => implicitly[Reads[BatchAction]].map(identity)
      case other => Reads(_ => JsError(s"Read ModelActionMessage failed due to unknown type $other."))
    }
  }
  implicit val writes: Writes[ModelAction] = Writes { obj =>
    val (jsValue, demuxer) = obj match {
      case action: CreateModelAction   => (Json.toJson(action)(CreateModelAction.format), "create-model")
      case action: DestroyModelAction   => (Json.toJson(action)(DestroyModelAction.format), "destroy-model")
      case action: AddEntityAction   => (Json.toJson(action)(AddEntityAction.format), "add-entity")
      case action: RemoveEntityAction   => (Json.toJson(action)(RemoveEntityAction.format), "remove-entity")
      case action: MoveEntityAction   => (Json.toJson(action)(MoveEntityAction.format), "move-entity")
      case action: PickEntityAction   => (Json.toJson(action)(PickEntityAction.format), "pick-entity")
      case action: DropEntityAction => (Json.toJson(action)(DropEntityAction.format), "drop-entity")
      case action: BatchAction => (Json.toJson(action)(BatchAction.format), "batch")
    }
    jsValue.transform(JsPath.json.update((JsPath \ 'demuxer).json.put(JsString(demuxer)))).get
  }
}
