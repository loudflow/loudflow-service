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
package com.loudflow.agent.impl

import play.api.libs.json._
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.loudflow.agent.impl.AgentCommand.ReadReply
import com.loudflow.domain.agent.{AgentProperties, AgentState}
import com.loudflow.domain.model.{ModelChange, ModelProperties}
import com.loudflow.service.Command
import com.loudflow.service.Command.CommandReply
import sangria.schema.{Field, ObjectType, StringType, fields}

sealed trait AgentCommand extends Command

/* ************************************************************************
   CRUD Commands
************************************************************************ */

final case class CreateAgent(traceId: String, agent: AgentProperties, model: ModelProperties) extends PersistentEntity.ReplyType[CommandReply] with AgentCommand {
  val demuxer = "create-agent"
}
object CreateAgent { implicit val format: Format[CreateAgent] = Json.format }

final case class DestroyAgent(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with AgentCommand {
  val demuxer = "destroy-agent"
}
object DestroyAgent { implicit val format: Format[DestroyAgent] = Json.format }

final case class ReadAgent(traceId: String) extends PersistentEntity.ReplyType[ReadReply] with AgentCommand {
  val demuxer = "read-agent"
}
object ReadAgent { implicit val format: Format[ReadAgent] = Json.format }

/* ************************************************************************
   Control Commands
************************************************************************ */

final case class StartAgent(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with AgentCommand {
  val demuxer = "start-agent"
}
object StartAgent { implicit val format: Format[StartAgent] = Json.format }

final case class StopAgent(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with AgentCommand {
  val demuxer = "stop-agent"
}
object StopAgent { implicit val format: Format[StopAgent] = Json.format }

final case class PauseAgent(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with AgentCommand {
  val demuxer = "pause-agent"
}
object PauseAgent { implicit val format: Format[PauseAgent] = Json.format }

final case class ResumeAgent(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with AgentCommand {
  val demuxer = "resume-agent"
}
object ResumeAgent { implicit val format: Format[ResumeAgent] = Json.format }

final case class AdvanceAgent(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with AgentCommand {
  val demuxer = "advance-agent"
}
object AdvanceAgent { implicit val format: Format[AdvanceAgent] = Json.format }

final case class UpdateAgent(traceId: String, change: ModelChange) extends PersistentEntity.ReplyType[CommandReply] with AgentCommand {
  val demuxer = "update-agent"
}
object UpdateAgent { implicit val format: Format[UpdateAgent] = Json.format }

/* ************************************************************************
   JSON Serialization
************************************************************************ */

object AgentCommand {
  implicit val reads: Reads[AgentCommand] = {
    (JsPath \ "demuxer").read[String].flatMap {
      case "create-agent" => implicitly[Reads[CreateAgent]].map(identity)
      case "destroy-agent" => implicitly[Reads[DestroyAgent]].map(identity)
      case "read-agent" => implicitly[Reads[ReadAgent]].map(identity)
      case "start-agent" => implicitly[Reads[StartAgent]].map(identity)
      case "stop-agent" => implicitly[Reads[StopAgent]].map(identity)
      case "pause-agent" => implicitly[Reads[PauseAgent]].map(identity)
      case "resume-agent" => implicitly[Reads[ResumeAgent]].map(identity)
      case "advance-agent" => implicitly[Reads[AdvanceAgent]].map(identity)
      case "update-agent" => implicitly[Reads[UpdateAgent]].map(identity)
      case other => Reads(_ => JsError(s"Read AgentCommand failed due to unknown type $other."))
    }
  }
  implicit val writes: Writes[AgentCommand] = Writes { obj =>
    val (jsValue, demuxer) = obj match {
      case command: CreateAgent => (Json.toJson(command)(CreateAgent.format), "create-agent")
      case command: DestroyAgent => (Json.toJson(command)(DestroyAgent.format), "destroy-agent")
      case command: ReadAgent => (Json.toJson(command)(ReadAgent.format), "read-agent")
      case command: StartAgent => (Json.toJson(command)(StartAgent.format), "start-agent")
      case command: StopAgent => (Json.toJson(command)(StopAgent.format), "stop-agent")
      case command: PauseAgent => (Json.toJson(command)(PauseAgent.format), "pause-agent")
      case command: ResumeAgent => (Json.toJson(command)(ResumeAgent.format), "resume-agent")
      case command: AdvanceAgent => (Json.toJson(command)(AdvanceAgent.format), "advance-agent")
      case command: UpdateAgent => (Json.toJson(command)(UpdateAgent.format), "update-agent")
    }
    jsValue.transform(JsPath.json.update((JsPath \ 'demuxer).json.put(JsString(demuxer)))).get
  }

  final case class ReadReply(id: String, traceId: String, state: AgentState)
  object ReadReply { implicit val format: Format[ReadReply] = Json.format }

  val ReadReplyType =
    ObjectType (
      "ReadReplyType",
      "Read command reply.",
      fields[Unit, ReadReply](
        Field("id", StringType, description = Some("Persistent entity identifier."), resolve = _.value.id),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("state", AgentState.SchemaType, description = Some("Agent state."), resolve = _.value.state)
      )
    )

}
