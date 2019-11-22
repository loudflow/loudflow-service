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
  require(modelType == ModelType.GRAPH && graph.isDefined, "Invalid argument 'graph' for ModelProperties.")
  def entityProperties(kind: String): Option[EntityProperties] = entities.find(_.kind == kind)
}

object ModelProperties { implicit val format: Format[ModelProperties] = Json.format }

object ModelType extends Enumeration {
  type ModelType = Value
  val GRAPH: ModelType.Value = Value
  implicit val format: Format[ModelType.Value] = Json.formatEnum(this)
}

final case class GraphProperties(grid: Option[GridProperties])
object GraphProperties { implicit val format: Format[GraphProperties] = Json.format }

final case class GridProperties(rows: Int, cols: Int, layers: Int = 0, cardinalOnly: Boolean = true) {
  require(rows > 0, "Invalid argument 'rows' for GridProperties.")
  require(cols > 0, "Invalid argument 'cols' for GridProperties.")
  require(layers >= 0, "Invalid argument 'layers' for GridProperties.")
}
object GridProperties { implicit val format: Format[GridProperties] = Json.format }
