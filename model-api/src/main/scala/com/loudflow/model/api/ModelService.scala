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
package com.loudflow.model.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.loudflow.api.{GraphQLRequest, HealthResponse}
import com.loudflow.domain.model.ModelChange
import play.api.libs.json.JsValue

trait ModelService extends Service {

  import ModelService._

  def checkServiceHealth: ServiceCall[NotUsed, HealthResponse]
  def getGraphQLQuery(query: String, operationName: Option[String] = None, variables: Option[String] = None): ServiceCall[NotUsed, JsValue]
  def postGraphQLQuery(query: Option[String] = None, operationName: Option[String] = None, variables: Option[String] = None): ServiceCall[GraphQLRequest, JsValue]

  def changeTopic: Topic[ModelChange]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named(SERVICE_NAME)
      .withCalls(
        restCall(Method.GET, s"$BASE_PATH/health", checkServiceHealth _),
        restCall(Method.GET, s"$BASE_PATH/graphql?query&operationName&variables", getGraphQLQuery _),
        restCall(Method.POST, s"$BASE_PATH/graphql?query&operationName&variables", postGraphQLQuery _)
      )
      .withTopics(
        topic(CHANGE_TOPIC_NAME, changeTopic _)
          .addProperty(KafkaProperties.partitionKeyStrategy, PartitionKeyStrategy[ModelChange](_.modelId))
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

object ModelService  {
  val API_VERSION = "v1"
  val SERVICE_NAME = "model"
  val BASE_PATH = s"/api/$API_VERSION/$SERVICE_NAME"
  val CHANGE_TOPIC_NAME = "model-change"
}
