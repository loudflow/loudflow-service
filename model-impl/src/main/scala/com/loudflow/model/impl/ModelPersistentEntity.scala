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

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.loudflow.domain.model.ModelState
import com.loudflow.model.impl.ModelCommand.ReadReply
import com.loudflow.service.Command.CommandReply
import com.typesafe.scalalogging.Logger

class ModelPersistentEntity extends PersistentEntity {

  final val log = Logger[ModelPersistentEntity]

  override type Command = ModelCommand
  override type Event = ModelEvent
  override type State = Option[ModelState]

  override def initialState: Option[ModelState] = None

  override def behavior: Behavior = {
    case Some(_) => extant
    case None => void
  }

  def void: Actions = { Actions()
    .onCommand[CreateModel, CommandReply] {
      case (CreateModel(traceId, properties), ctx, _) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][void] received command: CreateModel($properties)")
        ctx.thenPersist(ModelCreated(entityId, traceId, properties))(_ => ctx.reply(CommandReply(entityId, traceId, "CreateModel")))
    }
    .onEvent {
      case (ModelCreated(_, traceId, properties), _) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][void] received event: ModelCreated($properties)")
        Some(ModelState.create(entityId, properties))
    }
  }

  def extant: Actions = { Actions()
    .onCommand[DestroyModel, CommandReply] {
      case (DestroyModel(traceId), ctx, _) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received command: DestroyModel()")
        ctx.thenPersist(ModelDestroyed(entityId, traceId))(_ => ctx.reply(CommandReply(entityId, traceId, "DestroyModel")))
    }
    .onCommand[AddEntity, CommandReply] {
      case (AddEntity(traceId, kind, options, position), ctx, _) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received command: AddEntity($kind, $options, $position)")
        ctx.thenPersist(EntityAdded(entityId, traceId, kind, options, position))(_ => ctx.reply(CommandReply(entityId, traceId, "AddEntity")))
    }
    .onCommand[RemoveEntity, CommandReply] {
      case (RemoveEntity(traceId, id), ctx, _) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received command: RemoveEntity($id)")
        ctx.thenPersist(EntityRemoved(entityId, traceId, id))(_ => ctx.reply(CommandReply(entityId, traceId, "RemoveEntity")))
    }
    .onCommand[MoveEntity, CommandReply] {
      case (MoveEntity(traceId, id, position), ctx, _) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received command: MoveEntity($id, $position)")
        ctx.thenPersist(EntityMoved(entityId, traceId, id, position))(_ => ctx.reply(CommandReply(entityId, traceId, "MoveEntity")))
    }
    .onCommand[PickEntity, CommandReply] {
      case (PickEntity(traceId, id, targetId), ctx, _) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received command: PickEntity($id, $targetId)")
        log.warn(s"[$traceId] PickEntity command is not supported.")
        ctx.commandFailed(new IllegalStateException(s"[$traceId] ModelPersistentEntity[$entityId][extant] failed due to unsupported command: PickEntity($id, $targetId)"))
        ctx.done
    }
    .onCommand[DropEntity, CommandReply] {
      case (DropEntity(traceId, id, targetId), ctx, _) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received command: DropEntity($id, $targetId)")
        log.warn(s"[$traceId] DropEntity command is not supported.")
        ctx.commandFailed(new IllegalStateException(s"[$traceId] ModelPersistentEntity[$entityId][extant] failed due to unsupported command: DropEntity($id, $targetId)"))
        ctx.done
    }
    .onReadOnlyCommand[ReadModel, ReadReply] {
      case (ReadModel(traceId), ctx, state) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received command: ReadModel()")
        state match {
          case Some(s) => ctx.reply(ReadReply(entityId, traceId, s))
          case None =>
            ctx.commandFailed(new IllegalStateException(s"[$traceId] ModelPersistentEntity[$entityId][extant] failed due to missing state while handling command: ReadModel()"))
        }
    }
    .onEvent {
      case (ModelDestroyed(_, traceId), _) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received event: ModelDestroyed()")
        None
    }
    .onEvent {
      case (EntityAdded(_, traceId, kind, options, position), state) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received event: EntityAdded($kind, $options, $position)")
        state.map(ModelState.add(kind, options, position, _))
    }
    .onEvent {
      case (EntityRemoved(_, traceId, id), state) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received event: EntityRemoved($id)")
        state.map(ModelState.remove(id, _))
    }
    .onEvent {
      case (EntityMoved(_, traceId, id, position), state) =>
        log.trace(s"[$traceId] ModelPersistentEntity[$entityId][extant] received event: EntityMoved($id, $position)")
        state.map(ModelState.move(id, position, _))
    }
  }

}


