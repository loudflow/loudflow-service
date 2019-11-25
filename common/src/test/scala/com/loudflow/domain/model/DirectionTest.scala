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
import org.slf4j.{Logger, LoggerFactory}

class DirectionTest extends FunSuite with BeforeAndAfter {

  // private final val log: Logger = LoggerFactory.getLogger(classOf[DirectionTest])

  val origin = Position(0, 0)

  test("step in direction: north, step:1") {
    val p = Direction.stepInDirection(origin, Direction.North, 1)
    assert(p.get.x == 0.0 && p.get.y == 1.0)
  }
  test("step in direction: North, step:2, span:(0,1)") {
    val p = Direction.stepInDirection(origin, Direction.North, 2, Some(DoubleSpan(0, 1.0)), Some(DoubleSpan(0, 1.0)))
    assert(p.isEmpty)
  }
  test("step in direction: NorthWest, step:1") {
    val p = Direction.stepInDirection(origin, Direction.NorthWest, 1)
    assert(p.get.x == -1.0 && p.get.y == 1.0)
  }
  test("step in direction: NorthWestBelow, step:1") {
    val p = Direction.stepInDirection(origin, Direction.NorthWestBelow, 1)
    assert(p.get.x == -1.0 && p.get.y == 1.0 && p.get.z == -1.0)
  }

}
