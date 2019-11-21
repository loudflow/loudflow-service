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
import com.loudflow.domain.model.graph.GraphHelper
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.slf4j.{Logger, LoggerFactory}

class GraphHelperTest extends FunSuite with BeforeAndAfter {

  private final val log: Logger = LoggerFactory.getLogger(classOf[GraphHelperTest])

  val id: String = UUID.randomUUID().toString
  val xCount = 10
  val yCount = 10
  val gridProperties = GridProperties(xCount, yCount)

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
    val graph = GraphHelper.buildPositionLayer(gridProperties)
    GraphHelper.displayGridAsAscii(graph, gridProperties, "EMPTY GRID", asciiMapper).unsafeRunSync()
    assert(graph.nonEmpty)
    assert(graph.nodes.length == xCount * yCount)
    assert(graph.edges.length == (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

  test("add entity") {
    val graph = GraphHelper.buildPositionLayer(gridProperties)
    GraphHelper.addEntity(entity0, Position(1, 1), graph)
    GraphHelper.displayGridAsAscii(graph, gridProperties, "ADD AGENT TO (1,1)", asciiMapper).unsafeRunSync()
    assert(graph.nonEmpty)
    assert(graph.nodes.length == 1 + xCount * yCount)
    assert(graph.edges.length == 1 + (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

  test("move entity") {
    val graph = GraphHelper.buildPositionLayer(gridProperties)
    GraphHelper.addEntity(entity0, Position(1, 1), graph)
    GraphHelper.moveEntity(entity0, Position(5, 5), graph)
    GraphHelper.displayGridAsAscii(graph, gridProperties, "MOVE AGENT TO (5,5)", asciiMapper).unsafeRunSync()
    assert(graph.nonEmpty)
    assert(graph.nodes.length == 1 + xCount * yCount)
    assert(graph.edges.length == 1 + (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

  test("remove entity") {
    val graph = GraphHelper.buildPositionLayer(gridProperties)
    GraphHelper.addEntity(entity0, Position(1, 1), graph)
    GraphHelper.displayGridAsAscii(graph, gridProperties, "ADD AGENT TO (1,1)", asciiMapper).unsafeRunSync()
    assert(graph.nonEmpty)
    assert(graph.nodes.length == 1 + xCount * yCount)
    assert(graph.edges.length == 1 + (xCount - 1) * yCount + (yCount - 1) * xCount)
    GraphHelper.removeEntity(entity0, graph)
    GraphHelper.displayGridAsAscii(graph, gridProperties, "REMOVE AGENT", asciiMapper).unsafeRunSync()
    assert(graph.nodes.length == xCount * yCount)
    assert(graph.edges.length == (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

}
