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
package com.loudflow.domain.simulation

import com.loudflow.domain.model._

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.Random

trait GraphSimulation extends Simulation[GraphState] with Graph {

  /* ************************************************************************
     PUBLIC METHODS
  ************************************************************************ */

  def create(random: Random, state: GraphState, traceId: String): (List[ModelAction], Int) = {
    val actions = List(CreateModelAction(state.id, traceId, state.properties))
    if (state.properties.entities.nonEmpty)
      addEntityActions(state.properties.entities.toList, random, actions, calls = 0, state, traceId)
    else (List.empty, 0)
  }

  def destroy(state: GraphState, traceId: String): Seq[ModelAction] = {
    val actions = new ListBuffer[ModelAction]()
    actions += DestroyModelAction(state.id, traceId)
  }

  def change(change: ModelChange, state: GraphState, traceId: String): GraphState = change match {
    case ModelCreatedChange(modelId, _, properties) => create(modelId, properties).run(state).value._1
    case ModelDestroyedChange(_, _) => destroy().run(state).value._1
    case EntityAddedChange(_, _, entityType, kind, options) => add(EntityType.fromString(entityType), kind, options).run(state).value._1
    case EntityRemovedChange(_, _, entityId) => remove(entityId).run(state).value._1
    case EntityMovedChange(_, _, entityId, position) => move(entityId, position).run(state).value._1
    case _: EntityDroppedChange => state
    case _: EntityPickedChange => state
  }

  def advance(time: Long, random: Random, state: GraphState, traceId: String): (List[ModelAction], Int) = {
    val actions = new ListBuffer[ModelAction]()
    var calls: Int = 0
    state.properties.entities.foreach(entityProperties => {
      // INCREASE POPULATION
      entityProperties.population.growth.foreach(rate => {
        val entityList = findEntities(entityProperties.entityType, entityProperties.kind, state)
        val count = Math.round(rate * entityList.size / 100)
        val result = addEntityActions(entityProperties, random, count, List.empty[ModelAction], calls = 0, state, traceId)
        actions.appendAll(result._1)
        calls += result._2
      })
      // DECREASE POPULATION
      allEntities(state).foreach(entity => {
        entity.options.lifeSpan.foreach(span => {
          if ((time - entity.created) >= span) actions += RemoveEntityAction(state.id, traceId, entity.id)
        })
      })
    })
    (actions.toList, calls)
  }

  @tailrec
  private def addEntityActions(entityPropertiesList: List[EntityProperties], random: Random, actions: List[ModelAction], calls: Int, state: GraphState, traceId: String): (List[ModelAction], Int) =
    entityPropertiesList match {
      case Nil => (actions, calls)
      case entityProperties :: tail =>
        addEntityAction(entityProperties, random, state, traceId) match {
          case Some(result) =>
            addEntityActions(tail, random, actions :+ result._1, calls + result._2, state, traceId)
          case None =>
            log.warn(s"Failed to create AddEntityAction with entity properties [$entityProperties] because there is no addable position in current model[${state.id}] state.")
            addEntityActions(tail, random, actions, calls, state, traceId)
        }
    }

  @tailrec
  private def addEntityActions(entityProperties: EntityProperties, random: Random, count: Int, actions: List[ModelAction], calls: Int, state: GraphState, traceId: String): (List[ModelAction], Int) =
    if (count == 0) (actions, calls)
    else addEntityAction(entityProperties, random, state, traceId) match {
      case Some(result) =>
        addEntityActions(entityProperties, random, count - 1, actions :+ result._1, calls + result._2, state, traceId)
      case None =>
        log.warn(s"Failed to create AddEntityAction with entity properties [$entityProperties] because there is no addable position in current model[${state.id}] state.")
        addEntityActions(entityProperties, random, count - 1, actions, calls, state, traceId)
    }

  private def addEntityAction(entityProperties: EntityProperties, random: Random, state: GraphState, traceId: String): Option[(AddEntityAction, Int)] = {
    var calls: Int = 0
    val cluster = entityProperties.cluster.map(cluster => {
      calls += 1
      Entity.generateCluster(cluster, random)
    })
    val group = entityProperties.grouping.map(group => {
      calls += 1
      random.nextInt(group + 1)
    })
    val lifeSpan = entityProperties.population.lifeSpanRange.map(life => {
      calls += 1
      life.pick(random)
    })
    val score = entityProperties.score.map(score => {
      calls += 1
      score.span.pick(random)
    })
    val options = EntityOptions(cluster, group, lifeSpan, score)
    val entity = Entity(entityProperties.entityType, entityProperties.kind, options)
    val optionsWithPosition = EntityOptions(cluster, group, lifeSpan, score, randomPosition(entity, random, state))
    val action = AddEntityAction(state.id, traceId, entityProperties.entityType.toString, entityProperties.kind, optionsWithPosition)
    Some((action, calls + 1))
  }

}
