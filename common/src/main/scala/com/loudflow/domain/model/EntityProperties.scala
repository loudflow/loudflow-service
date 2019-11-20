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

import com.loudflow.util.Span
import com.wix.accord.dsl._
import com.wix.accord.transform.ValidationTransform
import play.api.libs.json.{Format, JsError, JsObject, JsPath, JsString, JsSuccess, Json, Reads, Writes}

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
  def interactionProperties(target: String): Set[InteractionProperties] = interactions.filter(_.target.kind == target)
}

object EntityProperties {
  implicit val format: Format[EntityProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[EntityProperties] = validator { properties =>
    properties.population is valid
    properties.motion is valid
    properties.cluster.each is valid
    properties.grouping.each should be > 0
    properties.proximity.each is valid
  }
}

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

final case class PopulationProperties(range: Span[Int], growth: Option[Int], lifeSpanRange: Option[Span[Int]])

object PopulationProperties {
  implicit val format: Format[PopulationProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[PopulationProperties] = validator { properties =>
    properties.range is valid
    properties.growth.each should be >= 0
    properties.lifeSpanRange.each is valid
    if (properties.lifeSpanRange.isDefined) properties.lifeSpanRange.get.min should be > 0
  }
}

final case class ClusterProperties(size: Span[Int] = Span(0, 0), step: Int = 0, locations: Array[Position] = Array.empty, is3D: Boolean = false, autoGenerate: Boolean = true)

object ClusterProperties {
  implicit val format: Format[ClusterProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[ClusterProperties] = validator { properties =>
    if (properties.autoGenerate) properties.size.min should be > 0
    if (properties.autoGenerate) properties.step should be > 0
    if (!properties.autoGenerate) properties.locations is notEmpty
  }
}

final case class MotionProperties(distance: Int)

object MotionProperties {
  implicit val format: Format[MotionProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[MotionProperties] = validator { properties =>
    properties.distance should be > 0
  }
}

final case class ProximityProperties(kind: String, distance: Int)
object ProximityProperties {
  implicit val format: Format[ProximityProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[ProximityProperties] = validator { properties =>
    properties.distance should be >= 0
  }
}

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
