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

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.testkit.javadsl.TestSink
import com.lightbend.lagom.scaladsl.api.broker.Topic
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import com.loudflow.api.{GraphQLRequest, HealthResponse}
import com.loudflow.domain.model.{GraphProperties, GridProperties, ModelAction, ModelChange, ModelProperties, ModelState, ModelType}
import com.loudflow.model.api.{CreateModelRequest, ModelService}
import com.loudflow.simulation.api.SimulationService
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsPath, Json, Writes}

class ModelServiceImplTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private final val log: Logger = LoggerFactory.getLogger(classOf[ModelServiceImplTest])

  private lazy val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new ModelApplication(ctx) with LocalServiceLocator // with TestTopicComponents
  }
  implicit private val system: ActorSystem = server.actorSystem
  implicit private val materializer: Materializer = server.materializer

  // create request
  val gridProperties = GridProperties(10, 10)
  val graphProperties = GraphProperties(Some(gridProperties))
  val modelProperties = ModelProperties(ModelType.GRAPH, Some(graphProperties))
  val stringifiedModelProperties: String = Json.toJson(modelProperties).toString
  val createMutation: String = """mutation ($properties: ModelPropertiesInputType!) {create(properties: $properties) {
                           |id
                           |command
                           |}}""".stripMargin
  val variables = s"""{ "properties": $stringifiedModelProperties}"""
  log.debug(s"CREATE QUERY: $createMutation")
  log.debug(s"VARIABLES: $variables")
  val createRequest = GraphQLRequest(createMutation, None, Some(variables))

  protected override def beforeAll(): Unit = server
  protected override def afterAll(): Unit = server.stop()

  "model service" should {

    "respond to service health check request" in {
      val client: ModelService = server.serviceClient.implement[ModelService]
      client.checkServiceHealth.invoke.map { response =>
        log.debug(s"RESPONSE: $response")
        response should ===(HealthResponse("model"))
      }
    }

    "respond to graphql create mutation" in  {
      val client: ModelService = server.serviceClient.implement[ModelService]
      client.postGraphQLQuery().invoke(createRequest).map { response =>
        log.debug(s"CREATE RESPONSE: $response")
        (response \ "data" \ "create" \ "command").as[String] shouldBe "CreateModel"
      }
    }

    "respond to graphql destroy mutation" in  {
      val client: ModelService = server.serviceClient.implement[ModelService]
      client.postGraphQLQuery().invoke(createRequest).flatMap { createResponse =>
        log.debug(s"CREATE RESPONSE: $createResponse")
        (createResponse \ "data" \ "create" \ "command").as[String] shouldBe "CreateModel"
        val id = (createResponse \ "data" \ "create" \ "id").as[String]
        val destroyMutation: String = s"""mutation {destroy(id: "$id") {
                                         |id
                                         |command
                                         |}}""".stripMargin
        log.debug(s"DESTROY MUTATION: $destroyMutation")
        val destroyRequest = GraphQLRequest(destroyMutation, None, None)
        client.postGraphQLQuery().invoke(destroyRequest).map { destroyResponse =>
          log.debug(s"DESTROY RESPONSE: $destroyResponse")
          (destroyResponse \ "data" \ "destroy" \ "id").as[String] shouldBe id
          (destroyResponse \ "data" \ "destroy" \ "command").as[String] shouldBe "DestroyModel"
        }
      }
    }

    "respond to graphql read query" in  {
      val client: ModelService = server.serviceClient.implement[ModelService]
      client.postGraphQLQuery().invoke(createRequest).flatMap { createResponse =>
        log.debug(s"CREATE RESPONSE: $createResponse")
        (createResponse \ "data" \ "create" \ "command").as[String] shouldBe "CreateModel"
        val id = (createResponse \ "data" \ "create" \ "id").as[String]
        val readQuery: String = s"""query {read(id: "$id") {
                                   |id
                                   |state {
                                   |id
                                   |seed
                                   |}
                                   |}}""".stripMargin
        log.debug(s"READ QUERY: $readQuery")
        val readRequest = GraphQLRequest(readQuery, None, None)
        client.postGraphQLQuery().invoke(readRequest).map { readResponse =>
          log.debug(s"READ RESPONSE: $readResponse")
          (readResponse \ "data" \ "read" \ "id").as[String] shouldBe id
          (readResponse \ "data" \ "read" \ "state" \ "seed").as[Long] shouldBe modelProperties.seed
        }
      }
    }

   }

}
