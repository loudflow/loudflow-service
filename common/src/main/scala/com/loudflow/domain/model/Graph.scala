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
package com.loudflow.domain.model

import java.util.UUID

import cats.effect.IO
import cats.data.{State => StateMonad}
import scalax.collection.mutable.{Graph => ScalaGraph}
import scalax.collection.GraphPredef.EdgeLikeIn
import scalax.collection.edge.Implicits._
import com.loudflow.domain.model.Graph.{G, allEntities}

import scala.util.Random

trait Graph extends Model[GraphState] {

  /* ************************************************************************
     PUBLIC METHODS
  ************************************************************************ */

  def create(id: String, properties: ModelProperties): StateMonad[GraphState, Unit] = StateMonad { state =>
    properties.graph.flatMap(_.grid) match {
      case Some(gridProperties) =>
        (GraphState(id, state.properties).copy(graph = Graph.buildPositionLayer(gridProperties)), ())
      case None =>
        (GraphState(id, state.properties), ())
    }
  }

  def destroy(): StateMonad[GraphState, Unit] = StateMonad { state =>
    (GraphState(state.id, state.properties), ())
  }

  def add(entityType: EntityType.Value, kind: String, options: EntityOptions): StateMonad[GraphState, Unit] = StateMonad { state =>
    val entity = Entity(entityType, kind, options)
    add(entity, state) match {
      case Some(modifiedGraph) => (state.copy(graph = modifiedGraph), ())
      case None => (state, ())
    }
  }

  def move(entityId: String, p: Position): StateMonad[GraphState, Unit] = StateMonad { state =>
    Graph.allEntities(state.graph).find(_.id == entityId) match {
      case Some(entity) =>
        move(entity, p, state) match {
          case Some(modifiedGraph) => (state.copy(graph = modifiedGraph), ())
          case None => (state, ())
        }
      case None => (state, ())
    }
  }

  def remove(entityId: String): StateMonad[GraphState, Unit] = StateMonad { state =>
    Graph.allEntities(state.graph).find(_.id == entityId) match {
      case Some(entity) =>
        remove(entity, state) match {
          case Some(modifiedGraph) => (state.copy(graph = modifiedGraph), ())
          case None => (state, ())
        }
      case None => (state, ())
    }
  }

  def allEntities(state: GraphState): Set[Entity] = Graph.allEntities(state.graph)

  def getEntity(entityId: String, state: GraphState): Option[Entity] = Graph.allEntities(state.graph).find(_.id == entityId)

  def findEntities(entityType: EntityType.Value, kind: String, state: GraphState): Set[Entity] = Graph.findEntities(entityType, kind, state.graph)

  def randomAddablePosition(e: Entity, r: Random, state: GraphState): Option[Position] =
    r.shuffle(Graph.allPositions(state.graph)).find(p => e.shiftCluster(p).forall(isAddable(e, _, state)))

  def randomMovablePosition(entityId: String, r: Random, state: GraphState): Option[Position] =
    getEntity(entityId, state).flatMap(entity => {
      r.shuffle(Graph.allPositions(state.graph)).find(p => entity.shiftCluster(p).forall(p => isAddable(entity, p, state) && isMovable(entity, p, state)))
    })

  def display(mapper: Option[Entity] => String, state: GraphState): IO[Unit] =
    state.gridProperties match {
      case Some(gridProperties) =>
        if (gridProperties.zCount > 0) {
          IO {
            for {
              z <- 1 to gridProperties.zCount
            } yield {
              println(s"LAYER $z")
              for {
                y <- 1 to gridProperties.yCount
                x <- 1 to gridProperties.xCount
              } yield {
                println(mapper(Graph.findEntities(Position(x, y, z), state.graph).headOption) mkString " ")
              }
            }
          }
        }
        else {
          IO {
            for {
              y <- 1 to gridProperties.yCount
              x <- 1 to gridProperties.xCount
            } yield {
              println(mapper(Graph.findEntities(Position(x, y), state.graph).headOption) mkString " ")
            }
          }
        }
      case None => IO(())
    }

  /* ************************************************************************
     PRIVATE METHODS: VALIDATION
  ************************************************************************ */

  private def isAddable(e: Entity, state: GraphState): Boolean =
    e.options.position match {
      case Some(p) => isConcurrencyAllowed(e, p, allowMove = false, state) && isProximityAllowed(e, p, state)
      case None => false
    }

  private def isAddable(e: Entity, p: Position, state: GraphState): Boolean = isConcurrencyAllowed(e, p, allowMove = false, state) && isProximityAllowed(e, p, state)

  private def isMovable(e: Entity, p: Position, state: GraphState): Boolean = isConcurrencyAllowed(e, p, allowMove = true, state) && isProximityAllowed(e, p, state)

  private def isGrowthAllowed(e: Entity, state: GraphState): Boolean =
    state.entityProperties(e.entityType, e.kind) match {
      case Some(entityProperties) => entityProperties.population.range.contains(Graph.findEntities(e.entityType, e.kind, state.graph).size)
      case None => false
    }

  private def isConcurrencyAllowed(e: Entity, p: Position, allowMove: Boolean, state: GraphState): Boolean = {
    e.shiftCluster(p).forall(point => {
      Graph.findEntities(point, state.graph).forall(targetEntity => {
        state.entityProperties(e.entityType, e.kind) match {
          case Some(entityProperties) =>
            entityProperties.concurrence
              .filter(concurrence => concurrence.entityType == targetEntity.entityType && concurrence.kind == targetEntity.kind)
              .forall(concurrence => (concurrence.behavior != ConcurrenceBehavior.Block) && (allowMove || (concurrence.behavior != ConcurrenceBehavior.Move)))
          case None => false
        }
      })
    })
  }

  private def isProximityAllowed(e: Entity, p: Position, state: GraphState): Boolean =
    e.shiftCluster(p).forall(point => {
      state.entityProperties(e.entityType, e.kind) match {
        case Some(entityProperties) =>
          entityProperties.proximity.forall(proximity => {
            Graph.findEntities(proximity.entityType, proximity.kind, state.graph).forall(targetEntity => {
              Graph.findPositions(targetEntity, state.graph).forall(targetPoint => {
                point.distanceFrom(targetPoint) > proximity.distance
              })
            })
          })
        case None => false
      }
    })

  private def isMotionAllowed(e: Entity, p: Position, state: GraphState): Boolean =
    e.shiftCluster(p).forall(point => {
      Graph.findPositions(e, state.graph).forall(bodyPoint => {
        state.entityProperties(e.entityType, e.kind) match {
          case Some(entityProperties) => entityProperties.motion.distance >= point.distanceFrom(bodyPoint)
          case None => false
        }
      })
    })

  /* ************************************************************************
     PRIVATE METHODS: ENTITY
  ************************************************************************ */

  private def add(e: Entity, state: GraphState): Option[G] =
    if (isGrowthAllowed(e, state) && isAddable(e, state))
      Some(Graph.addEntity(e, state.graph))
    else None

  private def move(e: Entity, p: Position, state: GraphState): Option[G] =
    if (isMotionAllowed(e, p, state) && isMovable(e, p, state))
      Some(Graph.moveEntity(e, p, state.graph))
    else None

  private def remove(e: Entity, state: GraphState): Option[G] =
    if (isGrowthAllowed(e, state))
      Some(Graph.removeEntity(e, state.graph))
    else None

}

object Graph {

  /* ************************************************************************
     DEFINITIONS
  ************************************************************************ */

  type G = ScalaGraph[Node, EdgeLikeIn]

  def emptyGraph: G = ScalaGraph.empty[Node, EdgeLikeIn]

  object Label {
    final val CONNECTION: String = "connection"
    final val ATTACHMENT: String = "attachment"
  }

  trait Node {
    val id: String = UUID.randomUUID().toString
    override def hashCode: Int = id.##
    override def equals(other: Any): Boolean = other match {
      case that: Node => this.id == that.id
      case _ => false
    }
  }

  def buildPositionLayer(positions: Set[Position], connections: Set[(String, String)]): G = {
    val g = emptyGraph
    positions.foreach(addPosition(_, g))
    val nodes = allPositions(g)
    connections.foreach(connection => {
      val node1 = nodes.find(_.id == connection._1)
      val node2 = nodes.find(_.id == connection._2)
      if (node1.isDefined && node2.isDefined) connect(node1.get, node2.get, g)
    })
    g
  }

  private def buildPositionLayer(properties: GridProperties): G = {
    val g = emptyGraph
    if (properties.zCount > 0) {
      val positions = for {
        x <- 1 to properties.xCount
        y <- 1 to properties.yCount
        z <- 1 to properties.zCount
      } yield Position(x, y, z)
      positions.foreach(position => {
        Graph.addPosition(position, g)
        if (properties.cardinalOnly)
          Direction.cardinal3D.foreach(direction => Graph.connect(position, Direction.stepInDirection(position, direction, 1.0), g))
        else
          Direction.compass3D.foreach(direction => Graph.connect(position, Direction.stepInDirection(position, direction, 1.0), g))
      })
    }
    else {
      val positions = for {
        x <- 1 to properties.xCount
        y <- 1 to properties.yCount
      } yield Position(x, y)
      positions.foreach(position => {
        Graph.addPosition(position, g)
        if (properties.cardinalOnly)
          Direction.cardinal.foreach(direction => Graph.connect(position, Direction.stepInDirection(position, direction, 1.0), g))
        else
          Direction.compass.foreach(direction => Graph.connect(position, Direction.stepInDirection(position, direction, 1.0), g))
      })
    }
    g
  }

  def buildEntityLayer(entities: Set[Entity], attachments: Set[(String, String)], g: G): G = {
    entities.foreach(entity => addEntity(entity, g))
    val nodes = g.nodes.map(_.value).toSet
    attachments.foreach(attachment => {
      val node1 = nodes.find(node => node.id == attachment._1 && node.isInstanceOf[Entity])
      val node2 = nodes.find(node => node.id == attachment._1 && node.isInstanceOf[Position])
      if (node1.isDefined && node2.isDefined) attach(node1.get.asInstanceOf[Entity], node2.get.asInstanceOf[Position], g)
    })
    g
  }

  /* ************************************************************************
     POSITION
  ************************************************************************ */

  private def allPositions(g: G): Set[Position] = filterPositions(g.nodes.map(_.value).toSet)

  private def findPositions(e: Entity, g: G): Set[Position] =
    g find e match {
      case Some(node) => Graph.filterPositions(node.neighbors.map(_.value))
      case None => Set.empty[Position]
    }

  private def filterPositions(nodes: Set[Graph.Node]): Set[Position] = nodes.collect { case value: Position => value }

  private def filterEntities(nodes: Set[Graph.Node]): Set[Entity] = nodes.collect { case value: Entity => value }

  private def addPosition(p: Position, g: G): G = g += p

  private def connect(p1: Position, p2: Position, g: G): G = g += (p1~+p2)(Label.CONNECTION)

  /* ************************************************************************
     ENTITY
  ************************************************************************ */

  private def allEntities(g: G): Set[Entity] = Graph.filterEntities(g.nodes.map(_.value).toSet)

  private def findEntities(p: Position, g: G): Set[Entity] =
    g find p match {
      case Some(node) => Graph.filterEntities(node.neighbors.map(_.value))
      case None => Set.empty[Entity]
    }

  private def findEntities(entityType: EntityType.Value, kind: String, g: G): Set[Entity] =
    allEntities(g).filter(entity => entity.entityType == entityType && entity.kind == kind)

  private def addEntity(e: Entity, g: G): G =
    e.options.position match {
      case Some(p) =>
        g += e
        attach(e, p, g)
      case None => g
    }

  private def moveEntity(e: Entity, p: Position, g: G): G = {
    detach(e, p, g)
    e.shiftCluster(p).foreach(Graph.attach(e, _, g))
    g
  }

  private def removeEntity(e: Entity, g: G): G = remove(e, g)

  private def attach(e: Entity, p: Position, g: G): G = g += (e ~+> p) (Label.ATTACHMENT)

  private def detach(e: Entity, p: Position, g: G): G = {
    g find (e ~+> p) (Label.ATTACHMENT) foreach { edge => g -= edge }
    g
  }

  private def remove(node: Graph.Node, g: G): G = {
    g find node foreach { n =>
      n.edges.foreach(g -= _)
      g -= n
    }
    g
  }

}
