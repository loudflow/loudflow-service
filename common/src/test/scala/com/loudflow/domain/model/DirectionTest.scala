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

import com.loudflow.util.DoubleSpan
import org.scalatest.{BeforeAndAfter, FunSuite}
import com.typesafe.scalalogging.Logger

class DirectionTest extends FunSuite with BeforeAndAfter {

  private final val log = Logger[DirectionTest]

  private final val origin = Position(0, 0)
  log.trace(s"ORIGIN: $origin")

  test("step in direction: north, step:1") {
    val p = Direction.stepInDirection(origin, Direction.North, 1)
    log.trace(s"STEP NORTH: $p")
    assert(p.get.x == 0.0 && p.get.y == 1.0)
  }
  test("step in direction: North, step:2, span:(0,1)") {
    val p = Direction.stepInDirection(origin, Direction.North, 2, Some(DoubleSpan(0, 1.0)), Some(DoubleSpan(0, 1.0)))
    log.trace(s"2 STEPS NORTH WITH SPAN(0,1): $p")
    assert(p.isEmpty)
  }
  test("step in direction: North, step:2, span:(0,2)") {
    val p = Direction.stepInDirection(origin, Direction.North, 2, Some(DoubleSpan(0, 2.0)), Some(DoubleSpan(0, 2.0)))
    log.trace(s"2 STEPS NORTH WITH SPAN(0,2): $p")
    assert(p.get.x == 0.0 && p.get.y == 2.0)
  }
  test("step in direction: NorthWest, step:1") {
    val p = Direction.stepInDirection(origin, Direction.NorthWest, 1)
    log.trace(s"STEP NORTHWEST: $p")
    assert(p.get.x == -1.0 && p.get.y == 1.0)
  }
  test("step in direction: NorthWestBelow, step:1") {
    val p = Direction.stepInDirection(origin, Direction.NorthWestBelow, 1)
    log.trace(s"STEP NORTHWEST-BELOW: $p")
    assert(p.get.x == -1.0 && p.get.y == 1.0 && p.get.z == -1.0)
  }

}
