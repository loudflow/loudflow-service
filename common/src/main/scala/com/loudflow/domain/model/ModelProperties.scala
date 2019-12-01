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
import sangria.schema.{BooleanType, EnumType, EnumValue, Field, InputField, InputObjectType, IntType, ListInputType, ListType, LongType, ObjectType, OptionInputType, OptionType, fields}

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

object ModelProperties {
  implicit val format: Format[ModelProperties] = Json.format
  val SchemaType =
    ObjectType (
      "ModelPropertiesType",
      "Model properties.",
      fields[Unit, ModelProperties](
        Field("modelType", ModelType.ModelTypeEnum, description = Some("Type of model."), resolve = _.value.modelType),
        Field("graph", OptionType(GraphProperties.SchemaType), description = Some("Graph properties for model."), resolve = _.value.graph),
        Field("seed", LongType, description = Some("Random number generator seed for the model."), resolve = _.value.seed),
        Field("entities", ListType(EntityProperties.SchemaType), description = Some("List of entity properties for each entity kind used in the model."), resolve = _.value.entities.toSeq)
      )
    )
  val SchemaInputType: InputObjectType[ModelProperties] =
    InputObjectType[ModelProperties] (
      "ModelPropertiesInputType",
      "Model properties input.",
      List(
        InputField("modelType", ModelType.ModelTypeEnum, "Type of model."),
        InputField("graph", OptionInputType(GraphProperties.SchemaInputType), "Graph properties for model."),
        InputField("seed", LongType, "Random number generator seed for the model."),
        InputField("entities", ListInputType(EntityProperties.SchemaInputType), "List of entity properties for each entity kind used in the model.")
      )
    )
}

object ModelType extends Enumeration {
  type ModelType = Value
  val GRAPH: ModelType.Value = Value
  implicit val format: Format[ModelType.Value] = Json.formatEnum(this)
  val ModelTypeEnum =
    EnumType (
      "ModelTypeEnum",
      Some("Type of model."),
      List (
        EnumValue("GRAPH", value = ModelType.GRAPH, description = Some("Graph-based model."))
      )
    )
}

final case class GraphProperties(grid: Option[GridProperties])
object GraphProperties {
  implicit val format: Format[GraphProperties] = Json.format
  val SchemaType =
    ObjectType (
      "GraphPropertiesType",
      "Properties for defining a graph-based model.",
      fields[Unit, GraphProperties](
        Field("grid", OptionType(GridProperties.SchemaType), description = Some("Grid properties for graph."), resolve = _.value.grid)
      )
    )
  val SchemaInputType: InputObjectType[GraphProperties] =
    InputObjectType[GraphProperties] (
      "GraphPropertiesInputType",
      "Properties for defining a graph-based model.",
      List(
        InputField("grid", OptionInputType(GridProperties.SchemaInputType), "Grid properties for graph.")
      )
    )
}

final case class GridProperties(rows: Int, cols: Int, layers: Int = 0, cardinalOnly: Boolean = true) {
  require(rows > 0, "Invalid argument 'rows' for GridProperties.")
  require(cols > 0, "Invalid argument 'cols' for GridProperties.")
  require(layers >= 0, "Invalid argument 'layers' for GridProperties.")
}
object GridProperties {
  implicit val format: Format[GridProperties] = Json.format
  val SchemaType =
    ObjectType (
      "GridPropertiesType",
      "Properties for defining a graph-based model with grid-based positions.",
      fields[Unit, GridProperties](
        Field("rows", IntType, description = Some("Number of rows in grid."), resolve = _.value.rows),
        Field("cols", IntType, description = Some("Number of columns in grid."), resolve = _.value.cols),
        Field("layers", IntType, description = Some("Number of layers if 3D grid."), resolve = _.value.layers),
        Field("cardinalOnly", BooleanType, description = Some("Flag for connecting grid positions in cardinal directions only."), resolve = _.value.cardinalOnly)
      )
    )
  val SchemaInputType: InputObjectType[GridProperties] =
    InputObjectType[GridProperties] (
      "GridPropertiesInputType",
      "Properties for defining a graph-based model with grid-based positions.",
      List(
        InputField("rows", IntType, "Number of rows in grid."),
        InputField("cols", IntType, "Number of columns in grid."),
        InputField("layers", IntType, "Number of layers if 3D grid."),
        InputField("cardinalOnly", BooleanType, "Flag for connecting grid positions in cardinal directions only.")
      )
    )
}
