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
package com.loudflow.domain.agent

import com.wix.accord.transform.ValidationTransform
import java.util.UUID

import play.api.libs.json._
import com.wix.accord.dsl._

final case class AgentProperties(agentType: AgentType.Value, seed: Long = UUID.randomUUID().getMostSignificantBits & Long.MaxValue, interval: Int = 100)
object AgentProperties {
  implicit val format: Format[AgentProperties] = Json.format
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[AgentProperties] = validator { properties =>
    properties.interval should be > 50
  }
}

object AgentType {

  sealed trait Value

  final case object Random extends Value {
    val demuxer = "random"
  }

  val values: Set[Value] = Set(Random)

  def fromString(value: String): Value = value.toLowerCase match {
    case "random" => Random
  }

  implicit val reads: Reads[Value] = Reads { json =>
    (JsPath \ "demuxer").read[String].reads(json).flatMap {
      case "random" => JsSuccess(Random)
      case other => JsError(s"Read AgentType failed due to unknown enumeration value $other.")
    }
  }
  implicit val writes: Writes[Value] = Writes {
    case Random => JsObject(Seq("demuxer" -> JsString("random")))
  }
}
