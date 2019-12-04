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

import akka.actor.{ActorSystem, Cancellable}
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntity, PersistentEntityRef, PersistentEntityRegistry}
import com.loudflow.agent.impl.AgentCommand.ReadReply
import com.loudflow.domain.agent.AgentState
import com.loudflow.domain.model._
import com.loudflow.service.Command.CommandReply
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, MILLISECONDS}

class AgentPersistentEntity(implicit val persistentEntityRegistry: PersistentEntityRegistry, val system: ActorSystem, val ec: ExecutionContext) extends PersistentEntity {

  final val log = Logger[AgentPersistentEntity]
  private final val ref: PersistentEntityRef[AgentCommand] = persistentEntityRegistry.refFor[AgentPersistentEntity](entityId)
  private var clock: Option[Cancellable] = None

  override type Command = AgentCommand
  override type Event = AgentEvent
  override type State = Option[AgentState]

  override def initialState: Option[AgentState] = None

  override def behavior: Behavior = {
    case Some(state) => if (state.isActive) active else idle
    case None => void
  }

  def void: Actions = { Actions()
    .onCommand[CreateAgent, CommandReply] {
      case (CreateAgent(traceId, agent, model), ctx, _) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][void] received command: CreateAgent($agent, $model)")
        ctx.thenPersist(AgentCreated(entityId, traceId, agent, model))(_ => ctx.reply(CommandReply(entityId, traceId, "CreateAgent")))
    }
    .onEvent {
      case (AgentCreated(_, traceId, agent, model), _) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][void] received event: AgentCreated($agent, $model)")
        Some(AgentState(agent, agent.seed, ModelState(entityId, model)))
    }
  }

  def idle: Actions = { Actions()
    .onCommand[DestroyAgent, CommandReply] {
      case (DestroyAgent(traceId), ctx, _) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][idle] received command: DestroyAgent()")
        ctx.thenPersist(AgentDestroyed(entityId, traceId))(_ => ctx.reply(CommandReply(entityId, traceId, "DestroyAgent")))
    }
    .onCommand[StartAgent, CommandReply] {
      case (StartAgent(traceId), ctx, state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][idle] received command: StartAgent()")
        state match {
          case Some(s) =>
            ctx.thenPersist(AgentStarted(entityId, traceId, createAgent(s.model, traceId)))(_ => ctx.reply(CommandReply(entityId, traceId, "StartAgent")))
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] AgentPersistentEntity[$entityId][running] failed due to missing state while handling command: StartAgent()"))
            ctx.done
        }
    }
    .onCommand[ResumeAgent, CommandReply] {
      case (ResumeAgent(traceId), ctx, _) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][idle] received command: ResumeAgent()")
        ctx.thenPersist(AgentResumed(entityId, traceId))(_ => ctx.reply(CommandReply(entityId, traceId, "ResumeAgent")))
    }
    .onReadOnlyCommand[ReadAgent, ReadReply] {
      case (ReadAgent(traceId), ctx, state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][idle] received command: ReadAgent()")
        state match {
          case Some(s) => ReadReply(entityId, traceId, s)
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] AgentPersistentEntity[$entityId][idle] failed due to missing state while handling command: ReadAgent()"))
        }
    }
    .onEvent {
      case (AgentDestroyed(_, traceId), _) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][idle] received event: AgentDestroyed()")
        None
    }
    .onEvent {
      case (AgentStarted(_, traceId, actions), state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][idle] received event: AgentStarted($actions)")
        state.map(s => {
          clock = startClock(s, traceId)
          AgentState(s.properties, s.properties.seed, s.model)
        })
    }
    .onEvent {
      case (AgentResumed(_, traceId), state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][idle] received event: AgentResumed()")
        state.map(s => {
          clock = startClock(s, traceId)
          s.copy(isActive = true)
        })
    }
  }

  def active: Actions = { Actions()
    .onCommand[DestroyAgent, CommandReply] {
      case (DestroyAgent(traceId), ctx, _) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received command: DestroyAgent()")
        ctx.thenPersist(AgentDestroyed(entityId, traceId))(_ => ctx.reply(CommandReply(entityId, traceId, "DestroyAgent")))
    }
    .onCommand[StopAgent, CommandReply] {
      case (StopAgent(traceId), ctx, state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received command: StopAgent()")
        state match {
          case Some(s) =>
            ctx.thenPersist(AgentStopped(entityId, traceId, destroyAgent(entityId, s.model, traceId)))(_ => ctx.reply(CommandReply(entityId, traceId, "StopAgent")))
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] AgentPersistentEntity[$entityId][active] failed due to missing state while handling command: StopAgent()"))
            ctx.done
        }
    }
    .onCommand[PauseAgent, CommandReply] {
      case (PauseAgent(traceId), ctx, _) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received command: PauseAgent()")
        ctx.thenPersist(AgentPaused(entityId, traceId))(_ => ctx.reply(CommandReply(entityId, traceId, "PauseAgent")))
    }
    .onCommand[AdvanceAgent, CommandReply] {
      case (AdvanceAgent(traceId), ctx, state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received command: AdvanceAgent()")
        state match {
          case Some(s) =>
            ctx.thenPersist(AgentAdvanced(entityId, traceId, advanceAgent(entityId, s.model, traceId)))(_ => ctx.reply(CommandReply(entityId, traceId, "AdvanceAgent")))
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] AgentPersistentEntity[$entityId][active] failed due to missing state while handling command: AdvanceAgent()"))
            ctx.done
        }
    }
    .onCommand[UpdateAgent, CommandReply] {
      case (UpdateAgent(traceId, changeEvent), ctx, _) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received command: UpdateAgent($changeEvent)")
        ctx.thenPersist(AgentUpdated(entityId, traceId, changeEvent))(_ => ctx.reply(CommandReply(entityId, traceId, "UpdateAgent")))
    }
    .onReadOnlyCommand[ReadAgent, ReadReply] {
      case (ReadAgent(traceId), ctx, state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received command: ReadAgent()")
        state match {
          case Some(s) => ReadReply(entityId, traceId, s)
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] AgentPersistentEntity[$entityId][active] failed due to missing state while handling command: ReadAgent()"))
        }
    }
    .onEvent {
      case (AgentDestroyed(_, traceId), _) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received event: AgentDestroyed()")
        None
    }
    .onEvent {
      case (AgentStopped(_, traceId, actions), state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received event: AgentStopped($actions)")
        state.map(s => {
          clock = stopClock(traceId)
          AgentState(s.properties, s.properties.seed, s.model)
        })
    }
    .onEvent {
      case (AgentPaused(_, traceId), state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received event: AgentPaused()")
        state.map(s => {
          clock = stopClock(traceId)
          s.copy(isActive = false)
        })
    }
    .onEvent {
      case (AgentAdvanced(_, traceId, actions), state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received event: AgentAdvanced($actions)")
        state.map(s => s.copy(ticks = s.ticks + 1))
    }
    .onEvent {
      case (AgentUpdated(_, traceId, changeEvent), state) =>
        log.trace(s"[$traceId] AgentPersistentEntity[$entityId][active] received event: AgentUpdated($changeEvent)")
        state.map(s => s.copy(model = ModelState.update(changeEvent, s.model)))
    }
  }

  private def createAgent(modelState: ModelState, traceId: String): List[ModelAction] = List(AddEntityAction(modelState.id, traceId, "agent::random"))

  private def destroyAgent(id: String, modelState: ModelState, traceId: String): List[ModelAction] = List(RemoveEntityAction(modelState.id, traceId, id))

  private def advanceAgent(id: String, modelState: ModelState, traceId: String): List[ModelAction] = List(MoveEntityAction(modelState.id, traceId, id))

  private def startClock(state: AgentState, traceId: String): Option[Cancellable] = {
    stopClock(traceId)
    Some(system.scheduler.schedule(Duration(0, MILLISECONDS), Duration(state.properties.interval, MILLISECONDS)) {
      ref.ask(AdvanceAgent(traceId)).map(_ => {
        log.info("Agent advanced.")
      })
    })
  }

  private def stopClock(traceId: String): Option[Cancellable] = {
    clock.foreach(_.cancel())
    None
  }

}
