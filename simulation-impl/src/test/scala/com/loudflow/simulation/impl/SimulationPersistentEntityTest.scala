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

import akka.Done
import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{PersistentEntityTestDriver, ServiceTest}
import com.loudflow.domain.model.{GraphProperties, GridProperties, ModelProperties, ModelType}
import com.loudflow.domain.simulation.{SimulationProperties, SimulationState}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.ExecutionContext

class SimulationPersistentEntityTest(implicit ec: ExecutionContext) extends WordSpec with Matchers with BeforeAndAfterAll {

  private implicit val system: ActorSystem = ActorSystem("GraphSimulationPersistentEntityTest", JsonSerializerRegistry.actorSystemSetupFor(SimulationSerializerRegistry))

  private lazy val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new SimulationApplication(ctx) with LocalServiceLocator
  }

  private implicit val registry: PersistentEntityRegistry = server.application.persistentEntityRegistry

  private def withTestDriver(block: PersistentEntityTestDriver[SimulationCommand, SimulationEvent, Option[SimulationState]] => Unit): Unit = {
    val id = UUID.randomUUID().toString
    val driver = new PersistentEntityTestDriver(system, new SimulationPersistentEntity(), id)
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
      val modelProperties = ModelProperties(ModelType.GRAPH, Some(graphProperties))
      val simulationProperties = SimulationProperties()
      val outcome = driver.run(CreateSimulation(traceId, simulationProperties, modelProperties))
      outcome.replies should ===(Done)
    }

  }
}
