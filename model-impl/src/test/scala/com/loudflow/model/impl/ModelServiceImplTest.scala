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
import com.loudflow.api.HealthResponse
import com.loudflow.domain.model.{GraphProperties, GridProperties, ModelAction, ModelChange, ModelProperties, ModelType}
import com.loudflow.model.api.{CreateModelRequest, ModelService}
import com.loudflow.simulation.api.SimulationService
import org.slf4j.{Logger, LoggerFactory}

class ModelServiceImplTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private final val log: Logger = LoggerFactory.getLogger(classOf[ModelServiceImplTest])

  private lazy val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new ModelApplication(ctx) with LocalServiceLocator // with TestTopicComponents
  }
  implicit private val system: ActorSystem = server.actorSystem
  implicit private val materializer: Materializer = server.materializer

  private lazy val client: ModelService = server.serviceClient.implement[ModelService]

  protected override def beforeAll(): Unit = server

  protected override def afterAll(): Unit = server.stop()

  "model service" should {

    "respond to service health check request" in {
      client.checkServiceHealth.invoke.map { response =>
        log.debug(s"RESPONSE: $response")
        response should ===(HealthResponse("model"))
      }
    }

    "respond to model health check request" in {
      val id = UUID.randomUUID().toString
      client.checkModelHealth(id).invoke.map { response =>
        log.debug(s"RESPONSE: $response")
        response should ===(HealthResponse("model", Some(id.toString)))
      }
    }

    "respond to create model request" in  {
      val gridProperties = GridProperties(10, 10)
      val graphProperties = GraphProperties(Some(gridProperties))
      val modelProperties = ModelProperties(ModelType.Graph, Some(graphProperties))
      val request = CreateModelRequest(modelProperties)
      client.createModel.invoke(request).map { response =>
        log.debug(s"RESPONSE: $response")
        // response should ===(HealthResponse("model", Some(id.toString)))
        assert(true)
      }
    }

/*
    "publish ModelChange messages" in  {
      val source = client.changeTopic.subscribe.atMostOnceSource
      source
        .runWith(TestSink.probe[ModelChange])
        .request(1)
        .expectNext should ===(PubMessage("msg 1"))
      log.debug(s"RESPONSE: $response")
      // response should ===(HealthResponse("model", Some(id.toString)))
      assert(true)
    }
*/

  }

}
