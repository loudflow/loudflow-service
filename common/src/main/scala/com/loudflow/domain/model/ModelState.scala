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

import com.loudflow.domain.model.entity.{Entity, EntityOptions, EntityProperties}
import com.loudflow.domain.model.graph.GraphModelState
import com.loudflow.util.JavaRandom
import play.api.libs.json._
import sangria.schema.{Field, InterfaceType, ListType, LongType, StringType, fields}

trait ModelState {
  def demuxer: String
  def id: String
  def properties: ModelProperties
  def seed: Long
  def entities: Set[Entity]
  def isEmpty: Boolean
  def entityProperties(kind: String): Option[EntityProperties] = properties.entityProperties(kind)
  def getEntity(entityId: String): Option[Entity] = entities.find(_.id == entityId)
  def findEntities(kind: String): Set[Entity] = entities.filter(_.kind == kind)
  val random: JavaRandom = new JavaRandom(seed)
}

object ModelState {

  implicit val reads: Reads[ModelState] = {
    (JsPath \ "demuxer").read[String].flatMap {
      case "graph" => implicitly[Reads[GraphModelState]].map(identity)
      case other => Reads(_ => JsError(s"Read Model.State failed due to unknown type $other."))
    }
  }

  implicit val writes: Writes[ModelState] = Writes { obj =>
    val (jsValue, demuxer) = obj match {
      case command: GraphModelState => (Json.toJson(command)(GraphModelState.format), "graph")
    }
    jsValue.transform(JsPath.json.update((JsPath \ 'demuxer).json.put(JsString(demuxer)))).get
  }

  val SchemaType =
    InterfaceType (
      "ModelStateType",
      "Model state.",
      fields[Unit, ModelState](
        Field("id", StringType, description = Some("Model identifier."), resolve = _.value.id),
        Field("seed", LongType, description = Some("Model seed."), resolve = _.value.seed),
        Field("properties", ModelProperties.SchemaType, description = Some("Model properties."), resolve = _.value.properties),
        Field("entities", ListType(Entity.SchemaType), description = Some("Model entities."), resolve = _.value.entities.toSeq)
      )
    )

  def apply(id: String, properties: ModelProperties): ModelState = properties.modelType match {
    case ModelType.GRAPH => GraphModelState(id, properties)
  }

  /* ************************************************************************
     PUBLIC METHODS
  ************************************************************************ */

  def create(id: String, properties: ModelProperties): ModelState = properties.modelType match {
    case ModelType.GRAPH => GraphModelState.create(id, properties)
  }

  def destroy(state: ModelState): ModelState = state match {
    case s: GraphModelState => GraphModelState.destroy(s)
  }

  def update(change: ModelChange, state: ModelState): ModelState = state match {
    case s: GraphModelState => GraphModelState.update(change, s)
  }

  def add(kind: String, options: Option[EntityOptions] = None, position: Option[Position] = None, state: ModelState): ModelState = state match {
    case s: GraphModelState => GraphModelState.add(kind, options, position, s)
  }

  def move(entityId: String, position: Option[Position], state: ModelState): ModelState = state match {
    case s: GraphModelState => GraphModelState.move(entityId, position, s)
  }

  def remove(entityId: String, state: ModelState): ModelState = state match {
    case s: GraphModelState => GraphModelState.remove(entityId, s)
  }

}

