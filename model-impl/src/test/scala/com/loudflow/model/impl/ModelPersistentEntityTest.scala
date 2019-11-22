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

import akka.Done
import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.slf4j.{Logger, LoggerFactory}
import com.loudflow.domain.model.{GraphProperties, GridProperties, ModelProperties, ModelType}
import com.loudflow.model.api.ReadModelResponse

class ModelPersistentEntityTest extends WordSpecLike with Matchers with BeforeAndAfterAll {

  private final val log: Logger = LoggerFactory.getLogger(classOf[ModelPersistentEntityTest])

  private val system = ActorSystem("GraphModelPersistentEntityTest", JsonSerializerRegistry.actorSystemSetupFor(ModelSerializerRegistry))

  private val traceId = UUID.randomUUID.toString
  private val gridProperties = GridProperties(10, 10)
  private val graphProperties = GraphProperties(Some(gridProperties))
  private val modelProperties = ModelProperties(ModelType.Graph, Some(graphProperties))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "ModelPersistentEntity" should {

    "handle CreateModel command" in {
      val id = UUID.randomUUID().toString
      val driver = new PersistentEntityTestDriver(system, new ModelPersistentEntity(), id)
      val created = driver.run(CreateModel(traceId, modelProperties))
      log.debug(s"CREATED: ${created.replies}")
      created.replies should be(Seq(Done))
    }

    "handle DestroyModel command" in {
      val id = UUID.randomUUID().toString
      val driver = new PersistentEntityTestDriver(system, new ModelPersistentEntity(), id)
      val created = driver.run(CreateModel(traceId, modelProperties))
      log.debug(s"CREATED: ${created.replies}")
      created.replies should be(Seq(Done))
      val destroyed = driver.run(DestroyModel(traceId))
      log.debug(s"DESTROYED: ${destroyed.replies}")
      destroyed.replies should be(Seq(Done))
    }

    "handle ReadModel command" in {
      val id = UUID.randomUUID().toString
      val driver = new PersistentEntityTestDriver(system, new ModelPersistentEntity(), id)
      val created = driver.run(CreateModel(traceId, modelProperties))
      log.debug(s"CREATED: ${created.replies}")
      created.replies should be(Seq(Done))
      val read = driver.run(ReadModel(traceId))
      log.debug(s"READ: ${read.replies}")
      read.replies.head shouldBe a [ReadModelResponse]
    }

    "handle AddEntity command" in {
      val id = UUID.randomUUID().toString
      val driver = new PersistentEntityTestDriver(system, new ModelPersistentEntity(), id)
      val created = driver.run(CreateModel(traceId, modelProperties))
      log.debug(s"CREATED: ${created.replies}")
      created.replies should be(Seq(Done))
      val added = driver.run(AddEntity(traceId, "agent::random"))
      log.debug(s"ADDED: ${added.replies}")
      added.replies should be(Seq(Done))
    }

    "handle MoveEntity command" in {
      val id = UUID.randomUUID().toString
      val driver = new PersistentEntityTestDriver(system, new ModelPersistentEntity(), id)
      val created = driver.run(CreateModel(traceId, modelProperties))
      log.debug(s"CREATED: ${created.replies}")
      created.replies should be(Seq(Done))
      val moved = driver.run(MoveEntity(traceId, UUID.randomUUID().toString))
      log.debug(s"MOVED: ${moved.replies}")
      moved.replies should be(Seq(Done))
    }

    "handle RemoveEntity command" in {
      val id = UUID.randomUUID().toString
      val driver = new PersistentEntityTestDriver(system, new ModelPersistentEntity(), id)
      val created = driver.run(CreateModel(traceId, modelProperties))
      log.debug(s"CREATED: ${created.replies}")
      created.replies should be(Seq(Done))
      val removed = driver.run(RemoveEntity(traceId, UUID.randomUUID().toString))
      log.debug(s"REMOVED: ${removed.replies}")
      removed.replies should be(Seq(Done))
    }

    "handle PickEntity command" in {
      val id = UUID.randomUUID().toString
      val driver = new PersistentEntityTestDriver(system, new ModelPersistentEntity(), id)
      val created = driver.run(CreateModel(traceId, modelProperties))
      log.debug(s"CREATED: ${created.replies}")
      created.replies should be(Seq(Done))
      val removed = driver.run(PickEntity(traceId, UUID.randomUUID().toString, UUID.randomUUID().toString))
      log.debug(s"PICKED: ${removed.replies}")
      removed.replies.head shouldBe a [IllegalStateException]
    }

    "handle DropEntity command" in {
      val id = UUID.randomUUID().toString
      val driver = new PersistentEntityTestDriver(system, new ModelPersistentEntity(), id)
      val created = driver.run(CreateModel(traceId, modelProperties))
      log.debug(s"CREATED: ${created.replies}")
      created.replies should be(Seq(Done))
      val removed = driver.run(DropEntity(traceId, UUID.randomUUID().toString, UUID.randomUUID().toString))
      log.debug(s"DROPPED: ${removed.replies}")
      removed.replies.head shouldBe a [IllegalStateException]
    }

  }

}
