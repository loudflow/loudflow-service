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

import com.loudflow.domain.model.entity.{Entity, EntityOptions}
import com.loudflow.domain.model.graph.{GraphLogic, GraphModelState}
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.slf4j.{Logger, LoggerFactory}

class GraphLogicTest extends FunSuite with BeforeAndAfter {

  private final val log: Logger = LoggerFactory.getLogger(classOf[GraphLogicTest])

  val id: String = UUID.randomUUID().toString
  val xCount = 10
  val yCount = 10
  val gridProperties = GridProperties(xCount, yCount)
  val graphProperties = GraphProperties(Some(gridProperties))
  val modelProperties = ModelProperties(ModelType.Graph, Some(graphProperties))
  val state: GraphModelState = GraphModelState(id, modelProperties)

  val entity0 = Entity("agent::random", EntityOptions())

  def asciiMapper(entity: Option[Entity]): String = entity match {
    case Some(e) => e.kind match {
      case "agent::random" => "A"
      case "thing::tile" => "T"
      case _ => "?"
    }
    case None => " "
  }

  test("build position layer from grid properties") {
    val graph = GraphLogic.buildPositionLayer(gridProperties)
    GraphLogic.displayGridAsAscii(graph, gridProperties, "EMPTY GRID", asciiMapper).unsafeRunSync()
    assert(graph.nonEmpty)
    assert(graph.nodes.length == xCount * yCount)
    assert(graph.edges.length == (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

  test("add entity") {
    val graph = GraphLogic.buildPositionLayer(gridProperties)
    GraphLogic.addEntity(entity0, Position(1, 1), graph)
    GraphLogic.displayGridAsAscii(graph, gridProperties, "ADD AGENT TO (1,1)", asciiMapper).unsafeRunSync()
    assert(graph.nonEmpty)
    assert(graph.nodes.length == 1 + xCount * yCount)
    assert(graph.edges.length == 1 + (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

  test("move entity") {
    val graph = GraphLogic.buildPositionLayer(gridProperties)
    GraphLogic.addEntity(entity0, Position(1, 1), graph)
    GraphLogic.moveEntity(entity0, Position(5, 5), graph)
    GraphLogic.displayGridAsAscii(graph, gridProperties, "MOVE AGENT TO (5,5)", asciiMapper).unsafeRunSync()
    assert(graph.nonEmpty)
    assert(graph.nodes.length == 1 + xCount * yCount)
    assert(graph.edges.length == 1 + (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

  test("remove entity") {
    val graph = GraphLogic.buildPositionLayer(gridProperties)
    GraphLogic.addEntity(entity0, Position(1, 1), graph)
    GraphLogic.displayGridAsAscii(graph, gridProperties, "ADD AGENT TO (1,1)", asciiMapper).unsafeRunSync()
    assert(graph.nonEmpty)
    assert(graph.nodes.length == 1 + xCount * yCount)
    assert(graph.edges.length == 1 + (xCount - 1) * yCount + (yCount - 1) * xCount)
    GraphLogic.removeEntity(entity0, graph)
    GraphLogic.displayGridAsAscii(graph, gridProperties, "REMOVE AGENT", asciiMapper).unsafeRunSync()
    assert(graph.nodes.length == xCount * yCount)
    assert(graph.edges.length == (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

}
