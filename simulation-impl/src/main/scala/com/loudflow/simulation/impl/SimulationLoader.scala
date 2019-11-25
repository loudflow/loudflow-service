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

import play.api.libs.ws.ahc.AhcWSComponents
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.softwaremill.macwire._
import com.loudflow.exception.ServiceExceptionSerializer
import com.loudflow.model.api.ModelService
import com.loudflow.simulation.api.SimulationService

class SimulationLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication = new SimulationApplication(context) {
    override def serviceLocator: ServiceLocator = NoServiceLocator
  }
  override def loadDevMode(context: LagomApplicationContext): LagomApplication = new SimulationApplication(context) with LagomDevModeComponents
  override def describeService = Some(readDescriptor[SimulationService])

}

abstract class SimulationApplication(context: LagomApplicationContext) extends LagomApplication(context) with CassandraPersistenceComponents with LagomKafkaComponents with AhcWSComponents {

  override lazy val lagomServer: LagomServer = serverFor[SimulationService](wire[SimulationServiceImpl])
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = SimulationSerializerRegistry
  override lazy val defaultExceptionSerializer = new ServiceExceptionSerializer(environment)

  lazy val modelService: ModelService = serviceClient.implement[ModelService]

}
