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

import com.wix.accord.dsl._
import com.wix.accord.transform.ValidationTransform
import play.api.libs.json._

trait ModelState {
  def demuxer: String
  def id: String
  def properties: ModelProperties
  def entities: Set[Entity]
  def isEmpty: Boolean
  def entityProperties(entityType: EntityType.Value, kind: String): Option[EntityProperties] = properties.entityProperties(entityType, kind)
}

object ModelState {
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[ModelState] = validator { properties =>
    properties.properties is valid
  }
  def apply(id: String, properties: ModelProperties): ModelState = properties.modelType match {
    case ModelType.Graph => GraphState(id, properties)
  }
  implicit val reads: Reads[ModelState] = {
    (JsPath \ "demuxer").read[String].flatMap {
      case "graph" => implicitly[Reads[GraphState]].map(identity)
      case other => Reads(_ => JsError(s"Read Model.State failed due to unknown type $other."))
    }
  }
  implicit val writes: Writes[ModelState] = Writes { obj =>
    val (jsValue, demuxer) = obj match {
      case command: GraphState => (Json.toJson(command)(GraphState.format), "graph")
    }
    jsValue.transform(JsPath.json.update((JsPath \ 'demuxer).json.put(JsString(demuxer)))).get
  }
}

