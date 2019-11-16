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
package com.loudflow.api

import play.api.libs.json.{Format, Json}

final case class HealthResponse private(data: HealthResponse.Data)

object HealthResponse {
  implicit val format: Format[HealthResponse] = Json.format
  def apply(`type`: String, id: Option[String] = None): HealthResponse = {
    new HealthResponse(HealthResponse.Data(`type`, id))
  }
  final case class Data(`type`: String, id: Option[String] = None)
  object Data { implicit val format: Format[Data] = Json.format }
}
