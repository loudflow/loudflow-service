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
package com.loudflow.domain.model.entity

import com.loudflow.domain.model.Position
import com.loudflow.util.IntSpan
import play.api.libs.json._

final case class EntityProperties
(
  kind: String,
  category: EntityCategory.Value,
  description: Option[String],
  population: PopulationProperties,
  motion: MotionProperties,
  proximity: Set[ProximityProperties],
  interactions: Set[InteractionProperties],
  cluster: Option[ClusterProperties],
  grouping: Option[Int]
) {
  require(grouping.isDefined && grouping.get > 0, "Invalid argument 'grouping' for EntityProperties.")
  def interactionProperties(target: String): Set[InteractionProperties] = interactions.filter(_.target.kind == target)
}
object EntityProperties { implicit val format: Format[EntityProperties] = Json.format }

object EntityCategory extends Enumeration {
  type EntityCategory = Value
  val AGENT: EntityCategory.Value = Value
  val THING: EntityCategory.Value = Value
  implicit val format: Format[EntityCategory.Value] = Json.formatEnum(this)
}

final case class PopulationProperties(range: IntSpan, growth: Option[Int], lifeSpanRange: Option[IntSpan]) {
  require(growth.isDefined && growth.get >= 0, "Invalid argument 'growth' for PopulationProperties..")
  require(lifeSpanRange.isDefined && lifeSpanRange.get.min > 0, "Invalid argument 'lifeSpanRange' for PopulationProperties..")
}
object PopulationProperties { implicit val format: Format[PopulationProperties] = Json.format }

final case class ClusterProperties(size: IntSpan = IntSpan(0, 0), step: Int = 0, locations: Array[Position] = Array.empty, is3D: Boolean = false, autoGenerate: Boolean = true) {
  require(autoGenerate && size.min > 0, "Invalid argument 'size.min' for ClusterProperties. For auto-generated clusters, minimum size must be greater than zero.")
  require(autoGenerate && step > 0, "Invalid argument 'step' for ClusterProperties. For auto-generated clusters, step must be greater than zero.")
  require(!autoGenerate && locations.nonEmpty, "Invalid argument 'locations' for ClusterProperties. For pre-defined clusters, locations cannot be empty.")
}
object ClusterProperties { implicit val format: Format[ClusterProperties] = Json.format }

final case class MotionProperties(distance: Int) {
  require(distance > 0, "Invalid argument 'distance' for MotionProperties.")
}
object MotionProperties { implicit val format: Format[MotionProperties] = Json.format }

final case class ProximityProperties(kind: String, distance: Int) {
  require(distance >= 0, "Invalid argument 'distance' for ProximityProperties.")
}
object ProximityProperties { implicit val format: Format[ProximityProperties] = Json.format }

final case class InteractionProperties(target: Participant, trigger: Option[Participant], result: InteractionResult.Value = InteractionResult.BLOCK_ACTOR, score: Option[Int] = None, scoreDecayRate: Option[Int] = None)
object InteractionProperties { implicit val format: Format[InteractionProperties] = Json.format }

final case class Participant(kind: String, score: Option[Int], scoreDecayRate: Option[Int])
object Participant { implicit val format: Format[Participant] = Json.format }

object InteractionResult extends Enumeration {
  type InteractionResult = Value
  val SHARE: InteractionResult.Value = Value
  val BLOCK_ACTOR: InteractionResult.Value = Value
  val MOVE_TARGET: InteractionResult.Value = Value
  val REMOVE_ACTOR: InteractionResult.Value = Value
  val REMOVE_TARGET: InteractionResult.Value = Value
  val REMOVE_BOTH: InteractionResult.Value = Value
  implicit val format: Format[InteractionResult.Value] = Json.formatEnum(this)
}
