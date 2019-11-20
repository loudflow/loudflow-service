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

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import org.slf4j.{Logger, LoggerFactory}
import com.loudflow.domain.model.ModelState

class ModelPersistentEntity extends PersistentEntity {

  final val log: Logger = LoggerFactory.getLogger(classOf[ModelPersistentEntity])

  override type Command = ModelCommand
  override type Event = ModelEvent
  override type State = Option[ModelState]

  override def initialState: Option[ModelState] = None

  override def behavior: Behavior = {
    case Some(_) => extant
    case None => void
  }

  def void: Actions = { Actions()
    .onCommand[CreateModel, Done] {
      case (CreateModel(traceId, properties), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: CreateModel($properties)")
        ctx.thenPersist(ModelCreated(entityId, traceId, properties))(_ => ctx.done)
    }
    .onEvent {
      case (ModelCreated(_, traceId, properties), _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received event: ModelCreated($properties)")
        Some(ModelState.create(entityId, properties))
    }
  }

  def extant: Actions = { Actions()
    .onCommand[DestroyModel, Done] {
      case (DestroyModel(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: DestroyModel()")
        ctx.thenPersist(ModelDestroyed(entityId, traceId))(_ => ctx.done)
    }
    .onCommand[AddEntity, Done] {
      case (AddEntity(traceId, kind, options, position), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: AddEntity($kind, $options, $position)")
        ctx.thenPersist(EntityAdded(entityId, traceId, kind, options, position))(_ => ctx.done)
    }
    .onCommand[RemoveEntity, Done] {
      case (RemoveEntity(traceId, id), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: RemoveEntity($id)")
        ctx.thenPersist(EntityRemoved(entityId, traceId, id))(_ => ctx.done)
    }
    .onCommand[MoveEntity, Done] {
      case (MoveEntity(traceId, id, position), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: MoveEntity($id, $position)")
        ctx.thenPersist(EntityMoved(entityId, traceId, id, position))(_ => ctx.done)
    }
    .onCommand[PickEntity, Done] {
      case (PickEntity(traceId, id, targetId), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: PickEntity($id, $targetId)")
        log.warn(s"[$traceId] PickEntity command is not supported.")
        ctx.done
    }
    .onCommand[DropEntity, Done] {
      case (DropEntity(traceId, id, targetId), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: DropEntity($id, $targetId)")
        log.warn(s"[$traceId] DropEntity command is not supported.")
        ctx.done
    }
    .onEvent {
      case (ModelDestroyed(_, traceId), _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received event: ModelDestroyed()")
        None
    }
    .onEvent {
      case (EntityAdded(_, traceId, kind, options, position), state) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received event: EntityAdded($kind, $options, $position)")
        state.map(ModelState.add(kind, options, position, _))
    }
    .onEvent {
      case (EntityRemoved(_, traceId, id), state) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received event: EntityRemoved($id)")
        state.map(ModelState.remove(id, _))
    }
    .onEvent {
      case (EntityMoved(_, traceId, id, position), state) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received event: EntityMoved($id, $position)")
        state.map(ModelState.move(id, position, _))
    }
  }

}


