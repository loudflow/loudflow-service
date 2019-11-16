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

import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.loudflow.agent.api.AgentService
import com.loudflow.api.HealthResponse

class AgentServiceTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private lazy val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new AgentApplication(ctx) with LocalServiceLocator
  }

  lazy val client: AgentService = server.serviceClient.implement[AgentService]

  override protected def beforeAll(): Unit = server

  override protected def afterAll(): Unit = server.stop()

  "simulation service" should {

    "respond to service health check" in {
      client.checkServiceHealth.invoke.map { response =>
        response should ===(HealthResponse("simulation"))
      }
    }

    "respond to simulation health check" in {
      val id = UUID.randomUUID().toString
      client.checkAgentHealth(id).invoke.map { response =>
        response should ===(HealthResponse("simulation", Some(id.toString)))
      }
    }

  }

}
