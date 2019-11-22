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
package com.loudflow.util

import play.api.libs.json.{Format, Json}

sealed trait DiscreteOrContinuous[T] {
  def contains(span: Span[T], value: T, inclusive: Boolean = true): Boolean
  def center(span: Span[T]): T
  def pick(span: Span[T], random: JavaRandom): T
  def toInt(span: Span[T]): Span[Int]
}

object DiscreteOrContinuous {
  implicit object Discrete extends DiscreteOrContinuous[Int] {
    def contains(span: Span[Int], value: Int, inclusive: Boolean = true): Boolean =
      if (inclusive) value >= span.min && value <= span.max else value > span.min && value < span.max
    def center(span: Span[Int]): Int = Math.round((span.max - span.min) / 2)
    def pick(span: Span[Int], random: JavaRandom): Int = span.min + random.nextInt((span.max - span.min) + 1)
    def toInt(span: Span[Int]): Span[Int] = span
  }
  implicit object Continuous extends DiscreteOrContinuous[Double] {
    def contains(span: Span[Double], value: Double, inclusive: Boolean = true): Boolean =
      if (inclusive) value >= span.min && value <= span.max else value > span.min && value < span.max
    def center(span: Span[Double]): Double = 0.5 * (span.max - span.min)
    def pick(span: Span[Double], random: JavaRandom): Double = span.min + random.nextDouble * (span.max - span.min)
    def toInt(span: Span[Double]): Span[Int] = Span(Math.round(span.min - 0.5).toInt, Math.round(span.max + 0.5).toInt)
  }
}

final case class Span[T](min: T, max: T)(implicit val dc: DiscreteOrContinuous[T]) {
  def contains(value: T, inclusive: Boolean = true): Boolean = dc.contains(this, value, inclusive)
  def center: T = dc.center(this)
  def pick(random: JavaRandom): T = dc.pick(this, random)
  def toInt: Span[Int] = dc.toInt(this)
}
object Span {
  implicit val intFormat: Format[Span[Int]] = Json.format
  implicit val doubleFormat: Format[Span[Double]] = Json.format
}
