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
import com.loudflow.domain.model.entity.{EntityOptions, EntityProperties}
import sangria.schema.{Field, InputField, InputObjectType, InterfaceType, ListInputType, LongType, ObjectType, OptionInputType, OptionType, StringType, fields, interfaces}

sealed trait ModelChange extends Message {
  def modelId: String
}

/* ************************************************************************
   Action Requests
************************************************************************ */

final case class ModelCreatedChange(modelId: String, traceId: String, properties: ModelProperties) extends ModelChange {
  val demuxer = "model-created"
}
object ModelCreatedChange {
  implicit val format: Format[ModelCreatedChange] = Json.format
  val SchemaType: ObjectType[Unit, ModelCreatedChange] =
    ObjectType (
      "ModelCreatedChangeType",
      "Model created change message.",
      interfaces[Unit, ModelCreatedChange](ModelChange.SchemaType),
      fields[Unit, ModelCreatedChange](
        Field("modelId", StringType, description = Some("Model identifier."), resolve = _.value.modelId),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("properties", ModelProperties.SchemaType, description = Some("Model properties."), resolve = _.value.properties)
      )
    )
  val SchemaInputType: InputObjectType[ModelCreatedChange] =
    InputObjectType[ModelCreatedChange] (
      "ModelCreatedChangeInputType",
      "Model created change message.",
      List(
        InputField("modelId", StringType, "Model identifier."),
        InputField("traceId", StringType, "Trace identifier."),
        InputField("properties", ModelProperties.SchemaInputType, "Model properties.")
      )
    )
}

final case class ModelDestroyedChange(modelId: String, traceId: String) extends ModelChange {
  val demuxer = "model-destroyed"
}
object ModelDestroyedChange {
  implicit val format: Format[ModelDestroyedChange] = Json.format
  val SchemaType: ObjectType[Unit, ModelDestroyedChange] =
    ObjectType (
      "ModelDestroyedChangeType",
      "Model destroyed change message.",
      interfaces[Unit, ModelDestroyedChange](ModelChange.SchemaType),
      fields[Unit, ModelDestroyedChange](
        Field("modelId", StringType, description = Some("Model identifier."), resolve = _.value.modelId),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId)
      )
    )
  val SchemaInputType: InputObjectType[ModelDestroyedChange] =
    InputObjectType[ModelDestroyedChange] (
      "ModelDestroyedChangeInputType",
      "Model destroyed change message.",
      List(
        InputField("modelId", StringType, "Model identifier."),
        InputField("traceId", StringType, "Trace identifier.")
      )
    )
}

final case class EntityAddedChange(modelId: String, traceId: String, kind: String, options: Option[EntityOptions], position: Option[Position]) extends ModelChange {
  val demuxer = "entity-added"
}
object EntityAddedChange {
  implicit val format: Format[EntityAddedChange] = Json.format
  val SchemaType: ObjectType[Unit, EntityAddedChange] =
    ObjectType (
      "EntityAddedChangeType",
      "Entity added change message.",
      interfaces[Unit, EntityAddedChange](ModelChange.SchemaType),
      fields[Unit, EntityAddedChange](
        Field("modelId", StringType, description = Some("Model identifier."), resolve = _.value.modelId),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("kind", StringType, description = Some("Entity kind."), resolve = _.value.kind),
        Field("options", OptionType(EntityOptions.SchemaType), description = Some("Entity options."), resolve = _.value.options),
        Field("position", OptionType(Position.SchemaType), description = Some("Entity position."), resolve = _.value.position)
      )
    )
  val SchemaInputType: InputObjectType[EntityAddedChange] =
    InputObjectType[EntityAddedChange] (
      "EntityAddedChangeInputType",
      "Entity added change message.",
      List(
        InputField("modelId", StringType, "Model identifier."),
        InputField("traceId", StringType, "Trace identifier."),
        InputField("kind", StringType, "Entity kind."),
        InputField("options", OptionInputType(EntityOptions.SchemaInputType), "Entity options."),
        InputField("position", OptionInputType(Position.SchemaInputType), "Entity position.")
      )
    )
}

final case class EntityRemovedChange(modelId: String, traceId: String, entityId: String) extends ModelChange {
  val demuxer = "entity-removed"
}
object EntityRemovedChange {
  implicit val format: Format[EntityRemovedChange] = Json.format
  val SchemaType: ObjectType[Unit, EntityRemovedChange] =
    ObjectType (
      "EntityRemovedChangeType",
      "Entity removed change message.",
      interfaces[Unit, EntityRemovedChange](ModelChange.SchemaType),
      fields[Unit, EntityRemovedChange](
        Field("modelId", StringType, description = Some("Model identifier."), resolve = _.value.modelId),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("entityId", StringType, description = Some("Entity identifier."), resolve = _.value.entityId)
      )
    )
  val SchemaInputType: InputObjectType[EntityRemovedChange] =
    InputObjectType[EntityRemovedChange] (
      "EntityRemovedChangeInputType",
      "Entity removed change message.",
      List(
        InputField("modelId", StringType, "Model identifier."),
        InputField("traceId", StringType, "Trace identifier."),
        InputField("entityId", StringType, "Entity identifier.")
      )
    )
}

final case class EntityMovedChange(modelId: String, traceId: String, entityId: String, position: Option[Position]) extends ModelChange {
  val demuxer = "entity-moved"
}
object EntityMovedChange {
  implicit val format: Format[EntityMovedChange] = Json.format
  val SchemaType: ObjectType[Unit, EntityMovedChange] =
    ObjectType (
      "EntityMovedChangeType",
      "Entity moved change message.",
      interfaces[Unit, EntityMovedChange](ModelChange.SchemaType),
      fields[Unit, EntityMovedChange](
        Field("modelId", StringType, description = Some("Model identifier."), resolve = _.value.modelId),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("entityId", StringType, description = Some("Entity identifier."), resolve = _.value.entityId),
        Field("position", OptionType(Position.SchemaType), description = Some("Entity position."), resolve = _.value.position)
      )
    )
  val SchemaInputType: InputObjectType[EntityMovedChange] =
    InputObjectType[EntityMovedChange] (
      "EntityMovedChangeInputType",
      "Entity moved change message.",
      List(
        InputField("modelId", StringType, "Model identifier."),
        InputField("traceId", StringType, "Trace identifier."),
        InputField("entityId", StringType, "Entity identifier."),
        InputField("position", OptionInputType(Position.SchemaInputType), "Entity position.")
      )
    )
}

final case class EntityPickedChange(modelId: String, traceId: String, entityId: String, targetId: String) extends ModelChange {
  val demuxer = "entity-picked"
}
object EntityPickedChange {
  implicit val format: Format[EntityPickedChange] = Json.format
  val SchemaType: ObjectType[Unit, EntityPickedChange] =
    ObjectType (
      "EntityPickedChangeType",
      "Entity picked change message.",
      interfaces[Unit, EntityPickedChange](ModelChange.SchemaType),
      fields[Unit, EntityPickedChange](
        Field("modelId", StringType, description = Some("Model identifier."), resolve = _.value.modelId),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("entityId", StringType, description = Some("Entity (doing the picking) identifier."), resolve = _.value.entityId),
        Field("targetId", StringType, description = Some("Target entity (being picked) identifier."), resolve = _.value.targetId)
      )
    )
  val SchemaInputType: InputObjectType[EntityPickedChange] =
    InputObjectType[EntityPickedChange] (
      "EntityPickedChangeInputType",
      "Entity picked change message.",
      List(
        InputField("modelId", StringType, "Model identifier."),
        InputField("traceId", StringType, "Trace identifier."),
        InputField("entityId", StringType, "Entity (doing the picking) identifier."),
        InputField("targetId", StringType, "Target entity (being picked) identifier.")
      )
    )
}

final case class EntityDroppedChange(modelId: String, traceId: String, entityId: String, targetId: String) extends ModelChange {
  val demuxer = "entity-dropped"
}
object EntityDroppedChange {
  implicit val format: Format[EntityDroppedChange] = Json.format
  val SchemaType: ObjectType[Unit, EntityDroppedChange] =
    ObjectType (
      "EntityDroppedChangeType",
      "Entity dropped change message.",
      interfaces[Unit, EntityDroppedChange](ModelChange.SchemaType),
      fields[Unit, EntityDroppedChange](
        Field("modelId", StringType, description = Some("Model identifier."), resolve = _.value.modelId),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("entityId", StringType, description = Some("Entity (doing the dropping) identifier."), resolve = _.value.entityId),
        Field("targetId", StringType, description = Some("Target entity (being dropped) identifier."), resolve = _.value.targetId)
      )
    )
  val SchemaInputType: InputObjectType[EntityDroppedChange] =
    InputObjectType[EntityDroppedChange] (
      "EntityDroppedChangeInputType",
      "Entity dropped change message.",
      List(
        InputField("modelId", StringType, "Model identifier."),
        InputField("traceId", StringType, "Trace identifier."),
        InputField("entityId", StringType, "Entity (doing the dropping) identifier."),
        InputField("targetId", StringType, "Target entity (being dropped) identifier.")
      )
    )
}

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
  val SchemaType: InterfaceType[Unit, ModelChange] =
    InterfaceType (
      "ModelChangeType",
      "Model change message.",
      fields[Unit, ModelChange](
        Field("modelId", StringType, description = Some("Model identifier."), resolve = _.value.modelId)
      )
    )
  val SchemaInputType: InputObjectType[ModelChange] =
    InputObjectType[ModelChange] (
      "ModelChangeInputType",
      "Model change input.",
      List(
        InputField("modelId", StringType, "Model identifier.")
      )
    )
}
