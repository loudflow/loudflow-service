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
package com.loudflow.simulation.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceCall, Service}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{PartitionKeyStrategy, KafkaProperties}
import com.loudflow.api.{CommandResponse, HealthResponse}
import com.loudflow.domain.model.{ModelChange, ModelAction}

trait SimulationService extends Service {

  import SimulationService._

  def checkServiceHealth: ServiceCall[NotUsed, HealthResponse]
  def checkSimulationHealth(id: String): ServiceCall[NotUsed, HealthResponse]

  def createSimulation: ServiceCall[CreateSimulationRequest, CommandResponse]
  def destroySimulation(id: String): ServiceCall[NotUsed, CommandResponse]
  def startSimulation(id: String): ServiceCall[NotUsed, CommandResponse]
  def stopSimulation(id: String): ServiceCall[NotUsed, CommandResponse]
  def pauseSimulation(id: String): ServiceCall[NotUsed, CommandResponse]
  def resumeSimulation(id: String): ServiceCall[NotUsed, CommandResponse]
  def readSimulation(id: String): ServiceCall[NotUsed, ReadSimulationResponse]

  def actionTopic: Topic[ModelAction]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named(SERVICE_NAME)
      .withCalls(
        restCall(Method.GET, s"$BASE_PATH/health", checkServiceHealth _),
        restCall(Method.GET, s"$BASE_PATH/:id/health", checkSimulationHealth _),
        restCall(Method.POST, s"$BASE_PATH", createSimulation _),
        restCall(Method.DELETE, s"$BASE_PATH/:id", destroySimulation _),
        restCall(Method.PATCH, s"$BASE_PATH/:id?action=start", startSimulation _),
        restCall(Method.PATCH, s"$BASE_PATH/:id?action=stop", stopSimulation _),
        restCall(Method.PATCH, s"$BASE_PATH/:id?action=pause", pauseSimulation _),
        restCall(Method.PATCH, s"$BASE_PATH/:id?action=resume", resumeSimulation _),
        restCall(Method.GET, s"$BASE_PATH/:id", readSimulation _)
      )
      .withTopics(
        topic(ACTION_TOPIC_NAME, actionTopic _)
          .addProperty(KafkaProperties.partitionKeyStrategy, PartitionKeyStrategy[ModelAction](_.modelId))
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

object SimulationService  {
  val API_VERSION = "v1"
  val SERVICE_NAME = "simulation"
  val BASE_PATH = s"/api/$API_VERSION/$SERVICE_NAME"
  val ACTION_TOPIC_NAME = "model-action"
}
