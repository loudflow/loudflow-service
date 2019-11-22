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

import com.loudflow.domain.model.entity.EntityProperties
import com.loudflow.util.JavaRandom
import play.api.libs.json._

final case class ModelProperties
(
  modelType: ModelType.Value,
  graph: Option[GraphProperties] = None,
  seed: Long = JavaRandom.seedUniquifier ^ System.nanoTime,
  entities: Set[EntityProperties] = Set.empty
) {
  require(modelType == ModelType.Graph && graph.isDefined, "Invalid argument 'graph' for ModelProperties.")
  def entityProperties(kind: String): Option[EntityProperties] = entities.find(_.kind == kind)
}

object ModelProperties { implicit val format: Format[ModelProperties] = Json.format }

final case class GraphProperties(grid: Option[GridProperties])
object GraphProperties { implicit val format: Format[GraphProperties] = Json.format }

final case class GridProperties(xCount: Int, yCount: Int, zCount: Int = 0, cardinalOnly: Boolean = true) {
  require(xCount > 0, "Invalid argument 'xCount' for GridProperties.")
  require(yCount > 0, "Invalid argument 'yCount' for GridProperties.")
  require(zCount >= 0, "Invalid argument 'zCount' for GridProperties.")
}
object GridProperties { implicit val format: Format[GridProperties] = Json.format }

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
