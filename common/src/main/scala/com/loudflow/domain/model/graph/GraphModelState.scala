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
package com.loudflow.domain.model.graph

import play.api.libs.json._
import com.loudflow.domain.model._
import com.loudflow.domain.model.entity._
import com.loudflow.domain.model.graph.GraphHelper._
import com.loudflow.util.shuffle
import org.slf4j.{Logger, LoggerFactory}

final case class GraphModelState(id: String, properties: ModelProperties, seed: Long, graph: Graph = emptyGraph) extends ModelState {
  val demuxer = "graph"
  def entities: Set[Entity] = GraphHelper.allEntities(graph)
  def positions: Set[Position] = GraphHelper.allPositions(graph)
  def attachments: Set[(String, String)] = GraphHelper.allAttachments(graph).map(edge => (edge.head.value.id, edge.last.value.id))
  def connections: Set[(String, String)] = GraphHelper.allConnections(graph).map(edge => (edge.head.value.id, edge.last.value.id))
  def isEmpty: Boolean = graph.nodes.isEmpty
  def graphProperties: GraphProperties = properties.graph.get
  def gridProperties: Option[GridProperties] = properties.graph.flatMap(_.grid)
  def getPosition(id: String): Option[Position] = positions.find(_.id == id)
  def entityPositions(id: String): Set[Position] = attachments.filter(_._1 == id).flatMap(value => getPosition(value._2))
  def entitiesAtPosition(id: String): Set[Entity] = attachments.filter(_._2 == id).flatMap(value => getEntity(value._1))
  def connectionsToPosition(id: String): Set[Position] = attachments.filter(_._1 == id).flatMap(value => getPosition(value._2))
}

object GraphModelState {

  private final val log: Logger = LoggerFactory.getLogger("GraphModelState")

  implicit val reads: Reads[GraphModelState] = (json: JsValue) => {
    val id = (json \ "id").as[String]
    val properties = (json \ "properties").as[ModelProperties]
    val seed = (json \ "seed").as[Long]
    val entities = (json \ "entities").as[Set[Entity]]
    val positions = (json \ "positions").as[Set[Position]]
    val attachments = (json \ "attachments").as[Set[(String, String)]]
    val connections = (json \ "connections").as[Set[(String, String)]]
    val graph = recoverGraph(entities, positions, attachments, connections)
    JsSuccess(new GraphModelState(id, properties, seed, graph))
  }

  implicit val writes: Writes[GraphModelState] = (state: GraphModelState) => Json.obj(
    "id" -> state.id,
    "properties" -> state.properties,
    "seed" -> state.seed,
    "entities" -> state.entities,
    "positions" -> state.positions,
    "attachments" -> state.attachments,
    "connections" -> state.connections
  )

  implicit val format: Format[GraphModelState] = Format(reads, writes)

  def apply(id: String, properties: ModelProperties): GraphModelState = new GraphModelState(id, properties, properties.seed)

  /* ************************************************************************
     PUBLIC METHODS
  ************************************************************************ */

  def create(id: String, properties: ModelProperties): GraphModelState = properties.graph.flatMap(_.grid) match {
    case Some(gridProperties) => GraphModelState(id, properties).copy(graph = buildPositionLayer(gridProperties))
    case None => GraphModelState(id, properties)
  }

  def destroy(state: GraphModelState): GraphModelState = GraphModelState(state.id, state.properties)

  def update(change: ModelChange, state: GraphModelState): GraphModelState = change match {
    case ModelCreatedChange(modelId, _, properties) => create(modelId, properties)
    case ModelDestroyedChange(_, _) => destroy(state)
    case EntityAddedChange(_, _, kind, options, position) => add(kind, options, position, state)
    case EntityRemovedChange(_, _, entityId) => remove(entityId, state)
    case EntityMovedChange(_, _, entityId, position) => move(entityId, position, state)
    case _: EntityDroppedChange => state
    case _: EntityPickedChange => state
  }

  def add(kind: String, options: Option[EntityOptions] = None, position: Option[Position] = None, state: GraphModelState): GraphModelState = options match {
    case Some(o) =>
      position match {
        case Some(p) => add(kind, o, p, state)
        case None => add(kind, o, state)
      }
    case None =>
      position match {
        case Some(p) => add(kind, p, state)
        case None => add(kind, state)
      }
  }

  def move(entityId: String, position: Option[Position] = None, state: GraphModelState): GraphModelState = state.getEntity(entityId) match {
    case Some(e) =>
      position match {
        case Some(p) => move(e, p, state)
        case None => move(e, state)
      }
    case None => state
  }

  def remove(entityId: String, state: GraphModelState): GraphModelState = state.getEntity(entityId) match {
    case Some(e) => remove(e, state)
    case None => state
  }

  /* ************************************************************************
     PRIVATE METHODS: VALIDATION
  ************************************************************************ */

  private def isAddable(e: Entity, p: Position, state: GraphModelState): Boolean = {
    log.trace(s"ENTER isAddable(${e.toString}, ${p.toString}, [state])")
    val result = isInteractionAllowed(e, p, allowMove = false, state) && isProximityAllowed(e, p, state)
    log.trace(s"EXIT  isAddable(${e.toString}, ${p.toString}, [state]) => $result")
    result
  }

  private def isMovable(e: Entity, p: Position, state: GraphModelState): Boolean = {
    log.trace(s"ENTER isMovable(${e.toString}, ${p.toString}, [state])")
    val result = isInteractionAllowed(e, p, allowMove = true, state) && isProximityAllowed(e, p, state)
    log.trace(s"EXIT  isMovable(${e.toString}, ${p.toString}, [state]) => $result")
    result
  }

  private def isGrowthAllowed(e: Entity, state: GraphModelState): Boolean = {
    log.trace(s"ENTER isGrowthAllowed(${e.toString}, [state])")
    val result = state.entityProperties(e.kind) match {
      case Some(entityProperties) => entityProperties.population.range.contains(GraphHelper.findEntities(e.kind, state.graph).size)
      case None => true
    }
    log.trace(s"ENTER isGrowthAllowed(${e.toString}, [state]) => $result")
    result
  }

  private def isInteractionAllowed(e: Entity, p: Position, allowMove: Boolean, state: GraphModelState): Boolean = {
    log.trace(s"ENTER isInteractionAllowed(${e.toString}, ${p.toString}, $allowMove, [state])")
    val result = e.shiftCluster(p).forall(point => {
      GraphHelper.findEntities(point, state.graph).forall(target => {
        state.entityProperties(e.kind) match {
          case Some(entityProperties) =>
            entityProperties.interactionProperties(target.kind)
              .forall(interactionProperties => (interactionProperties.result != InteractionResult.ActorBlocked) && (allowMove || (interactionProperties.result != InteractionResult.TargetMoved)))
          case None => false
        }
      })
    })
    log.trace(s"EXIT  isInteractionAllowed(${e.toString}, ${p.toString}, $allowMove, [state]) => $result")
    result
  }

  private def isProximityAllowed(e: Entity, p: Position, state: GraphModelState): Boolean = {
    log.trace(s"ENTER isProximityAllowed(${e.toString}, ${p.toString}, [state])")
    val result = e.shiftCluster(p).forall(point => {
      state.entityProperties(e.kind) match {
        case Some(entityProperties) =>
          entityProperties.proximity.forall(proximity => {
            GraphHelper.findEntities(proximity.kind, state.graph).forall(targetEntity => {
              GraphHelper.findPositions(targetEntity, state.graph).forall(targetPoint => {
                point.distanceFrom(targetPoint) > proximity.distance
              })
            })
          })
        case None => true
      }
    })
    log.trace(s"EXIT  isProximityAllowed(${e.toString}, ${p.toString}, [state]) => $result")
    result
  }

  private def isMotionAllowed(e: Entity, p: Position, state: GraphModelState): Boolean = {
    log.trace(s"ENTER isMotionAllowed(${e.toString}, ${p.toString}, [state])")
    val result = e.shiftCluster(p).forall(point => {
      GraphHelper.findPositions(e, state.graph).forall(bodyPoint => {
        state.entityProperties(e.kind) match {
          case Some(entityProperties) => entityProperties.motion.distance >= point.distanceFrom(bodyPoint)
          case None => true
        }
      })
    })
    log.trace(s"ENTER isMotionAllowed(${e.toString}, ${p.toString}, [state]) => $result")
    result
  }

  private def randomAddablePosition(e: Entity, state: GraphModelState): Option[Position] = {
    log.trace(s"ENTER randomAddablePosition(${e.toString}, [state])")
    val p = shuffle(GraphHelper.allPositions(state.graph).toSeq, state.random).find(p => e.shiftCluster(p).forall(isAddable(e, _, state)))
    log.trace(s"EXIT  randomAddablePosition(${e.toString}, [state]) => ${p.toString}")
    p
  }

  private def randomMovablePosition(e: Entity, state: GraphModelState): Option[Position] = {
    log.trace(s"ENTER randomMovablePosition(${e.toString}, [state])")
    val p = shuffle(GraphHelper.allPositions(state.graph).toSeq, state.random).find(p => e.shiftCluster(p).forall(p => isAddable(e, p, state) && isMovable(e, p, state)))
    log.trace(s"EXIT  randomMovablePosition(${e.toString}, [state]) => ${p.toString}")
    p
  }

  /* ************************************************************************
     PRIVATE METHODS: ENTITY
  ************************************************************************ */

  private def add(kind: String, state: GraphModelState): GraphModelState = {
    log.trace(s"ENTER add($kind, [state])")
    state.properties.entityProperties(kind) match {
      case Some(properties) =>
        add(kind, entity.EntityOptions(properties, state.random), state.copy(seed = state.random.seed.get()))
      case None =>
        add(kind, EntityOptions(), state)
    }
  }

  private def add(kind: String, options: EntityOptions, state: GraphModelState): GraphModelState = {
    log.trace(s"ENTER add($kind, ${options.toString}, [state])")
    add(Entity(kind, options), state)
  }

  private def add(kind: String, p: Position, state: GraphModelState): GraphModelState = {
    log.trace(s"ENTER add($kind, ${p.toString}, [state])")
    state.properties.entityProperties(kind) match {
      case Some(properties) =>
        add(kind, entity.EntityOptions(properties, state.random), p, state.copy(seed = state.random.seed.get()))
      case None =>
        add(kind, EntityOptions(), p, state)
    }
  }

  private def add(kind: String, options: EntityOptions, p: Position, state: GraphModelState): GraphModelState = {
    log.trace(s"ENTER add($kind, ${options.toString}, ${p.toString}, [state])")
    add(Entity(kind, options), p, state)
  }

  private def add(e: Entity, state: GraphModelState): GraphModelState = {
    log.trace(s"ENTER add(${e.toString}, [state])")
    randomAddablePosition(e, state) match {
      case Some(p) => add(e, p, state)
      case None => state
    }
  }

  private def add(e: Entity, p: Position, state: GraphModelState): GraphModelState = {
    log.trace(s"ENTER add(${e.toString}, ${p.toString}, [state])")
    if (isGrowthAllowed(e, state) && isAddable(e, p, state)) {
      val modifiedGraph = GraphHelper.addEntity(e, p, state.graph)
      state.copy(graph = modifiedGraph)
    } else state
  }

  private def move(e: Entity, state: GraphModelState): GraphModelState = {
    log.trace(s"ENTER move(${e.toString}, [state])")
    randomMovablePosition(e, state) match {
      case Some(p) =>
        val modifiedGraph = GraphHelper.moveEntity(e, p, state.graph)
        state.copy(graph = modifiedGraph)
      case None => state
    }
  }

  private def move(e: Entity, p: Position, state: GraphModelState): GraphModelState = {
    log.trace(s"ENTER move(${e.toString}, ${p.toString}, [state])")
    if (isMotionAllowed(e, p, state) && isMovable(e, p, state)) {
      val modifiedGraph = GraphHelper.moveEntity(e, p, state.graph)
      state.copy(graph = modifiedGraph)
    }
    else state
  }

  private def remove(e: Entity, state: GraphModelState): GraphModelState =
    if (isGrowthAllowed(e, state)) {
      val modifiedGraph = GraphHelper.removeEntity(e, state.graph)
      state.copy(graph = modifiedGraph)
    } else state

}
