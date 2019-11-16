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

import org.scalatest.{FunSuite, BeforeAndAfter}
import org.slf4j.{Logger, LoggerFactory}

class GraphTest extends FunSuite with BeforeAndAfter {

  private final val log: Logger = LoggerFactory.getLogger(classOf[GraphTest])

  val id: String = UUID.randomUUID().toString
  val gridProperties = GridProperties(10, 10)
  val graphProperties = GraphProperties(Some(gridProperties))
  val modelProperties = ModelProperties(ModelType.Graph, Some(graphProperties))
  val state: GraphState = GraphState(id, modelProperties)

/*
  test("new grid state has no occupants") {
    assert(Grid.cells[Occupied](state).isEmpty)
  }

  test("add one occupant") {
    val id: String = UUID.randomUUID().toString
    val occupant = Agent(id)
    val newState = Grid.add(occupant, Vacant(1, 1)).run(state).value._1
    val occupiedCells = Grid.cells[Occupied](newState)
    state.display(_ => "X")
    assert(occupiedCells.length == 1)
    assert(occupiedCells.head.occupant.id == occupant.id)
  }
*/

  test ("test graph state") (pending)

}
