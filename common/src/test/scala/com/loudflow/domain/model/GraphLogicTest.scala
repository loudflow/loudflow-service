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

import com.loudflow.domain.model.entity.Entity
import com.loudflow.domain.model.graph.{GraphLogic, GraphModelState}
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.slf4j.{Logger, LoggerFactory}

class GraphLogicTest extends FunSuite with BeforeAndAfter {

  private final val log: Logger = LoggerFactory.getLogger(classOf[GraphLogicTest])

  val id: String = UUID.randomUUID().toString
  val gridProperties = GridProperties(10, 10)
  val graphProperties = GraphProperties(Some(gridProperties))
  val modelProperties = ModelProperties(ModelType.Graph, Some(graphProperties))
  val state: GraphModelState = GraphModelState(id, modelProperties)

  def asciiMapper(entity: Option[Entity]): String = entity match {
    case Some(e) => e.kind match {
      case "agent::random" => "A"
      case "thing::tile" => "T"
      case _ => "?"
    }
    case None => "X"
  }

  test("build position layer from grid properties") {
    val graph = GraphLogic.buildPositionLayer(gridProperties)
    GraphLogic.displayGridAsAscii(graph, gridProperties, asciiMapper).unsafeRunSync()
    assert(graph.nonEmpty)
    assert(graph.nodes.length == 100)
  }
}
