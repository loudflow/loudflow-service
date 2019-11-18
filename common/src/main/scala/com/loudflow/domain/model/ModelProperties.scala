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

import java.util.UUID

import com.loudflow.util.JavaRandom
import com.wix.accord.dsl._
import com.wix.accord.transform.ValidationTransform
import play.api.libs.json._

final case class ModelProperties
(
  modelType: ModelType.Value,
  id: String = UUID.randomUUID().toString,
  seed: Long = JavaRandom.seedUniquifier ^ System.nanoTime,
  graph: Option[GraphProperties] = None,
  entities: Set[EntityProperties] = Set.empty
) {
  def entityProperties(entityType: EntityType.Value, kind: String): Option[EntityProperties] =
    entities.find(entity => entity.entityType == entityType && entity.kind == kind)
}

object ModelProperties {
  implicit val format: Format[ModelProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[ModelProperties] = validator { properties =>
    if (properties.modelType == ModelType.Graph) {
      properties.graph is notEmpty
    }
    properties.entities.each is valid
  }
}

final case class GraphProperties(grid: Option[GridProperties])
object GraphProperties {
  implicit val format: Format[GraphProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[GraphProperties] = validator { properties =>
    properties.grid.each is valid
  }
}

final case class GridProperties(xCount: Int, yCount: Int, zCount: Int = 0, cardinalOnly: Boolean = true)
object GridProperties {
  implicit val format: Format[GridProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[GridProperties] = validator { properties =>
    properties.xCount should be > 0
    properties.yCount should be > 0
    properties.zCount should be >= 0
  }
}

object ModelType {

  sealed trait Value

  final case object Graph extends Value {
    val demuxer = "graph"
  }

  val values: Set[Value] = Set(Graph)

  implicit val reads: Reads[Value] = Reads { json =>
    (JsPath \ "demuxer").read[String].reads(json).flatMap {
      case "graph" => JsSuccess(Graph)
      case other => JsError(s"Read Model.Type failed due to unknown enumeration value $other.")
    }
  }
  implicit val writes: Writes[Value] = Writes {
    case Graph => JsObject(Seq("demuxer" -> JsString("graph")))
  }
}

