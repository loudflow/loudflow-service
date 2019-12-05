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

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.loudflow.domain.model.{GraphProperties, GridProperties, ModelProperties, ModelType}
import com.loudflow.domain.simulation.SimulationProperties
import com.loudflow.service.{GraphQLRequest, HealthResponse}
import com.loudflow.simulation.api.SimulationService
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import play.api.libs.json.Json
import com.typesafe.scalalogging.Logger

class SimulationServiceImplTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private final val log = Logger[SimulationServiceImplTest]

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new SimulationApplication(ctx) with LocalServiceLocator
  }
  private implicit val system: ActorSystem = server.actorSystem
  private implicit val materializer: Materializer = server.materializer

  // create request
  private val gridProperties = GridProperties(10, 10)
  private val graphProperties = GraphProperties(Some(gridProperties))
  private val modelProperties = ModelProperties(ModelType.GRAPH, Some(graphProperties))
  private val stringifiedModelProperties: String = Json.toJson(modelProperties).toString
  private val simulationProperties = SimulationProperties()
  private val stringifiedSimulationProperties: String = Json.toJson(simulationProperties).toString
  private val createMutation: String =
    """mutation ($simulation: SimulationPropertiesInputType!, $model: ModelPropertiesInputType!) {create(simulation: $simulation, model: $model) {
      |id
      |command
      |}}""".stripMargin
  private val variables = s"""{"simulation": $stringifiedSimulationProperties, "model": $stringifiedModelProperties}"""
  log.debug(s"CREATE QUERY: $createMutation")
  log.debug(s"VARIABLES: $variables")
  // private val createRequest = GraphQLRequest(createMutation, None, Some(variables))

  protected override def afterAll(): Unit = server.stop()

  "simulation service" should {

    "respond to service health check" in {
      val client: SimulationService = server.serviceClient.implement[SimulationService]
      client.checkServiceHealth.invoke.map { response =>
        log.debug(s"RESPONSE: $response")
        response should ===(HealthResponse("simulation"))
      }
    }

  }

}
