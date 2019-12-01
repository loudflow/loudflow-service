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
package com.loudflow.simulation.impl

import play.api.libs.json._
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.loudflow.domain.model.{ModelChange, ModelProperties}
import com.loudflow.domain.simulation.{SimulationProperties, SimulationState}
import com.loudflow.service.Command.CommandReply
import com.loudflow.service.Command
import com.loudflow.simulation.impl.SimulationCommand.ReadReply
import sangria.schema.{Field, ObjectType, StringType, fields}

sealed trait SimulationCommand extends Command

/* ************************************************************************
   CRUD Commands
************************************************************************ */

final case class CreateSimulation(traceId: String, simulation: SimulationProperties, model: ModelProperties) extends PersistentEntity.ReplyType[CommandReply] with SimulationCommand {
  val demuxer = "create-simulation"
}
object CreateSimulation { implicit val format: Format[CreateSimulation] = Json.format }

final case class DestroySimulation(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with SimulationCommand {
  val demuxer = "destroy-simulation"
}
object DestroySimulation { implicit val format: Format[DestroySimulation] = Json.format }

final case class ReadSimulation(traceId: String) extends PersistentEntity.ReplyType[ReadReply] with SimulationCommand {
  val demuxer = "read-simulation"
}
object ReadSimulation { implicit val format: Format[ReadSimulation] = Json.format }

/* ************************************************************************
   Control Commands
************************************************************************ */

final case class StartSimulation(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with SimulationCommand {
  val demuxer = "start-simulation"
}
object StartSimulation { implicit val format: Format[StartSimulation] = Json.format }

final case class StopSimulation(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with SimulationCommand {
  val demuxer = "stop-simulation"
}
object StopSimulation { implicit val format: Format[StopSimulation] = Json.format }

final case class PauseSimulation(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with SimulationCommand {
  val demuxer = "pause-simulation"
}
object PauseSimulation { implicit val format: Format[PauseSimulation] = Json.format }

final case class ResumeSimulation(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with SimulationCommand {
  val demuxer = "resume-simulation"
}
object ResumeSimulation { implicit val format: Format[ResumeSimulation] = Json.format }

final case class AdvanceSimulation(traceId: String) extends PersistentEntity.ReplyType[CommandReply] with SimulationCommand {
  val demuxer = "advance-simulation"
}
object AdvanceSimulation { implicit val format: Format[AdvanceSimulation] = Json.format }

final case class UpdateSimulation(traceId: String, change: ModelChange) extends PersistentEntity.ReplyType[CommandReply] with SimulationCommand {
  val demuxer = "update-simulation"
}
object UpdateSimulation { implicit val format: Format[UpdateSimulation] = Json.format }

/* ************************************************************************
   JSON Serialization
************************************************************************ */

object SimulationCommand {
  implicit val reads: Reads[SimulationCommand] = {
    (JsPath \ "demuxer").read[String].flatMap {
      case "create-simulation" => implicitly[Reads[CreateSimulation]].map(identity)
      case "destroy-simulation" => implicitly[Reads[DestroySimulation]].map(identity)
      case "read-simulation" => implicitly[Reads[ReadSimulation]].map(identity)
      case "start-simulation" => implicitly[Reads[StartSimulation]].map(identity)
      case "stop-simulation" => implicitly[Reads[StopSimulation]].map(identity)
      case "pause-simulation" => implicitly[Reads[PauseSimulation]].map(identity)
      case "resume-simulation" => implicitly[Reads[ResumeSimulation]].map(identity)
      case "advance-simulation" => implicitly[Reads[AdvanceSimulation]].map(identity)
      case "update-simulation" => implicitly[Reads[UpdateSimulation]].map(identity)
      case other => Reads(_ => JsError(s"Read SimulationCommand failed due to unknown type $other."))
    }
  }
  implicit val writes: Writes[SimulationCommand] = Writes { obj =>
    val (jsValue, demuxer) = obj match {
      case command: CreateSimulation => (Json.toJson(command)(CreateSimulation.format), "create-simulation")
      case command: DestroySimulation => (Json.toJson(command)(DestroySimulation.format), "destroy-simulation")
      case command: ReadSimulation => (Json.toJson(command)(ReadSimulation.format), "read-simulation")
      case command: StartSimulation => (Json.toJson(command)(StartSimulation.format), "start-simulation")
      case command: StopSimulation => (Json.toJson(command)(StopSimulation.format), "stop-simulation")
      case command: PauseSimulation => (Json.toJson(command)(PauseSimulation.format), "pause-simulation")
      case command: ResumeSimulation => (Json.toJson(command)(ResumeSimulation.format), "resume-simulation")
      case command: AdvanceSimulation => (Json.toJson(command)(AdvanceSimulation.format), "advance-simulation")
      case command: UpdateSimulation => (Json.toJson(command)(UpdateSimulation.format), "update-simulation")
    }
    jsValue.transform(JsPath.json.update((JsPath \ 'demuxer).json.put(JsString(demuxer)))).get
  }

  final case class ReadReply(id: String, traceId: String, state: SimulationState)
  object ReadReply { implicit val format: Format[ReadReply] = Json.format }

  val ReadReplyType =
    ObjectType (
      "ReadReplyType",
      "Read command reply.",
      fields[Unit, ReadReply](
        Field("id", StringType, description = Some("Persistent entity identifier."), resolve = _.value.id),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("state", SimulationState.SchemaType, description = Some("Simulation state."), resolve = _.value.state)
      )
    )

}
