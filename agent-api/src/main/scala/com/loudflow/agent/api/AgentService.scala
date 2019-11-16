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
package com.loudflow.agent.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceCall, Service}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{PartitionKeyStrategy, KafkaProperties}
import com.loudflow.api.{CommandResponse, HealthResponse}
import com.loudflow.domain.model.ModelAction

trait AgentService extends Service {

  import AgentService._

  def checkServiceHealth: ServiceCall[NotUsed, HealthResponse]
  def checkAgentHealth(id: String): ServiceCall[NotUsed, HealthResponse]

  def createAgent: ServiceCall[CreateAgentRequest, CommandResponse]
  def destroyAgent(id: String): ServiceCall[NotUsed, CommandResponse]
  def startAgent(id: String): ServiceCall[NotUsed, CommandResponse]
  def stopAgent(id: String): ServiceCall[NotUsed, CommandResponse]
  def pauseAgent(id: String): ServiceCall[NotUsed, CommandResponse]
  def resumeAgent(id: String): ServiceCall[NotUsed, CommandResponse]
  def readAgent(id: String): ServiceCall[NotUsed, ReadAgentResponse]

  def actionTopic: Topic[ModelAction]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named(SERVICE_NAME)
      .withCalls(
        restCall(Method.GET, s"$BASE_PATH/health", checkServiceHealth _),
        restCall(Method.GET, s"$BASE_PATH/:id/health", checkAgentHealth _),
        restCall(Method.POST, s"$BASE_PATH", createAgent _),
        restCall(Method.DELETE, s"$BASE_PATH/:id", destroyAgent _),
        restCall(Method.PATCH, s"$BASE_PATH/:id?action=start", startAgent _),
        restCall(Method.PATCH, s"$BASE_PATH/:id?action=stop", stopAgent _),
        restCall(Method.PATCH, s"$BASE_PATH/:id?action=pause", pauseAgent _),
        restCall(Method.PATCH, s"$BASE_PATH/:id?action=resume", resumeAgent _),
        restCall(Method.GET, s"$BASE_PATH/:id", readAgent _)
      )
      .withTopics(
        topic(ACTION_TOPIC_NAME, actionTopic _)
          .addProperty(KafkaProperties.partitionKeyStrategy, PartitionKeyStrategy[ModelAction](_.modelId))
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

object AgentService  {
  val API_VERSION = "v1"
  val SERVICE_NAME = "agent"
  val BASE_PATH = s"/api/$API_VERSION/$SERVICE_NAME"
  val ACTION_TOPIC_NAME = "model-action"
}
