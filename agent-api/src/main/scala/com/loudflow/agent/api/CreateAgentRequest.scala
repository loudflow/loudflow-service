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
package com.loudflow.agent.api

import com.loudflow.domain.agent.AgentProperties
import com.loudflow.domain.model.ModelProperties
import play.api.libs.json.{Format, Json}
import com.wix.accord.dsl._
import com.wix.accord.transform.ValidationTransform

final case class CreateAgentRequest private(data: CreateAgentRequest.Data)

object CreateAgentRequest {
  implicit val format: Format[CreateAgentRequest] = Json.format
  def apply(agent: AgentProperties, model: ModelProperties): CreateAgentRequest = {
    new CreateAgentRequest(CreateAgentRequest.Data(Attributes(agent, model)))
  }
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[CreateAgentRequest] = validator { properties =>
    properties.data is valid
  }
  final case class Data(attributes: Attributes) {
    val `type`: String = "agent"
  }
  object Data {
    implicit val format: Format[Data] = Json.format
    implicit val propertiesValidator: ValidationTransform.TransformedValidator[Data] = validator { properties =>
      properties.attributes is valid
    }
  }
  final case class Attributes(agent: AgentProperties, model: ModelProperties)
  object Attributes {
    implicit val format: Format[Attributes] = Json.format
    implicit val propertiesValidator: ValidationTransform.TransformedValidator[Attributes] = validator { properties =>
      properties.agent is valid
      properties.model is valid
    }
  }
}
