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

final case class CommandResponse private(data: CommandResponse.Data)

object CommandResponse {
  implicit val format: Format[CommandResponse] = Json.format
  def apply(`type`: String, id: String, command: String): CommandResponse = {
    new CommandResponse(CommandResponse.Data(`type`, id, command))
  }
  final case class Data private(`type`: String, id: String, attributes: Attributes)
  object Data {
    implicit val format: Format[Data] = Json.format
    def apply(`type`: String, id: String, command: String): Data = new Data(`type`, id, Attributes(command))
  }
  final case class Attributes(command: String)
  object Attributes { implicit val format: Format[Attributes] = Json.format }
}
