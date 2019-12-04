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
package com.loudflow.model.impl

import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import akka.{Done, NotUsed}
import akka.stream.scaladsl.Flow
import com.loudflow.domain.model.{ModelChange, ModelProperties}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{ResponseHeader, TransportErrorCode, TransportException}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRef, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.loudflow.domain.model.graph.GraphModelState
import com.loudflow.service.{Command, GraphQLRequest, HealthResponse}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.loudflow.model.api.ModelService
import com.loudflow.simulation.api.SimulationService
import play.api.libs.json.{Format, JsObject, JsValue, Json}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import sangria.schema._
import sangria.marshalling.playJson._

import scala.util.{Failure, Success}

class ModelServiceImpl(simulationService: SimulationService, persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends ModelService {

  private final val log = LoggerFactory.getLogger(classOf[ModelServiceImpl])

  persistentEntityRegistry.register(new ModelPersistentEntity)

  simulationService.actionTopic.subscribe.atLeastOnce(
    Flow.fromFunction(action => {
      log.trace(s"[${action.traceId}] ModelService received model action message [$action]")
      ModelCommand.fromAction(action).foreach(command => {
        persistentEntity(action.modelId).ask(command)
      })
      Done
    })
  )

  override def checkServiceHealth: ServiceCall[NotUsed, HealthResponse] = ServiceCall { _ =>
    Future.successful(HealthResponse("model"))
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

  override def changeTopic: Topic[ModelChange] =
    TopicProducer.taggedStreamWithOffset(ModelEvent.Tag.allTags.toList) {
      (tag, offset) => persistentEntityRegistry.eventStream(tag, offset).map(event => (toModelChange(event), event.offset))
    }

  /* ************************************************************************
     PRIVATE
  ************************************************************************ */

  private val modelQueries: List[Field[ModelServiceImpl.Context, Unit]] = List(
    Field(
      name = "read",
      fieldType = ModelCommand.ReadReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => readModel(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    )
  )

  private val modelMutations: List[Field[ModelServiceImpl.Context, Unit]] = List(
    Field(
      name = "create",
      fieldType = Command.CommandReplyType,
      arguments = Argument("properties", ModelProperties.SchemaInputType) :: Nil,
      resolve = graphqlCtx => createModel(graphqlCtx.args.arg[ModelProperties]("properties"), graphqlCtx.ctx.traceId)
    ),
    Field(
      name = "destroy",
      fieldType = Command.CommandReplyType,
      arguments = Argument("id", StringType) :: Nil,
      resolve = graphqlCtx => destroyModel(graphqlCtx.args.arg[String]("id"), graphqlCtx.ctx.traceId)
    )
  )

  private def createModel(properties: ModelProperties, traceId: String): Future[Command.CommandReply] = {
    val command = CreateModel(traceId, properties)
    val id = UUID.randomUUID.toString
    persistentEntity(id).ask(command)
  }

  private def destroyModel(id: String, traceId: String): Future[Command.CommandReply] = {
    val command = DestroyModel(traceId)
    persistentEntity(id).ask(command)
  }

  private def readModel(id: String, traceId: String): Future[ModelCommand.ReadReply] = {
    val command = ReadModel(traceId)
    persistentEntity(id).ask(command)
  }

  private val modelSchema = Schema(
    query = ObjectType("Query", fields(modelQueries: _*)),
    mutation = Some(ObjectType("Mutation", fields(modelMutations: _*))),
    additionalTypes = GraphModelState.SchemaType :: Nil
  )

  private def graphQLQuery(query: String, operationName: Option[String], variables: Option[String], traceId: String): Future[(ResponseHeader, JsValue)] = {
    log.debug(s"QUERY: $query")
    QueryParser.parse(query) match {
      case Success(ast) =>
        Executor.execute(
          schema = modelSchema,
          queryAst = ast,
          userContext = ModelServiceImpl.Context(traceId),
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
    log.trace(s"[$traceId] ModelService received request ${header.method} ${header.uri}")
    serviceCall(traceId)
  })

  private def toModelChange(element: EventStreamElement[ModelEvent]): ModelChange = element.event match {
    case event: ModelEvent => ModelEvent.toChange(event)
  }

  private def persistentEntity(id: String): PersistentEntityRef[ModelCommand] = persistentEntityRegistry.refFor[ModelPersistentEntity](id)

}

object ModelServiceImpl {

  final case class Context(traceId: String)
  object Context { implicit val format: Format[Context] = Json.format }

}
