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

import akka.Done
import akka.actor.{Cancellable, ActorSystem}
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntityRegistry, PersistentEntityRef}
import com.loudflow.domain.simulation.{GraphSimulationState, GraphSimulation, SimulationState}
import org.slf4j.{LoggerFactory, Logger}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{MILLISECONDS, Duration}

class GraphSimulationPersistentEntity(implicit val persistentEntityRegistry: PersistentEntityRegistry, val system: ActorSystem, val ec: ExecutionContext) extends SimulationPersistentEntity[GraphSimulationState] with GraphSimulation {

  final val log: Logger = LoggerFactory.getLogger(classOf[GraphSimulationPersistentEntity])
  private final val ref: PersistentEntityRef[SimulationCommand] = persistentEntityRegistry.refFor[GraphSimulationPersistentEntity](entityId)
  private var clock: Option[Cancellable] = None

  def void: Actions = { Actions()
    .onCommand[CreateSimulation, Done] {
      case (CreateSimulation(traceId, simulation, model), ctx, _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][void] received command: CreateSimulation($simulation, $model)")
        ctx.thenPersist(SimulationCreated(entityId, traceId, simulation, model))(_ => ctx.done)
    }
    .onEvent {
      case (SimulationCreated(_, traceId, simulation, model), _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][void] received event: SimulationCreated($simulation, $model)")
        Some(GraphSimulationState(simulation, model))
    }
  }

  def idle: Actions = { Actions()
    .onCommand[DestroySimulation, Done] {
      case (DestroySimulation(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][idle] received command: DestroySimulation()")
        ctx.thenPersist(SimulationDestroyed(entityId, traceId))(_ => ctx.done)
    }
    .onCommand[StartSimulation, Done] {
      case (StartSimulation(traceId), ctx, state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][idle] received command: StartSimulation()")
        state match {
          case Some(s) =>
            val result = create(s.random, s.model, traceId)
            ctx.thenPersist(SimulationStarted(entityId, traceId, result._1, result._2))(_ => ctx.done)
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] SimulationPersistentEntity[$entityId][running] failed due to missing state while handling command: StartSimulation()"))
            ctx.done
        }
    }
    .onCommand[ResumeSimulation, Done] {
      case (ResumeSimulation(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][idle] received command: ResumeSimulation()")
        ctx.thenPersist(SimulationResumed(entityId, traceId))(_ => ctx.done)
    }
    .onEvent {
      case (SimulationDestroyed(_, traceId), _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][idle] received event: SimulationDestroyed()")
        None
    }
    .onEvent {
      case (SimulationStarted(_, traceId, actions, callsMade), state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][idle] received event: SimulationStarted($actions, $callsMade)")
        state.map(s => {
          clock = startClock(s, traceId)
          GraphSimulationState(s.properties, s.model, callsMade)
        })
    }
    .onEvent {
      case (SimulationResumed(_, traceId), state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][idle] received event: SimulationResumed()")
        state.map(s => {
          clock = startClock(s, traceId)
          s.copy(isRunning = true)
        })
    }
  }

  def running: Actions = { Actions()
    .onCommand[DestroySimulation, Done] {
      case (DestroySimulation(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received command: DestroySimulation()")
        ctx.thenPersist(SimulationDestroyed(entityId, traceId))(_ => ctx.done)
    }
    .onCommand[StopSimulation, Done] {
      case (StopSimulation(traceId), ctx, state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received command: StopSimulation()")
        state match {
          case Some(s) =>
            ctx.thenPersist(SimulationStopped(entityId, traceId, destroy(s.model, traceId)))(_ => ctx.done)
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] SimulationPersistentEntity[$entityId][running] failed due to missing state while handling command: StopSimulation()"))
            ctx.done
        }
    }
    .onCommand[PauseSimulation, Done] {
      case (PauseSimulation(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received command: PauseSimulation()")
        ctx.thenPersist(SimulationPaused(entityId, traceId))(_ => ctx.done)
    }
    .onCommand[AdvanceSimulation, Done] {
      case (AdvanceSimulation(traceId), ctx, state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received command: AdvanceSimulation()")
        state match {
          case Some(s) =>
            if (s.isStep) {
              val result = advance(s.time, s.random, s.model, traceId)
              ctx.thenPersist(SimulationAdvanced(entityId, traceId, result._1, result._2))(_ => ctx.done)
            } else
              ctx.thenPersist(SimulationAdvanced(entityId, traceId, Seq.empty, 0))(_ => ctx.done)
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] SimulationPersistentEntity[$entityId][running] failed due to missing state while handling command: AdvanceSimulation()"))
            ctx.done
        }
    }
    .onCommand[UpdateSimulation, Done] {
      case (UpdateSimulation(traceId, changeEvent), ctx, _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received command: UpdateSimulation($changeEvent)")
        ctx.thenPersist(SimulationUpdated(entityId, traceId, changeEvent))(_ => ctx.done)
    }
    .onEvent {
      case (SimulationDestroyed(_, traceId), _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received event: SimulationDestroyed()")
        None
    }
    .onEvent {
      case (SimulationStopped(_, traceId, actions), state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received event: SimulationStopped($actions)")
        state.map(s => {
          clock = stopClock(traceId)
          GraphSimulationState(s.properties, s.model)
        })
    }
    .onEvent {
      case (SimulationPaused(_, traceId), state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received event: SimulationPaused()")
        state.map(s => {
          clock = stopClock(traceId)
          s.copy(isRunning = false)
        })
    }
    .onEvent {
      case (SimulationAdvanced(_, traceId, actions, callsMade), state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received event: SimulationAdvanced($actions, $callsMade)")
        state.map(s => s.copy(ticks = s.ticks + 1, calls = s.calls + callsMade))
    }
    .onEvent {
      case (SimulationUpdated(_, traceId, changeEvent), state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received event: SimulationUpdated($changeEvent)")
        state.map(s => s.copy(model = change(changeEvent, s.model, traceId)))
    }
  }

  def startClock(state: SimulationState, traceId: String): Option[Cancellable] = {
    stopClock(traceId)
    Some(system.scheduler.schedule(Duration(0, MILLISECONDS), Duration(state.properties.interval, MILLISECONDS)) {
      ref.ask(AdvanceSimulation(traceId)).map(_ => {
        log.info("Simulation advanced.")
      })
    })
  }

  def stopClock(traceId: String): Option[Cancellable] = {
    clock.foreach(_.cancel())
    None
  }

}
