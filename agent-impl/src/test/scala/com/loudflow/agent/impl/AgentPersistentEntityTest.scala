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

import scala.concurrent.ExecutionContext
import akka.Done
import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.testkit.{PersistentEntityTestDriver, ServiceTest}
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.loudflow.domain.agent.{AgentProperties, AgentType, AgentState}
import com.loudflow.domain.model.{GraphProperties, GridProperties, ModelProperties, ModelType}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class AgentPersistentEntityTest(implicit ec: ExecutionContext) extends WordSpec with Matchers with BeforeAndAfterAll {

  private val system = ActorSystem("GraphSimulationPersistentEntityTest", JsonSerializerRegistry.actorSystemSetupFor(AgentSerializerRegistry))

  private lazy val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new AgentApplication(ctx) with LocalServiceLocator
  }

  private def withTestDriver(block: PersistentEntityTestDriver[AgentCommand, AgentEvent, Option[AgentState]] => Unit): Unit = {
    val id = UUID.randomUUID().toString
    val driver = new PersistentEntityTestDriver(system, new AgentPersistentEntity()(server.application.persistentEntityRegistry, system, ec), id)
    block(driver)
    driver.getAllIssues should have size 0
  }

  override protected def beforeAll(): Unit = server

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "GraphSimulationPersistentEntity" should {

    "handle CreateSimulation command" in withTestDriver { driver =>
      val traceId = UUID.randomUUID.toString
      val gridProperties = GridProperties(10, 10)
      val graphProperties = GraphProperties(Some(gridProperties))
      val modelProperties = ModelProperties(ModelType.Graph, Some(graphProperties))
      val agentProperties = AgentProperties(AgentType.Random)
      val outcome = driver.run(CreateAgent(traceId, agentProperties, modelProperties))
      outcome.replies should ===(Done)
    }

  }
}
