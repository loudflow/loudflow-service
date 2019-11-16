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

import com.wix.accord.dsl._
import com.wix.accord.transform.ValidationTransform
import play.api.libs.json.{JsSuccess, Format, Reads, Writes, JsError, JsObject, JsString, Json, JsPath}

final case class EntityProperties
(
  entityType: EntityType.Value,
  kind: String,
  description: Option[String],
  population: PopulationProperties,
  motion: MotionProperties,
  concurrence: Set[ConcurrenceProperties],
  proximity: Set[ProximityProperties],
  cluster: Option[ClusterProperties],
  grouping: Option[Int],
  score: Option[ScoreProperties]
)

object EntityProperties {
  implicit val format: Format[EntityProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[EntityProperties] = validator { properties =>
    properties.population is valid
    properties.motion is valid
    properties.cluster.each is valid
    properties.grouping.each should be > 0
    properties.score.each is valid
    properties.proximity.each is valid
  }
}

object EntityType {

  sealed trait Value

  final case object Agent extends Value {
    val demuxer = "agent"
  }

  final case object Thing extends Value {
    val demuxer = "thing"
  }

  val values: Set[Value] = Set(Agent, Thing)

  def fromString(value: String): Value = value.toLowerCase match {
    case "agent" => Agent
    case "thing" => Thing
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

final case class ScoreProperties(span: Span[Int], decline: Int)

object ScoreProperties {
  implicit val format: Format[ScoreProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[ScoreProperties] = validator { properties =>
    properties.span is valid
    properties.decline should be >= 0
  }
}

final case class ProximityProperties(entityType: EntityType.Value, kind: String, distance: Int)
object ProximityProperties {
  implicit val format: Format[ProximityProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[ProximityProperties] = validator { properties =>
    properties.distance should be >= 0
  }
}

final case class ConcurrenceProperties(entityType: EntityType.Value, kind: String, behavior: ConcurrenceBehavior.Value)
object ConcurrenceProperties { implicit val format: Format[ConcurrenceProperties] = Json.format }

object ConcurrenceBehavior {

  sealed trait Value

  final case object Block extends Value {
    val valueType = "block"
  }

  final case object Move extends Value {
    val valueType = "move"
  }

  final case object Share extends Value {
    val valueType = "share"
  }

  val values: Seq[Value] = Seq(Block, Move, Share)

  implicit val reads: Reads[Value] = Reads { json =>
    (JsPath \ "valueType").read[String].reads(json).flatMap {
      case "block" => JsSuccess(Block)
      case "move" => JsSuccess(Move)
      case "share" => JsSuccess(Share)
      case other => JsError(s"Read ConcurrenceBehavior failed due to unknown enumeration value $other.")
    }
  }
  implicit val writes: Writes[Value] = Writes {
    case Block => JsObject(Seq("valueType" -> JsString("block")))
    case Move => JsObject(Seq("valueType" -> JsString("move")))
    case Share => JsObject(Seq("valueType" -> JsString("share")))
  }

}
