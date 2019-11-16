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
package com.loudflow.model.api

import play.api.libs.json.{Format, Json}
import com.wix.accord.dsl._
import com.wix.accord.transform.ValidationTransform
import com.loudflow.domain.model.ModelState

final case class ReadModelResponse private(data: ReadModelResponse.Data)

object ReadModelResponse {
  implicit val format: Format[ReadModelResponse] = Json.format
  def apply(id: String, state: ModelState): ReadModelResponse = {
    new ReadModelResponse(ReadModelResponse.Data(id, state))
  }
  implicit val propertiesValidator: ValidationTransform.TransformedValidator[ReadModelResponse] = validator { properties =>
    properties.data is valid
  }
  final case class Data(id: String, attributes: ModelState) {
    val `type`: String = "model"
  }
  object Data {
    implicit val format: Format[Data] = Json.format
    implicit val propertiesValidator: ValidationTransform.TransformedValidator[Data] = validator { properties =>
      properties.attributes is valid
    }
  }
}
