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
import com.typesafe.scalalogging.Logger

class GraphModelStateTest extends FunSuite with BeforeAndAfter {

  private final val log = Logger[GraphModelStateTest]

  val id: String = UUID.randomUUID().toString
  val xCount = 10
  val yCount = 10
  val gridProperties: GridProperties = GridProperties(xCount, yCount)
  val graphProperties: GraphProperties = GraphProperties(Some(gridProperties))
  val modelProperties: ModelProperties = ModelProperties(ModelType.GRAPH, Some(graphProperties))

  val entity0: Entity = Entity("agent::random", EntityOptions())

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

  test("add") {
    val state = GraphModelState.create(id, modelProperties)
    val newState = GraphModelState.add("agent::random", None, None, state)
    GraphHelper.displayGridAsAscii(newState.graph, gridProperties, "STATE WITH ONE RANDOMLY-PLACED AGENT", asciiMapper).unsafeRunSync()
    assert(newState.graph.nonEmpty)
    assert(newState.graph.nodes.length == 1 + xCount * yCount)
    assert(newState.graph.edges.length == 1 + (xCount - 1) * yCount + (yCount - 1) * xCount)
  }

  test("move") {
    val state = GraphModelState.create(id, modelProperties)
    val newState = GraphModelState.add("agent::random", None, Some(Position(5,5)), state)
    val entity = newState.entities.head
    GraphHelper.displayGridAsAscii(newState.graph, gridProperties, "STATE WITH ONE AGENT PLACED AT (5,5)", asciiMapper).unsafeRunSync()
    assert(newState.entityPositions(entity.id).head.id == Position(5, 5).id)
    val newerState = GraphModelState.move(entity.id, Some(Position(6,6)), state)
    GraphHelper.displayGridAsAscii(newerState.graph, gridProperties, "STATE WITH AGENT MOVED TO (6,6)", asciiMapper).unsafeRunSync()
    assert(newerState.entityPositions(entity.id).head.id == Position(6, 6).id)
  }

  test("remove") {
    val state = GraphModelState.create(id, modelProperties)
    val newState = GraphModelState.add("agent::random", None, Some(Position(5,5)), state)
    val entity = newState.entities.head
    assert(newState.entityPositions(entity.id).head.id == Position(5, 5).id)
    val newerState = GraphModelState.remove(entity.id, newState)
    assert(newerState.entities.isEmpty)
  }

  test("destroy") {
    val state = GraphModelState.create(id, modelProperties)
    GraphModelState.add("agent::random", None, Some(Position(5,5)), state)
    val entity = state.entities.head
    assert(state.entityPositions(entity.id).head.id == Position(5, 5).id)
    val newState = GraphModelState.destroy(state)
    assert(newState.entities.isEmpty)
    assert(newState.positions.isEmpty)
    assert(newState.attachments.isEmpty)
    assert(newState.connections.isEmpty)
  }

}
