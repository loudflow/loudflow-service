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
import com.loudflow.domain.model.ModelChange
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRef, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.loudflow.api.{CommandResponse, HealthResponse}
import com.wix.accord.validate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.loudflow.model.api.{CreateModelRequest, ModelService, ReadModelResponse}
import com.loudflow.simulation.api.SimulationService

class ModelServiceImpl(simulationService: SimulationService, persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends ModelService {

  private final val log: Logger = LoggerFactory.getLogger(classOf[ModelServiceImpl])

  simulationService.actionTopic.subscribe.atLeastOnce(
    Flow.fromFunction(action => {
      log.trace(s"[${action.traceId}] SimulationService received model change event [$action]")
      ModelCommand.fromAction(action).foreach(command => {
        getPersistentEntity(action.modelId).ask(command)
      })
      Done
    })
  )

  override def checkServiceHealth = ServiceCall { _ =>
    Future.successful(HealthResponse("model"))
  }

  override def checkModelHealth(id: String) = ServiceCall { _ =>
    Future.successful(HealthResponse("model", Some(id.toString)))
  }

  override def createModel: ServerServiceCall[CreateModelRequest, CommandResponse] = trace { traceId =>
    ServerServiceCall { request =>
      val id = UUID.randomUUID.toString
      log.trace(s"[$traceId] Request body: $request")
      validate(request)
      val command = CreateModel(traceId, request.data.attributes)
      createPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def destroyModel(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = DestroyModel(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def readModel(id: String): ServiceCall[NotUsed, ReadModelResponse] = trace { traceId =>
    ServerServiceCall { _ => {
      val command = ReadModel(traceId)
      getPersistentEntity(id).ask(command).map(state => {
        ReadModelResponse(id, state)
      })
    }}
  }

  def trace[Request, Response](serviceCall: String => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = ServerServiceCall.compose(header => {
    val traceId = UUID.randomUUID.toString
    log.trace(s"[$traceId] ModelService received request ${header.method} ${header.uri}")
    serviceCall(traceId)
  })

  def accepted(id: String, command: ModelCommand): CommandResponse = {
    val commandName = command.getClass.getSimpleName
    log.trace(s"[${command.traceId}] ModelService accepted command [$commandName]")
    CommandResponse("model", id, commandName)
  }

  override def changeTopic: Topic[ModelChange] =
    TopicProducer.taggedStreamWithOffset(ModelEvent.Tag.allTags.toList) {
      (tag, offset) => persistentEntityRegistry.eventStream(tag, offset).map(event => (toModelChange(event), event.offset))
    }

  private def toModelChange(element: EventStreamElement[ModelEvent]): ModelChange = element.event match {
    case event: ModelEvent => ModelEvent.toChange(event)
  }

  private def createPersistentEntity(id: String): PersistentEntityRef[ModelCommand] = persistentEntityRegistry.refFor[ModelPersistentEntity](id)

  private def getPersistentEntity(id: String): PersistentEntityRef[ModelCommand] = persistentEntityRegistry.refFor[ModelPersistentEntity](id)

}
