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

import akka.Done
import play.api.libs.json._
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.loudflow.domain.Message
import com.loudflow.domain.model._

sealed trait ModelCommand extends Message {
  def demuxer: String
}

/* ************************************************************************
   CRUD Commands
************************************************************************ */

final case class CreateModel(traceId: String, properties: ModelProperties) extends ModelCommand with PersistentEntity.ReplyType[Done] {
  val demuxer = "create-model"
}
object CreateModel { implicit val format: Format[CreateModel] = Json.format }

final case class DestroyModel(traceId: String) extends ModelCommand with PersistentEntity.ReplyType[Done] {
  val demuxer = "destroy-model"
}
object DestroyModel { implicit val format: Format[DestroyModel] = Json.format }

final case class ReadModel(traceId: String) extends ModelCommand with PersistentEntity.ReplyType[ModelState] {
  val demuxer = "read-model"
}
object ReadModel { implicit val format: Format[ReadModel] = Json.format }

/* ************************************************************************
   Action Commands
************************************************************************ */

final case class AddEntity(traceId: String, kind: String, options: Option[EntityOptions], position: Option[Position]) extends ModelCommand with PersistentEntity.ReplyType[Done] {
  val demuxer = "add-entity"
}
object AddEntity { implicit val format: Format[AddEntity] = Json.format }

final case class RemoveEntity(traceId: String, entityId: String) extends ModelCommand with PersistentEntity.ReplyType[Done] {
  val demuxer = "remove-entity"
}
object RemoveEntity { implicit val format: Format[RemoveEntity] = Json.format }

final case class MoveEntity(traceId: String, entityId: String, position: Option[Position]) extends ModelCommand with PersistentEntity.ReplyType[Done] {
  val demuxer = "move-entity"
}
object MoveEntity { implicit val format: Format[MoveEntity] = Json.format }

final case class PickEntity(traceId: String, entityId: String, targetId: String) extends ModelCommand with PersistentEntity.ReplyType[Done] {
  val demuxer = "pick-entity"
}
object PickEntity { implicit val format: Format[PickEntity] = Json.format }

final case class DropEntity(traceId: String, entityId: String, targetId: String) extends ModelCommand with PersistentEntity.ReplyType[Done] {
  val demuxer = "drop-entity"
}
object DropEntity { implicit val format: Format[DropEntity] = Json.format }

/* ************************************************************************
   JSON Serialization
************************************************************************ */

object ModelCommand {
  implicit val reads: Reads[ModelCommand] = {
    (JsPath \ "demuxer").read[String].flatMap {
      case "create-model" => implicitly[Reads[CreateModel]].map(identity)
      case "destroy-model" => implicitly[Reads[DestroyModel]].map(identity)
      case "read-model" => implicitly[Reads[ReadModel]].map(identity)
      case "add-entity" => implicitly[Reads[AddEntity]].map(identity)
      case "remove-entity" => implicitly[Reads[RemoveEntity]].map(identity)
      case "move-entity" => implicitly[Reads[MoveEntity]].map(identity)
      case "pick-entity" => implicitly[Reads[PickEntity]].map(identity)
      case "drop-entity" => implicitly[Reads[DropEntity]].map(identity)
      case other => Reads(_ => JsError(s"Read ModelCommand failed due to unknown type $other."))
    }
  }
  implicit val writes: Writes[ModelCommand] = Writes { obj =>
    val (jsValue, demuxer) = obj match {
      case command: CreateModel   => (Json.toJson(command)(CreateModel.format), "create-model")
      case command: DestroyModel   => (Json.toJson(command)(DestroyModel.format), "destroy-model")
      case command: ReadModel   => (Json.toJson(command)(ReadModel.format), "read-model")
      case command: AddEntity   => (Json.toJson(command)(AddEntity.format), "add-entity")
      case command: RemoveEntity => (Json.toJson(command)(RemoveEntity.format), "remove-entity")
      case command: MoveEntity => (Json.toJson(command)(MoveEntity.format), "move-entity")
      case command: PickEntity => (Json.toJson(command)(PickEntity.format), "pick-entity")
      case command: DropEntity => (Json.toJson(command)(DropEntity.format), "drop-entity")
    }
    jsValue.transform(JsPath.json.update((JsPath \ 'demuxer).json.put(JsString(demuxer)))).get
  }
  def fromAction(action: ModelAction): Seq[ModelCommand with PersistentEntity.ReplyType[Done]] = action match {
    case CreateModelAction(_, traceId, properties) => Seq(CreateModel(traceId, properties))
    case DestroyModelAction(_, traceId) => Seq(DestroyModel(traceId))
    case AddEntityAction(_, traceId, kind, options, position) => Seq(AddEntity(traceId, kind, options, position))
    case RemoveEntityAction(_, traceId, entityId) => Seq(RemoveEntity(traceId, entityId))
    case MoveEntityAction(_, traceId, entityId, position) => Seq(MoveEntity(traceId, entityId, position))
    case PickEntityAction(_, traceId, entityId, targetId) => Seq(PickEntity(traceId, entityId, targetId))
    case DropEntityAction(_, traceId, entityId, targetId) => Seq(DropEntity(traceId, entityId, targetId))
    case BatchAction(_, _, actions) => actions.flatMap(fromAction)
  }
}
