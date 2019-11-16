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

import play.api.libs.json._

import com.loudflow.domain.model.Graph.Node

final case class Position(x: Double, y: Double, z: Double = 0.0, index: Int = 0) extends Node {
  val margin = 0.001
  def distanceFrom(position: Position): Double = Math.sqrt(squareDiff(this.x, position.x) + squareDiff(this.y, position.y) + squareDiff(this.z, position.z))
  def shift(position: Position): Position = Position(diff(position.x, this.x), diff(position.y, this.y), diff(position.z, this.z))
  def isOrigin: Boolean = isWithinMargin(this.x) && isWithinMargin(this.y) && isWithinMargin(this.z)
  private def diff(value1: Double, value2: Double): Double = value2 - value1
  private def squareDiff(value1: Double, value2: Double): Double = diff(value1, value2) * diff(value1, value2)
  private def isWithinMargin(value: Double): Boolean = value >= -margin && value <= margin
}
object Position {
  implicit val format: Format[Position] = Json.format
  final case class Boundaries(x: Span[Double], y: Span[Double], z: Option[Span[Double]] = None)
}
