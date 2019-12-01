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

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{PersistentEntityTestDriver, ServiceTest}
import com.loudflow.domain.model.{GraphProperties, GridProperties, ModelProperties, ModelType}
import com.loudflow.domain.simulation.SimulationProperties
import com.loudflow.service.Command.CommandReply
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class SimulationPersistentEntityTest(implicit ec: ExecutionContext) extends WordSpecLike with Matchers with BeforeAndAfterAll {

  private final val log: Logger = LoggerFactory.getLogger(classOf[SimulationPersistentEntityTest])

  private implicit val system: ActorSystem = ActorSystem("SimulationPersistentEntityTest", JsonSerializerRegistry.actorSystemSetupFor(SimulationSerializerRegistry))
  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new SimulationApplication(ctx) with LocalServiceLocator
  }
  private implicit val registry: PersistentEntityRegistry = server.application.persistentEntityRegistry

  private val traceId = UUID.randomUUID.toString
  private val gridProperties = GridProperties(10, 10)
  private val graphProperties = GraphProperties(Some(gridProperties))
  private val modelProperties = ModelProperties(ModelType.GRAPH, Some(graphProperties))
  private val simulationProperties = SimulationProperties()

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "SimulationPersistentEntity" should {

    "handle CreateSimulation command" in {
      val id = UUID.randomUUID().toString
      val driver = new PersistentEntityTestDriver(system, new SimulationPersistentEntity(), id)
      val created = driver.run(CreateSimulation(traceId, simulationProperties, modelProperties))
      log.debug(s"CREATED: ${created.replies}")
      created.replies should be(Seq(CommandReply(id, traceId, "CreateModel")))
    }

  }
}
