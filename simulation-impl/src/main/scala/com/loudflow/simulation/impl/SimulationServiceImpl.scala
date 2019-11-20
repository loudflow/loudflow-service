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
import com.loudflow.domain.model.{BatchAction, ModelAction}
import com.loudflow.model.api.ModelService
import com.loudflow.simulation.api.{CreateSimulationRequest, ReadSimulationResponse, SimulationService}
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRef, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.loudflow.api.{CommandResponse, HealthResponse}
import com.wix.accord.validate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.collection.immutable

class SimulationServiceImpl(modelService: ModelService, persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends SimulationService {

  private final val log: Logger = LoggerFactory.getLogger(classOf[SimulationServiceImpl])

  modelService.changeTopic.subscribe.atLeastOnce(
    Flow.fromFunction(change => {
      log.trace(s"[${change.traceId}] SimulationService received model change event [$change]")
      getPersistentEntity(change.modelId).ask(UpdateSimulation(change.traceId, change))
      Done
    })
  )

  override def checkServiceHealth = ServiceCall { _ =>
    Future.successful(HealthResponse("simulation"))
  }

  override def checkSimulationHealth(id: String) = ServiceCall { _ =>
    Future.successful(HealthResponse("simulation", Some(id.toString)))
  }

  override def createSimulation: ServerServiceCall[CreateSimulationRequest, CommandResponse] = trace { traceId =>
    ServerServiceCall { request =>
      val id = UUID.randomUUID.toString
      log.trace(s"[$traceId] Request body: $request")
      validate(request)
      val command = CreateSimulation(traceId, request.data.attributes.simulation, request.data.attributes.model)
      createPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def destroySimulation(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = DestroySimulation(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def readSimulation(id: String): ServiceCall[NotUsed, ReadSimulationResponse] = trace { traceId =>
    ServerServiceCall { _ => {
      val command = ReadSimulation(traceId)
      getPersistentEntity(id).ask(command)
    }}
  }

  override def startSimulation(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = StartSimulation(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def stopSimulation(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = StopSimulation(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def pauseSimulation(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = PauseSimulation(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  override def resumeSimulation(id: String): ServerServiceCall[NotUsed, CommandResponse] = trace { traceId =>
    ServerServiceCall { _ =>
      val command = ResumeSimulation(traceId)
      getPersistentEntity(id).ask(command).map(_ => accepted(id, command))
    }
  }

  def trace[Request, Response](serviceCall: String => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = ServerServiceCall.compose(header => {
    val traceId = UUID.randomUUID.toString
    log.trace(s"[$traceId] SimulationService received request ${header.method} ${header.uri}")
    serviceCall(traceId)
  })

  def accepted(id: String, command: SimulationCommand): CommandResponse = {
    val commandName = command.getClass.getSimpleName
    log.trace(s"[${command.traceId}] SimulationService accepted command [$commandName]")
    CommandResponse("simulation", id, commandName)
  }

  override def actionTopic: Topic[ModelAction] =
    TopicProducer.taggedStreamWithOffset(SimulationEvent.Tag.allTags.toList) {
      (tag, offset) => {
        persistentEntityRegistry.eventStream(tag, offset).mapConcat(toModelAction)
      }
    }

  private def toModelAction(e: EventStreamElement[SimulationEvent]): immutable.Seq[(ModelAction, Offset)] = {
    SimulationEvent.toAction(e.event) match {
      case action @ BatchAction(_, _, actions) if actions.nonEmpty => immutable.Seq((action, e.offset))
      case _ => Nil
    }
  }

  private def createPersistentEntity(id: String): PersistentEntityRef[SimulationCommand] = persistentEntityRegistry.refFor[SimulationPersistentEntity](id)

  private def getPersistentEntity(id: String): PersistentEntityRef[SimulationCommand] = persistentEntityRegistry.refFor[SimulationPersistentEntity](id)

}
