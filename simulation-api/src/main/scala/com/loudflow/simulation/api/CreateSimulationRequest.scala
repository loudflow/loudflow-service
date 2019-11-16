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
package com.loudflow.simulation.api

import com.loudflow.domain.model.ModelProperties
import play.api.libs.json.{Format, Json}
import com.wix.accord.dsl._
import com.wix.accord.transform.ValidationTransform
import com.loudflow.domain.simulation.SimulationProperties

final case class CreateSimulationRequest private(data: CreateSimulationRequest.Data)

object CreateSimulationRequest {
  implicit val format: Format[CreateSimulationRequest] = Json.format
  def apply(simulation: SimulationProperties, model: ModelProperties): CreateSimulationRequest = {
    new CreateSimulationRequest(CreateSimulationRequest.Data(Attributes(simulation, model)))
  }
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[CreateSimulationRequest] = validator { properties =>
    properties.data is valid
  }
  final case class Data(attributes: Attributes) {
    val `type`: String = "simulation"
  }
  object Data {
    implicit val format: Format[Data] = Json.format
    implicit val propertiesValidator: ValidationTransform.TransformedValidator[Data] = validator { properties =>
      properties.attributes is valid
    }
  }
  final case class Attributes(simulation: SimulationProperties, model: ModelProperties)
  object Attributes {
    implicit val format: Format[Attributes] = Json.format
    implicit val propertiesValidator: ValidationTransform.TransformedValidator[Attributes] = validator { properties =>
      properties.simulation is valid
      properties.model is valid
    }
  }
}
