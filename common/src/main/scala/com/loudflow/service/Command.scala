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
package com.loudflow.service

import com.loudflow.domain.Message
import play.api.libs.json._
import sangria.schema.{Field, ObjectType, StringType, fields}

trait Command extends Message {
  def demuxer: String
}
object Command {

  final case class CommandReply(id: String, traceId: String, command: String)
  object CommandReply { implicit val format: Format[CommandReply] = Json.format }

  val CommandReplyType: ObjectType[Unit, CommandReply] =
    ObjectType (
      "CommandReplyType",
      "Command accepted reply.",
      fields[Unit, CommandReply](
        Field("id", StringType, description = Some("Persistent entity identifier."), resolve = _.value.id),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("command", StringType, description = Some("Command name."), resolve = _.value.command)
      )
    )

}
