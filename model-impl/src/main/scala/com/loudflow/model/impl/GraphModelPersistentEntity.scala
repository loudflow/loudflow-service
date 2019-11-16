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
import org.slf4j.{LoggerFactory, Logger}
import com.loudflow.domain.model.{Graph, GraphState}

class GraphModelPersistentEntity extends ModelPersistentEntity[GraphState] with Graph {

  final val log: Logger = LoggerFactory.getLogger(classOf[GraphModelPersistentEntity])

  override def void: Actions = { Actions()
    .onCommand[CreateModel, Done] {
      case (CreateModel(traceId, properties), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: CreateModel($properties)")
        ctx.thenPersist(ModelCreated(entityId, traceId, properties))(_ => ctx.done)
    }
    .onEvent {
      case (ModelCreated(_, traceId, properties), state) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received event: ModelCreated($properties)")
        newState[Unit](create(entityId, properties), state)
    }
  }

  override def extant: Actions = { Actions()
    .onCommand[DestroyModel, Done] {
      case (DestroyModel(traceId), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: DestroyModel()")
        ctx.thenPersist(ModelDestroyed(entityId, traceId))(_ => ctx.done)
    }
    .onCommand[AddEntity, Done] {
      case (AddEntity(traceId, entityType, kind, options), ctx, _) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received command: AddEntity($entityType, $kind, $options)")
        ctx.thenPersist(EntityAdded(entityId, traceId, entityType, kind, options))(_ => ctx.done)
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
      case (EntityAdded(_, traceId, entityType, kind, options), state) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received event: EntityAdded($entityType, $kind, $options)")
        newState[Unit](add(entityType, kind, options), state)
    }
    .onEvent {
      case (EntityRemoved(_, traceId, id), state) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received event: EntityRemoved($id)")
        newState[Unit](remove(id), state)
    }
    .onEvent {
      case (EntityMoved(_, traceId, id, position), state) =>
        log.trace(s"[$traceId] GraphModelPersistentEntity[$entityId][void] received event: EntityMoved($id, $position)")
        newState[Unit](move(id, position), state)
    }
  }

}


