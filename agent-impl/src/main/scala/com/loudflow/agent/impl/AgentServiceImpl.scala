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

import java.util.UUID

import akka.persistence.query.Offset

import scala.concurrent.{ExecutionContext, Future}
import akka.{Done, NotUsed}
import com.loudflow.domain.model.{BatchAction, EntityAddedChange, EntityDroppedChange, EntityMovedChange, EntityPickedChange, EntityRemovedChange, ModelAction, ModelChange, ModelCreatedChange, ModelDestroyedChange, ModelProperties}
import com.loudflow.model.api.ModelService
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{ResponseHeader, TransportErrorCode, TransportException}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRef, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.loudflow.agent.api.AgentService
import com.loudflow.domain.agent.AgentProperties
import com.loudflow.domain.model.graph.GraphModelState
import com.loudflow.service.{Command, GraphQLRequest, HealthResponse}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.{Format, JsObject, JsValue, Json}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import sangria.schema.{Argument, Field, ObjectType, Schema, StringType, fields}
import sangria.marshalling.playJson._

import scala.collection.immutable
import scala.util.{Failure, Success}

class AgentServiceImpl(modelService: ModelService, persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends AgentService {

  private final val log: Logger = LoggerFactory.getLogger(classOf[AgentServiceImpl])

  modelService.changeTopic.subscribe.atLeastOnce(
    Flow.fromFunction(change => {
      log.trace(s"[${change.traceId}] AgentService received model change event [$change]")
      persistentEntity(change.modelId).ask(UpdateAgent(change.traceId, change))
      Done
    })
  )

  override def checkServiceHealth = ServiceCall { _ =>
    Future.successful(HealthResponse("agent"))
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
    TopicProducer.taggedStreamWithOffset(AgentEvent.Tag.allTags.toList) {
      (tag, offset) => {
        persistentEntityRegistry.eventStream(tag, offset).mapConcat(toModelAction)
      }
    }

  /* ************************************************************************
     PRIVATE
  ************************************************************************ */

  private val agentQueries: List[Field[AgentServiceImpl.Context, Unit]] = List(
    Field(
      name = "read",
      fieldType = AgentCommand.ReadReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => readAgent(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    )
  )

  private val agentMutations: List[Field[AgentServiceImpl.Context, Unit]] = List(
    Field(
      name = "create",
      fieldType = Command.CommandReplyType,
      arguments = List(
        Argument("agent", AgentProperties.SchemaInputType),
        Argument("model", ModelProperties.SchemaInputType)
      ),
      resolve = graphqlCtx => createAgent(graphqlCtx.args.arg[AgentProperties]("agent"), graphqlCtx.args.arg[ModelProperties]("model"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "destroy",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => destroyAgent(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "start",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => startAgent(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "stop",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => stopAgent(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "pause",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => pauseAgent(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "resume",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => resumeAgent(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "advance",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => advanceAgent(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "update",
      fieldType = Command.CommandReplyType,
      arguments = List(
        Argument("id", StringType),
        Argument("change", ModelChange.SchemaInputType)
      ),
      resolve = graphqlCtx => updateAgent(graphqlCtx.args.arg[String]("id"), graphqlCtx.args.arg[ModelChange]("change"), graphqlCtx.ctx.traceId)
    )
  )

  private val agentSchema = Schema(
    query = ObjectType("Query", fields(agentQueries: _*)),
    mutation = Some(ObjectType("Mutation", fields(agentMutations: _*))),
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

  private def createAgent(agentProperties: AgentProperties, modelProperties: ModelProperties, traceId: String): Future[Command.CommandReply] = {
    val command = CreateAgent(traceId, agentProperties, modelProperties)
    val id = UUID.randomUUID.toString
    persistentEntity(id).ask(command)
  }

  private def destroyAgent(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = DestroyAgent(traceId)
    persistentEntity(id).ask(command)
  }

  private def startAgent(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = StartAgent(traceId)
    persistentEntity(id).ask(command)
  }

  private def stopAgent(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = StopAgent(traceId)
    persistentEntity(id).ask(command)
  }

  private def pauseAgent(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = PauseAgent(traceId)
    persistentEntity(id).ask(command)
  }

  private def resumeAgent(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = ResumeAgent(traceId)
    persistentEntity(id).ask(command)
  }

  private def advanceAgent(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = AdvanceAgent(traceId)
    persistentEntity(id).ask(command)
  }

  private def updateAgent(id: String, change: ModelChange, traceId: String): Future[Command.CommandReply] = {
    val command = UpdateAgent(traceId, change)
    persistentEntity(id).ask(command)
  }

  private def readAgent(id: String, traceId: String): Future[AgentCommand.ReadReply] = {
    val command = ReadAgent(traceId)
    persistentEntity(id).ask(command)
  }

  private def graphQLQuery(query: String, operationName: Option[String], variables: Option[String], traceId: String): Future[(ResponseHeader, JsValue)] = {
    log.debug(s"QUERY: $query")
    QueryParser.parse(query) match {
      case Success(ast) =>
        Executor.execute(
          schema = agentSchema,
          queryAst = ast,
          userContext = AgentServiceImpl.Context(traceId),
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

  def trace[Request, Response](serviceCall: String => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = ServerServiceCall.compose(header => {
    val traceId = UUID.randomUUID.toString
    log.trace(s"[$traceId] AgentService received request ${header.method} ${header.uri}")
    serviceCall(traceId)
  })

  private def toModelAction(e: EventStreamElement[AgentEvent]): immutable.Seq[(ModelAction, Offset)] = {
    AgentEvent.toAction(e.event) match {
      case action @ BatchAction(_, _, actions) if actions.nonEmpty => immutable.Seq((action, e.offset))
      case _ => Nil
    }
  }

  private def persistentEntity(id: String): PersistentEntityRef[AgentCommand] = persistentEntityRegistry.refFor[AgentPersistentEntity](id)

}

object AgentServiceImpl {

  final case class Context(traceId: String)
  object Context { implicit val format: Format[Context] = Json.format }

}
