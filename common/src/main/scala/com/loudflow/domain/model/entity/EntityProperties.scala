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
import com.loudflow.util.Span
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

object EntityCategory {

  sealed trait Value

  final object Agent extends Value {
    val demuxer = "agent"
  }

  final object Thing extends Value {
    val demuxer = "thing"
  }

  val values: Set[Value] = Set(Agent, Thing)

  def fromString(value: String): Option[Value] = value.toLowerCase match {
    case "agent" => Some(Agent)
    case "thing" => Some(Thing)
    case _ => None
  }

  implicit val reads: Reads[Value] = Reads { json =>
    (JsPath \ "demuxer").read[String].reads(json).flatMap {
      case "agent" => JsSuccess(Agent)
      case "thing" => JsSuccess(Thing)
      case other => JsError(s"Read Entity.Type failed due to unknown enumeration value $other.")
    }
  }
  implicit val writes: Writes[Value] = Writes {
    case Agent => JsObject(Seq("demuxer" -> JsString("agent")))
    case Thing => JsObject(Seq("demuxer" -> JsString("thing")))
  }
}

final case class PopulationProperties(range: Span[Int], growth: Option[Int], lifeSpanRange: Option[Span[Int]]) {
  require(growth.isDefined && growth.get >= 0, "Invalid argument 'growth' for PopulationProperties..")
  require(lifeSpanRange.isDefined && lifeSpanRange.get.min > 0, "Invalid argument 'lifeSpanRange' for PopulationProperties..")
}
object PopulationProperties { implicit val format: Format[PopulationProperties] = Json.format }

final case class ClusterProperties(size: Span[Int] = Span(0, 0), step: Int = 0, locations: Array[Position] = Array.empty, is3D: Boolean = false, autoGenerate: Boolean = true) {
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

final case class InteractionProperties(target: Participant, trigger: Option[Participant], result: InteractionResult.Value = InteractionResult.ActorBlocked, score: Option[Int] = None, scoreDecayRate: Option[Int] = None)
object InteractionProperties { implicit val format: Format[InteractionProperties] = Json.format }

final case class Participant(kind: String, score: Option[Int], scoreDecayRate: Option[Int])
object Participant { implicit val format: Format[Participant] = Json.format }

object InteractionResult {

  sealed trait Value
  final case object Share extends Value {
    val valueType = "share"
  }
  final case object ActorBlocked extends Value {
    val valueType = "actor-blocked"
  }
  final case object TargetMoved extends Value {
    val valueType = "target-moved"
  }
  final case object ActorRemoved extends Value {
    val valueType = "actor-removed"
  }
  final case object TargetRemoved extends Value {
    val valueType = "target-removed"
  }
  final case object BothRemoved extends Value {
    val valueType = "both-removed"
  }

  val values: Seq[Value] = Seq(Share, ActorBlocked, TargetMoved, ActorRemoved, TargetRemoved, BothRemoved)

  implicit val reads: Reads[Value] = Reads { json =>
    (JsPath \ "valueType").read[String].reads(json).flatMap {
      case "share" => JsSuccess(Share)
      case "actor-blocked" => JsSuccess(ActorBlocked)
      case "target-moved" => JsSuccess(TargetMoved)
      case "actor-removed" => JsSuccess(ActorRemoved)
      case "target-removed" => JsSuccess(TargetRemoved)
      case "both-removed" => JsSuccess(BothRemoved)
      case other => JsError(s"Read ConcurrenceOutcome failed due to unknown enumeration value $other.")
    }
  }
  implicit val writes: Writes[Value] = Writes {
    case Share => JsObject(Seq("valueType" -> JsString("share")))
    case ActorBlocked => JsObject(Seq("valueType" -> JsString("actor-blocked")))
    case TargetMoved => JsObject(Seq("valueType" -> JsString("target-moved")))
    case ActorRemoved => JsObject(Seq("valueType" -> JsString("actor-removed")))
    case TargetRemoved => JsObject(Seq("valueType" -> JsString("target-removed")))
    case BothRemoved => JsObject(Seq("valueType" -> JsString("both-removed")))
  }

}
