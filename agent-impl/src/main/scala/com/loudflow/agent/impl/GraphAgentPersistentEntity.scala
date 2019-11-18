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

import akka.Done
import akka.actor.{Cancellable, ActorSystem}
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntityRegistry, PersistentEntityRef}
import com.loudflow.domain.agent.{GraphAgentState, GraphAgent, AgentState}
import org.slf4j.{LoggerFactory, Logger}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{MILLISECONDS, Duration}

class GraphAgentPersistentEntity(implicit val persistentEntityRegistry: PersistentEntityRegistry, val system: ActorSystem, val ec: ExecutionContext) extends AgentPersistentEntity[GraphAgentState] with GraphAgent {

  final val log: Logger = LoggerFactory.getLogger(classOf[GraphAgentPersistentEntity])
  private final val ref: PersistentEntityRef[AgentCommand] = persistentEntityRegistry.refFor[GraphAgentPersistentEntity](entityId)
  private var clock: Option[Cancellable] = None

  def void: Actions = { Actions()
    .onCommand[CreateAgent, Done] {
      case (CreateAgent(traceId, agent, model), ctx, _) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][void] received command: CreateAgent($agent, $model)")
        ctx.thenPersist(AgentCreated(entityId, traceId, agent, model))(_ => ctx.done)
    }
    .onEvent {
      case (AgentCreated(_, traceId, agent, model), _) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][void] received event: AgentCreated($agent, $model)")
        Some(GraphAgentState(agent, model))
    }
  }

  def idle: Actions = { Actions()
    .onCommand[DestroyAgent, Done] {
      case (DestroyAgent(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][idle] received command: DestroyAgent()")
        ctx.thenPersist(AgentDestroyed(entityId, traceId))(_ => ctx.done)
    }
    .onCommand[StartAgent, Done] {
      case (StartAgent(traceId), ctx, state) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][idle] received command: StartAgent()")
        state match {
          case Some(s) =>
            val result = create(s.random, s.model, traceId)
            ctx.thenPersist(AgentStarted(entityId, traceId, result._1, result._2))(_ => ctx.done)
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] AgentPersistentEntity[$entityId][running] failed due to missing state while handling command: StartAgent()"))
            ctx.done
        }
    }
    .onCommand[ResumeAgent, Done] {
      case (ResumeAgent(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][idle] received command: ResumeAgent()")
        ctx.thenPersist(AgentResumed(entityId, traceId))(_ => ctx.done)
    }
    .onEvent {
      case (AgentDestroyed(_, traceId), _) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][idle] received event: AgentDestroyed()")
        None
    }
    .onEvent {
      case (AgentStarted(_, traceId, actions, callsMade), state) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][idle] received event: AgentStarted($actions, $callsMade)")
        state.map(s => {
          clock = startClock(s, traceId)
          GraphAgentState(s.properties, s.model, callsMade)
        })
    }
    .onEvent {
      case (AgentResumed(_, traceId), state) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][idle] received event: AgentResumed()")
        state.map(s => {
          clock = startClock(s, traceId)
          s.copy(isActive = true)
        })
    }
  }

  def running: Actions = { Actions()
    .onCommand[DestroyAgent, Done] {
      case (DestroyAgent(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received command: DestroyAgent()")
        ctx.thenPersist(AgentDestroyed(entityId, traceId))(_ => ctx.done)
    }
    .onCommand[StopAgent, Done] {
      case (StopAgent(traceId), ctx, state) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received command: StopAgent()")
        state match {
          case Some(s) =>
            ctx.thenPersist(AgentStopped(entityId, traceId, destroy(entityId, s.model, traceId)))(_ => ctx.done)
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] AgentPersistentEntity[$entityId][running] failed due to missing state while handling command: StopAgent()"))
            ctx.done
        }
    }
    .onCommand[PauseAgent, Done] {
      case (PauseAgent(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received command: PauseAgent()")
        ctx.thenPersist(AgentPaused(entityId, traceId))(_ => ctx.done)
    }
    .onCommand[AdvanceAgent, Done] {
      case (AdvanceAgent(traceId), ctx, state) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received command: AdvanceAgent()")
        state match {
          case Some(s) =>
            val result = advance(entityId, s.random, s.model, traceId)
            ctx.thenPersist(AgentAdvanced(entityId, traceId, result._1, result._2))(_ => ctx.done)
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] AgentPersistentEntity[$entityId][running] failed due to missing state while handling command: AdvanceAgent()"))
            ctx.done
        }
    }
    .onCommand[UpdateAgent, Done] {
      case (UpdateAgent(traceId, changeEvent), ctx, _) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received command: UpdateAgent($changeEvent)")
        ctx.thenPersist(AgentUpdated(entityId, traceId, changeEvent))(_ => ctx.done)
    }
    .onEvent {
      case (AgentDestroyed(_, traceId), _) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received event: AgentDestroyed()")
        None
    }
    .onEvent {
      case (AgentStopped(_, traceId, actions), state) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received event: AgentStopped($actions)")
        state.map(s => {
          clock = stopClock(traceId)
          GraphAgentState(s.properties, s.model)
        })
    }
    .onEvent {
      case (AgentPaused(_, traceId), state) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received event: AgentPaused()")
        state.map(s => {
          clock = stopClock(traceId)
          s.copy(isActive = false)
        })
    }
    .onEvent {
      case (AgentAdvanced(_, traceId, actions, callsMade), state) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received event: AgentAdvanced($actions, $callsMade)")
        state.map(s => s.copy(ticks = s.ticks + 1, calls = s.calls + callsMade))
    }
    .onEvent {
      case (AgentUpdated(_, traceId, changeEvent), state) =>
        log.trace(s"[$traceId] GraphAgentPersistentEntity[$entityId][running] received event: AgentUpdated($changeEvent)")
        state.map(s => s.copy(model = updateModel(changeEvent, s.model, traceId)))
    }
  }

  def startClock(state: AgentState, traceId: String): Option[Cancellable] = {
    stopClock(traceId)
    Some(system.scheduler.schedule(Duration(0, MILLISECONDS), Duration(state.properties.interval, MILLISECONDS)) {
      ref.ask(AdvanceAgent(traceId)).map(_ => {
        log.info("Agent advanced.")
      })
    })
  }

  def stopClock(traceId: String): Option[Cancellable] = {
    clock.foreach(_.cancel())
    None
  }

}
