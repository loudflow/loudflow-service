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
import com.loudflow.domain.model.{BatchAction, ModelAction, ModelType}
import com.loudflow.model.api.ModelService
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRef, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.loudflow.agent.api.{AgentService, CreateAgentRequest, ReadAgentResponse}
import com.loudflow.api.{CommandResponse, HealthResponse}
import com.wix.accord.validate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.collection.immutable

class AgentServiceImpl(modelService: ModelService, persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends AgentService {

  private final val log: Logger = LoggerFactory.getLogger(classOf[AgentServiceImpl])

  modelService.changeTopic.subscribe.atLeastOnce(
    Flow.fromFunction(change => {
      log.trace(s"[${change.traceId}] AgentService received model change event [$change]")
      getPersistentEntity(change.modelId).ask(UpdateAgent(change.traceId, change))
      Done
    })
  )

  override def checkServiceHealth = ServiceCall { _ =>
    Future.successful(HealthResponse("agent"))
  }

  override def checkAgentHealth(id: String) = ServiceCall { _ =>
    Future.successful(HealthResponse("agent", Some(id.toString)))
  }

  override def createAgent: ServerServiceCall[CreateAgentRequest, CommandResponse] = trace { traceId =>
    ServerServiceCall { request =>
      val id = UUID.randomUUID.toString
      log.trace(s"[$traceId] Request body: $request")
      validate(request)
      val command = CreateAgent(traceId, request.data.attributes.agent, request.data.attributes.model)
      createPersistentEntity(id, request.data.attributes.model.modelType).ask(command).map(_ => accepted(id, command))
    }
  }

  override def destroyAgent(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = DestroyAgent(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def readAgent(id: String): ServiceCall[NotUsed, ReadAgentResponse] = trace { traceId =>
    ServerServiceCall { _ => {
      val command = ReadAgent(traceId)
      getPersistentEntity(id).ask(command).map(state => {
        ReadAgentResponse(id, state)
      })
    }}
  }

  override def startAgent(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = StartAgent(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def stopAgent(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = StopAgent(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def pauseAgent(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = PauseAgent(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def resumeAgent(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = ResumeAgent(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  def trace[Request, Response](serviceCall: String => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = ServerServiceCall.compose(header => {
    val traceId = UUID.randomUUID.toString
    log.trace(s"[$traceId] AgentService received request ${header.method} ${header.uri}")
    serviceCall(traceId)
  })

  def accepted(id: String, command: AgentCommand): CommandResponse = {
    val commandName = command.getClass.getSimpleName
    log.trace(s"[${command.traceId}] AgentService accepted command [$commandName]")
    CommandResponse("agent", id, commandName)
  }

  override def actionTopic: Topic[ModelAction] =
    TopicProducer.taggedStreamWithOffset(AgentEvent.Tag.allTags.toList) {
      (tag, offset) => {
        persistentEntityRegistry.eventStream(tag, offset).mapConcat(toModelAction)
      }
    }

  private def toModelAction(e: EventStreamElement[AgentEvent]): immutable.Seq[(ModelAction, Offset)] = {
    AgentEvent.toAction(e.event) match {
      case action @ BatchAction(_, _, actions) if actions.nonEmpty => immutable.Seq((action, e.offset))
      case _ => Nil
    }
  }

  private def createPersistentEntity(id: String, modelType: ModelType.Value): PersistentEntityRef[AgentCommand] = modelType match {
    case ModelType.Graph =>
      persistentEntityRegistry.refFor[GraphAgentPersistentEntity](id)
  }

  private def getPersistentEntity(id: String): PersistentEntityRef[AgentCommand] = persistentEntityRegistry.refFor[AgentPersistentEntity[_]](id)

}
