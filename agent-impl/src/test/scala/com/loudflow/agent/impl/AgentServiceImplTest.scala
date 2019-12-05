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

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.loudflow.agent.api.AgentService
import com.loudflow.domain.agent.{AgentProperties, AgentType}
import com.loudflow.domain.model.{GraphProperties, GridProperties, ModelProperties, ModelType}
import com.loudflow.service.{GraphQLRequest, HealthResponse}
import play.api.libs.json.Json
import com.typesafe.scalalogging.Logger

class AgentServiceImplTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private final val log = Logger[AgentServiceImplTest]

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new AgentApplication(ctx) with LocalServiceLocator
  }
  private implicit val system: ActorSystem = server.actorSystem
  private implicit val materializer: Materializer = server.materializer

  // create request
  private val gridProperties = GridProperties(10, 10)
  private val graphProperties = GraphProperties(Some(gridProperties))
  private val modelProperties = ModelProperties(ModelType.GRAPH, Some(graphProperties))
  private val stringifiedModelProperties: String = Json.toJson(modelProperties).toString
  private val agentProperties = AgentProperties(AgentType.RANDOM)
  private val stringifiedAgentProperties: String = Json.toJson(agentProperties).toString
  private val createMutation: String =
    """mutation ($agent: AgentPropertiesInputType!, $model: ModelPropertiesInputType!) {create(agent: $agent, model: $model) {
      |id
      |command
      |}}""".stripMargin
  private val variables = s"""{"agent": $stringifiedAgentProperties, "model": $stringifiedModelProperties}"""
  log.debug(s"CREATE QUERY: $createMutation")
  log.debug(s"VARIABLES: $variables")
  // private val createRequest = GraphQLRequest(createMutation, None, Some(variables))

  protected override def afterAll(): Unit = server.stop()

  "agent service" should {

    "respond to service health check" in {
      val client: AgentService = server.serviceClient.implement[AgentService]
      client.checkServiceHealth.invoke.map { response =>
        log.debug(s"RESPONSE: $response")
        response should ===(HealthResponse("agent"))
      }
    }

  }

}
