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
import sangria.macros.derive.{DocumentField, ObjectTypeDescription, deriveObjectType}
import sangria.schema.{InputField, InputObjectType, IntType, ObjectType}

final case class IntSpan(min: Int, max: Int) {
  def contains(value: Int, inclusive: Boolean = true): Boolean = if (inclusive) value >= min && value <= max else value > min && value < max
  def center: Int = Math.round((max - min) / 2)
  def pick(random: JavaRandom): Int = min + random.nextInt((max - min) + 1)
}
object IntSpan {
  implicit val format: Format[IntSpan] = Json.format
  val SchemaType: ObjectType[Unit, IntSpan] =
    deriveObjectType[Unit, IntSpan](
      ObjectTypeDescription("Data structure defining integer min/max limits with some helper functions."),
      DocumentField("min", "Minimum value of integer span."),
      DocumentField("max", "Maximum value of integer span."))
  val SchemaInputType: InputObjectType[IntSpan] =
    InputObjectType[IntSpan] (
      "IntSpanInputType",
      "Data structure defining integer min/max limits with some helper functions.",
      List(
        InputField("min", IntType, "Minimum value of integer span."),
        InputField("max", IntType, "Maximum value of integer span.")
      )
    )
}

final case class DoubleSpan(min: Double, max: Double) {
  def contains(value: Double, inclusive: Boolean = true): Boolean = if (inclusive) value >= min && value <= max else value > min && value < max
  def center: Double = 0.5 * (max - min)
  def pick(random: JavaRandom): Double = min + random.nextDouble * (max - min)
}
object DoubleSpan {
  implicit val format: Format[DoubleSpan] = Json.format
  val SchemaType: ObjectType[Unit, DoubleSpan] =
    deriveObjectType[Unit, DoubleSpan](
      ObjectTypeDescription("Data structure defining double min/max limits with some helper functions."),
      DocumentField("min", "Minimum value of double span."),
      DocumentField("max", "Maximum value of double span."))
}
