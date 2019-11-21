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
import com.loudflow.domain.model.graph.{GraphHelper, GraphModelState}
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.slf4j.{Logger, LoggerFactory}

class GraphModelStateTest extends FunSuite with BeforeAndAfter {

  private final val log: Logger = LoggerFactory.getLogger(classOf[GraphModelStateTest])

  val id: String = UUID.randomUUID().toString
  val xCount = 10
  val yCount = 10
  val gridProperties = GridProperties(xCount, yCount)
  val graphProperties = GraphProperties(Some(gridProperties))
  val modelProperties = ModelProperties(ModelType.Graph, Some(graphProperties))

  val entity0 = Entity("agent::random", EntityOptions())

  def asciiMapper(entity: Option[Entity]): String = entity match {
    case Some(e) => e.kind match {
      case "agent::random" => "A"
      case "thing::tile" => "T"
      case _ => "?"
    }
    case None => " "
  }

  test("create") {
    val state = GraphModelState.create(id, modelProperties)
    GraphHelper.displayGridAsAscii(state.graph, gridProperties, "EMPTY STATE", asciiMapper).unsafeRunSync()
    assert(state.graph.nonEmpty)
    assert(state.graph.nodes.length == xCount * yCount)
    assert(state.graph.edges.length == (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

  test("addcreate") {
    val state = GraphModelState.create(id, modelProperties)
    GraphModelState.add("agent::random", None, None, state)
    GraphHelper.displayGridAsAscii(state.graph, gridProperties, "STATE WITH ONE RANDOMLY-PLACED AGENT", asciiMapper).unsafeRunSync()
    assert(state.graph.nonEmpty)
    assert(state.graph.nodes.length == 1 + xCount * yCount)
    assert(state.graph.edges.length == 1 + (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

}