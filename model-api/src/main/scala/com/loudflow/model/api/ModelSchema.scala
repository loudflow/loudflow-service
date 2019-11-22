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
package com.loudflow.model.api

import com.loudflow.domain.model.entity.EntityProperties
import sangria.schema._
import com.loudflow.domain.model.{GraphProperties, GridProperties, ModelProperties, ModelType}

object ModelSchema {

  val ModelTypeEnum =
    EnumType (
      "ModelType",
      Some("Type of model."),
      List (
        EnumValue("GRAPH", value = ModelType.GRAPH, description = Some("Graph-based model."))
      )
    )

  val GridProperties =
    ObjectType (
      "GridProperties",
      "Properties for defining a graph-based model with grid-based positions.",
      fields[Unit, GridProperties](
        Field("rows", IntType, description = Some("Number of rows in grid."), resolve = _.value.rows),
        Field("cols", IntType, description = Some("Number of columns in grid."), resolve = _.value.cols),
        Field("layers", IntType, description = Some("Number of layers if 3D grid."), resolve = _.value.layers),
        Field("cardinalOnly", BooleanType, description = Some("Flag for connecting grid positions in cardinal directions only."), resolve = _.value.cardinalOnly)
      )
    )

  val GraphProperties =
    ObjectType (
      "GraphProperties",
      "Properties for defining a graph-based model.",
      fields[Unit, GraphProperties](
        Field("grid", OptionType(GridProperties), description = Some("Grid properties for graph."), resolve = _.value.grid)
      )
    )

  val ModelProperties =
    ObjectType (
      "ModelProperties",
      "Model properties.",
      fields[Unit, ModelProperties](
        Field("modelType", ModelTypeEnum, description = Some("Type of model."), resolve = _.value.modelType),
        Field("graph", OptionType(GraphProperties), description = Some("Graph properties for model."), resolve = _.value.graph),
        Field("seed", LongType, description = Some("Random number generator seed for the model."), resolve = _.value.seed),
        Field("entities", ListType(EntityProperties), description = Some("List of entity properties for each entity kind used in the model."), resolve = _.value.entities)
      )
    )

}
