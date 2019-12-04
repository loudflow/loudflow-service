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

import java.util.UUID

import akka.persistence.query.Offset

import scala.concurrent.{ExecutionContext, Future}
import akka.{Done, NotUsed}
import com.loudflow.domain.model.{BatchAction, EntityAddedChange, EntityDroppedChange, EntityMovedChange, EntityPickedChange, EntityRemovedChange, ModelAction, ModelChange, ModelCreatedChange, ModelDestroyedChange, ModelProperties}
import com.loudflow.model.api.ModelService
import com.loudflow.simulation.api.SimulationService
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{ResponseHeader, TransportErrorCode, TransportException}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRef, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.loudflow.domain.model.graph.GraphModelState
import com.loudflow.domain.simulation.SimulationProperties
import com.loudflow.service.{Command, GraphQLRequest, HealthResponse}
import play.api.libs.json.{Format, JsObject, JsValue, Json}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import sangria.schema._
import sangria.marshalling.playJson._

import scala.collection.immutable
import scala.util.{Failure, Success}
import com.typesafe.scalalogging.Logger

class SimulationServiceImpl(modelService: ModelService, persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends SimulationService {

  private final val log = Logger[SimulationServiceImpl]

  modelService.changeTopic.subscribe.atLeastOnce(
    Flow.fromFunction(change => {
      log.trace(s"[${change.traceId}] SimulationService received model change event [$change]")
      persistentEntity(change.modelId).ask(UpdateSimulation(change.traceId, change))
      Done
    })
  )

  override def checkServiceHealth: ServiceCall[NotUsed, HealthResponse] = ServiceCall { _ =>
    Future.successful(HealthResponse("simulation"))
  }

  override def getGraphQLQuery(query: String, operationName: Option[String], variables: Option[String]): ServiceCall[NotUsed, JsValue] = trace { traceId =>
    ServerServiceCall { (_, _) => graphQLQuery(query, operationName, variables, traceId) }
  }

  override def postGraphQLQuery(query: Option[String], operationName: Option[String], variables: Option[String]): ServiceCall[GraphQLRequest, JsValue] = trace { traceId =>
    ServerServiceCall { (requestHeader, request) => {
      requestHeader.protocol.contentType match {
        case Some(contentType) =>
          if (contentType.toLowerCase == "application/graphql")
            graphQLQuery(request.toString, None, None, traceId) // TODO: this prob does not work, need content negotiation so that it is treated as string before parsing as GraphQLRequest.
          else
            graphQLQuery(query.getOrElse(request.query), operationName.orElse(request.operationName), variables.orElse(request.variables), traceId)
        case None =>
          graphQLQuery(query.getOrElse(request.query), operationName.orElse(request.operationName), variables.orElse(request.variables), traceId)
      }
    }}
  }

  override def actionTopic: Topic[ModelAction] =
    TopicProducer.taggedStreamWithOffset(SimulationEvent.Tag.allTags.toList) {
      (tag, offset) => {
        persistentEntityRegistry.eventStream(tag, offset).mapConcat(toModelAction)
      }
    }

  /* ************************************************************************
     PRIVATE
  ************************************************************************ */

  private val simulationQueries: List[Field[SimulationServiceImpl.Context, Unit]] = List(
    Field(
      name = "read",
      fieldType = SimulationCommand.ReadReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => readSimulation(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    )
  )

  private val simulationMutations: List[Field[SimulationServiceImpl.Context, Unit]] = List(
    Field(
      name = "create",
      fieldType = Command.CommandReplyType,
      arguments = List(
        Argument("simulation", SimulationProperties.SchemaInputType),
        Argument("model", ModelProperties.SchemaInputType)
      ),
      resolve = graphqlCtx => createSimulation(graphqlCtx.args.arg[SimulationProperties]("simulation"), graphqlCtx.args.arg[ModelProperties]("model"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "destroy",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => destroySimulation(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "start",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => startSimulation(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "stop",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => stopSimulation(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "pause",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => pauseSimulation(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "resume",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => resumeSimulation(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "advance",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => advanceSimulation(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "update",
      fieldType = Command.CommandReplyType,
      arguments = List(
        Argument("id", StringType),
        Argument("change", ModelChange.SchemaInputType)
      ),
      resolve = graphqlCtx => updateSimulation(graphqlCtx.args.arg[String]("id"), graphqlCtx.args.arg[ModelChange]("change"), graphqlCtx.ctx.traceId)
    )
  )

  private val simulationSchema = Schema(
    query = ObjectType("Query", fields(simulationQueries: _*)),
    mutation = Some(ObjectType("Mutation", fields(simulationMutations: _*))),
    additionalTypes = List(
      GraphModelState.SchemaType,
      ModelCreatedChange.SchemaType,
      ModelCreatedChange.SchemaInputType,
      ModelDestroyedChange.SchemaType,
      ModelDestroyedChange.SchemaInputType,
      EntityAddedChange.SchemaType,
      EntityAddedChange.SchemaInputType,
      EntityRemovedChange.SchemaType,
      EntityRemovedChange.SchemaInputType,
      EntityMovedChange.SchemaType,
      EntityMovedChange.SchemaInputType,
      EntityPickedChange.SchemaType,
      EntityPickedChange.SchemaInputType,
      EntityDroppedChange.SchemaType,
      EntityDroppedChange.SchemaInputType
    )
  )

  private def createSimulation(simulationProperties: SimulationProperties, modelProperties: ModelProperties, traceId: String): Future[Command.CommandReply] = {
    val command = CreateSimulation(traceId, simulationProperties, modelProperties)
    val id = UUID.randomUUID.toString
    persistentEntity(id).ask(command)
  }

  private def destroySimulation(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = DestroySimulation(traceId)
    persistentEntity(id).ask(command)
  }

  private def startSimulation(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = StartSimulation(traceId)
    persistentEntity(id).ask(command)
  }

  private def stopSimulation(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = StopSimulation(traceId)
    persistentEntity(id).ask(command)
  }

  private def pauseSimulation(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = PauseSimulation(traceId)
    persistentEntity(id).ask(command)
  }

  private def resumeSimulation(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = ResumeSimulation(traceId)
    persistentEntity(id).ask(command)
  }

  private def advanceSimulation(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = AdvanceSimulation(traceId)
    persistentEntity(id).ask(command)
  }

  private def updateSimulation(id: String, change: ModelChange, traceId: String): Future[Command.CommandReply] = {
    val command = UpdateSimulation(traceId, change)
    persistentEntity(id).ask(command)
  }

  private def readSimulation(id: String, traceId: String): Future[SimulationCommand.ReadReply] = {
    val command = ReadSimulation(traceId)
    persistentEntity(id).ask(command)
  }

  private def graphQLQuery(query: String, operationName: Option[String], variables: Option[String], traceId: String): Future[(ResponseHeader, JsValue)] = {
    log.debug(s"QUERY: $query")
    QueryParser.parse(query) match {
      case Success(ast) =>
        Executor.execute(
          schema = simulationSchema,
          queryAst = ast,
          userContext = SimulationServiceImpl.Context(traceId),
          variables = parseVariables(variables)
        )
          .map(result => (ResponseHeader.Ok, result))
          .recover {
            case error: QueryAnalysisError =>
              log.error(s"GRAPHQL QUERY ANALYSIS ERROR: $error")
              (ResponseHeader.Ok.withStatus(400), error.resolveError)
            case error: ErrorWithResolver =>
              log.error(s"GRAPHQL EXECUTION ERROR: $error")
              (ResponseHeader.Ok.withStatus(500), error.resolveError)
          }
      case Failure(exception) =>
        log.error(s"GRAPHQL QUERY PARSER EXCEPTION: $exception")
        throw new TransportException(TransportErrorCode.BadRequest, s"${exception.getMessage}")
    }
  }

  private def parseVariables(variables: Option[String]) =
    variables match {
      case Some(v) => if (v.trim == "" || v.trim == "null") Json.obj() else Json.parse(v).as[JsObject]
      case None => Json.obj()
    }

  private def trace[Request, Response](serviceCall: String => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = ServerServiceCall.compose(header => {
    val traceId = UUID.randomUUID.toString
    log.trace(s"[$traceId] SimulationService received request ${header.method} ${header.uri}")
    serviceCall(traceId)
  })

  private def toModelAction(e: EventStreamElement[SimulationEvent]): immutable.Seq[(ModelAction, Offset)] = {
    SimulationEvent.toAction(e.event) match {
      case action @ BatchAction(_, _, actions) if actions.nonEmpty => immutable.Seq((action, e.offset))
      case _ => Nil
    }
  }

  private def persistentEntity(id: String): PersistentEntityRef[SimulationCommand] = persistentEntityRegistry.refFor[SimulationPersistentEntity](id)

}

object SimulationServiceImpl {

  final case class Context(traceId: String)
  object Context { implicit val format: Format[Context] = Json.format }

}
