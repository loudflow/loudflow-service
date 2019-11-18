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
import akka.actor.{ActorSystem, Cancellable}
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntity, PersistentEntityRef, PersistentEntityRegistry}
import com.loudflow.domain.model._
import com.loudflow.domain.simulation.SimulationState
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, MILLISECONDS}

class SimulationPersistentEntity[S <: ModelState](implicit val persistentEntityRegistry: PersistentEntityRegistry, val system: ActorSystem, val ec: ExecutionContext) extends PersistentEntity with Model[S] {

  final val log: Logger = LoggerFactory.getLogger(classOf[SimulationPersistentEntity[S]])
  private final val ref: PersistentEntityRef[SimulationCommand] = persistentEntityRegistry.refFor[SimulationPersistentEntity[S]](entityId)
  private var clock: Option[Cancellable] = None

  override type Command = SimulationCommand
  override type Event = SimulationEvent
  override type State = Option[SimulationState[S]]

  override def initialState: Option[SimulationState[S]] = None

  override def behavior: Behavior = {
    case Some(state) => if (state.isRunning) running else idle
    case None => void
  }

  def void: Actions = { Actions()
    .onCommand[CreateSimulation, Done] {
      case (CreateSimulation(traceId, simulation, model), ctx, _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][void] received command: CreateSimulation($simulation, $model)")
        ctx.thenPersist(SimulationCreated(entityId, traceId, simulation, model))(_ => ctx.done)
    }
    .onEvent {
      case (SimulationCreated(_, traceId, simulation, model), _) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][void] received event: SimulationCreated($simulation, $model)")
        Some(SimulationState[S](simulation, simulation.seed, ModelState(model).asInstanceOf[S]))
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
            ctx.thenPersist(SimulationStarted(entityId, traceId, createModel(s.model, traceId)))(_ => ctx.done)
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
      case (SimulationStarted(_, traceId, actions), state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][idle] received event: SimulationStarted($actions)")
        state.map(s => {
          clock = startClock(s, traceId)
          SimulationState[S](s.properties, s.properties.seed, s.model)
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
            ctx.thenPersist(SimulationStopped(entityId, traceId, destroyModel(s.model, traceId)))(_ => ctx.done)
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
            ctx.thenPersist(SimulationAdvanced(entityId, traceId, advanceModel(s.time, s.model, traceId)))(_ => ctx.done)
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
          SimulationState[S](s.properties, s.properties.seed, s.model)
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
      case (SimulationAdvanced(_, traceId, actions), state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received event: SimulationAdvanced($actions)")
        state.map(s => s.copy(ticks = s.ticks + 1))
    }
    .onEvent {
      case (SimulationUpdated(_, traceId, changeEvent), state) =>
        log.trace(s"[$traceId] GraphSimulationPersistentEntity[$entityId][running] received event: SimulationUpdated($changeEvent)")
        state.map(s => s.copy(model = updateModel(changeEvent, s.model, traceId)))
    }
  }

  def createModel(modelState: S, traceId: String): List[ModelAction] = {
    val createAction = CreateModelAction(modelState.properties.id, traceId, modelState.properties)
    val addActions = modelState.properties.entities.map(properties => AddEntityAction(modelState.properties.id, traceId, properties.entityType.toString, properties.kind)).toList
    createAction +: addActions
  }

  def destroyModel(modelState: S, traceId: String): List[ModelAction] = List(DestroyModelAction(modelState.properties.id, traceId))

  def advanceModel(time: Long, modelState: S, traceId: String): List[ModelAction] =
    modelState.properties.entities.flatMap(entityProperties => {
      // INCREASE POPULATION
      val addActions = entityProperties.population.growth match {
        case Some(rate) =>
          val entityList = modelState.findEntities(entityProperties.entityType, entityProperties.kind)
          val count = Math.round(rate * entityList.size / 100)
          (1 to count).map(_ => AddEntityAction(modelState.properties.id, traceId, entityProperties.entityType.toString, entityProperties.kind)).toList
        case None => List.empty[ModelAction]
      }
      // DECREASE POPULATION
      val removeActions = modelState.entities.flatMap(entity => {
        entity.options.lifeSpan match {
          case Some(span) =>
            if ((time - entity.created) >= span) Some(RemoveEntityAction(modelState.properties.id, traceId, entity.id)) else None
          case None => None
        }
      }).toList
      (addActions :: removeActions).asInstanceOf[List[ModelAction]]
    }).toList

  def updateModel(change: ModelChange, modelState: S, traceId: String): S = change match {
    case ModelCreatedChange(_, _, properties) => create(properties).run(modelState).value._1
    case ModelDestroyedChange(_, _) => destroy().run(modelState).value._1
    case EntityAddedChange(_, _, entityType, kind, options) => add(EntityType.fromString(entityType), kind, options).run(modelState).value._1
    case EntityRemovedChange(_, _, entityId) => remove(entityId).run(modelState).value._1
    case EntityMovedChange(_, _, entityId, position) => move(entityId, position).run(modelState).value._1
    case _: EntityDroppedChange => modelState
    case _: EntityPickedChange => modelState
  }

  def startClock(state: SimulationState[S], traceId: String): Option[Cancellable] = {
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
