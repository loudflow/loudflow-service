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
package com.loudflow.domain.agent

import com.loudflow.domain.model._

trait GraphAgent extends Agent[GraphState] with Graph {

  /* ************************************************************************
     PUBLIC METHODS
  ************************************************************************ */

  def create(state: GraphState, traceId: String): (Seq[ModelAction], Int) = {
    val entity = Entity(EntityType.Agent, "random", EntityOptions())
    randomAddablePosition(entity, state) match {
      case Some(position) =>
        (Seq(AddEntityAction(state.id, traceId, EntityType.Agent.toString, "random", EntityOptions(None, None, None, None, Some(position)))), 1)
      case None => (Seq.empty, 0)
    }
  }

  def destroy(id: String, state: GraphState, traceId: String): Seq[ModelAction] = Seq(RemoveEntityAction(state.id, traceId, id))

  def advance(id: String,  state: GraphState, traceId: String): (Seq[ModelAction], Int) =
    randomMovablePosition(id, state) match {
      case Some(position) =>
        (Seq(MoveEntityAction(state.id, traceId, id, position)), 1)
      case None => (Seq.empty, 0)
    }

  def updateModel(change: ModelChange, state: GraphState, traceId: String): GraphState = change match {
    case ModelCreatedChange(modelId, _, properties) => create(modelId, properties).run(state).value._1
    case ModelDestroyedChange(_, _) => destroy().run(state).value._1
    case EntityAddedChange(_, _, entityType, kind, options) => add(EntityType.fromString(entityType), kind, options).run(state).value._1
    case EntityRemovedChange(_, _, entityId) => remove(entityId).run(state).value._1
    case EntityMovedChange(_, _, entityId, position) => move(entityId, position).run(state).value._1
    case _: EntityDroppedChange => state
    case _: EntityPickedChange => state
  }

}
