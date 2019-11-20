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

import java.util.UUID
import scalax.collection.edge.Implicits._
import scalax.collection.GraphPredef.EdgeLikeIn
import scalax.collection.mutable.{Graph => ScalaGraph}
import cats.effect.IO

import com.loudflow.domain.model._
import com.loudflow.domain.model.entity.Entity

object GraphLogic {

  /* ************************************************************************
     DEFINITIONS
  ************************************************************************ */

  type Graph = ScalaGraph[Node, EdgeLikeIn]

  def emptyGraph: Graph = ScalaGraph.empty[Node, EdgeLikeIn]

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

  /* ************************************************************************
     PRIVATE METHODS: GRAPH
  ************************************************************************ */

  def recoverGraph(entities: Set[Entity], positions: Set[Position], attachments: Set[(String, String)], connections: Set[(String, String)]): Graph = {
    val g = buildPositionLayer(positions, connections)
    buildEntityLayer(entities, attachments, g)
  }

  def buildPositionLayer(positions: Set[Position], connections: Set[(String, String)]): Graph = {
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

  def buildPositionLayer(properties: GridProperties): Graph = {
    val g = emptyGraph
    if (properties.zCount > 0) {
      val positions = for {
        x <- 1 to properties.xCount
        y <- 1 to properties.yCount
        z <- 1 to properties.zCount
      } yield Position(x, y, z)
      positions.foreach(addPosition(_, g))
      positions.foreach(position => {
        if (properties.cardinalOnly)
          Direction.cardinal3D.foreach(direction => connect(position, Direction.stepInDirection(position, direction, 1.0), g))
        else
          Direction.compass3D.foreach(direction => connect(position, Direction.stepInDirection(position, direction, 1.0), g))
      })
    }
    else {
      val positions = for {
        x <- 1 to properties.xCount
        y <- 1 to properties.yCount
      } yield Position(x, y)
      positions.foreach(addPosition(_, g))
      positions.foreach(position => {
        if (properties.cardinalOnly) {
          Direction.cardinal.foreach(direction => {
            connect(position, Direction.stepInDirection(position, direction, 1.0), g)
          })
        } else
          Direction.compass.foreach(direction => connect(position, Direction.stepInDirection(position, direction, 1.0), g))
      })
      println(g.nodes.length)
      println(g.edges.length)
      println(g.nodes.filter(n => n.value.asInstanceOf[Position] == positions.head).toSeq.length)
      println(g.order)
      println(g.graphSize)
      println(g.size)
    }
    g
  }

  def buildEntityLayer(entities: Set[Entity], attachments: Set[(String, String)], g: Graph): Graph = {
    entities.foreach(e => e.position.foreach(addEntity(e, _, g)))
    val nodes = g.nodes.map(_.value).toSet
    attachments.foreach(attachment => {
      val node1 = nodes.find(node => node.id == attachment._1 && node.isInstanceOf[Entity])
      val node2 = nodes.find(node => node.id == attachment._1 && node.isInstanceOf[Position])
      if (node1.isDefined && node2.isDefined) attach(node1.get.asInstanceOf[Entity], node2.get.asInstanceOf[Position], g)
    })
    g
  }

  def displayGridAsAscii(g: Graph, gridProperties: GridProperties, mapper: Option[Entity] => String): IO[Unit] =
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
            println(mapper(findEntities(Position(x, y, z), g).headOption) mkString " ")
          }
        }
      }
    }
    else {
      IO {
        val builder = StringBuilder.newBuilder
        builder.append("   ")
        (1 to gridProperties.xCount).foreach(col => builder.append("  " + col + " "))
        println(builder)
        builder.clear()
        (1 to gridProperties.xCount).foreach(_ => builder.append("+---"))
        val rowSeparator = "   " + builder.toString() + "+"
        for {
          y <- 1 to gridProperties.yCount
        } yield {
          println(rowSeparator)
          builder.clear()
          if (y < 10) builder.append(y + "  |") else builder.append(y + " |")
          for {
            x <- 1 to gridProperties.xCount
          } yield {
            builder.append(" " + mapper(findEntities(Position(x, y), g).headOption) + " |")
          }
          println(builder)
        }
        println(rowSeparator)
      }
    }

  /* ************************************************************************
     POSITION
  ************************************************************************ */

  def allPositions(g: Graph): Set[Position] = filterPositions(g.nodes.map(_.value).toSet)

  def findPositions(e: Entity, g: Graph): Set[Position] =
    g find e match {
      case Some(node) => filterPositions(node.neighbors.map(_.value))
      case None => Set.empty[Position]
    }

  def filterPositions(nodes: Set[Node]): Set[Position] = nodes.collect { case value: Position => value }

  def filterEntities(nodes: Set[Node]): Set[Entity] = nodes.collect { case value: Entity => value }

  def addPosition(p: Position, g: Graph): Graph = g += p

  def connect(p1: Position, p2: Position, g: Graph): Graph = g += (p1~+p2)(Label.CONNECTION)

  /* ************************************************************************
     ENTITY
  ************************************************************************ */

  def allEntities(g: Graph): Set[Entity] = filterEntities(g.nodes.map(_.value).toSet)

  def findEntities(p: Position, g: Graph): Set[Entity] =
    g find p match {
      case Some(node) => filterEntities(node.neighbors.map(_.value))
      case None => Set.empty[Entity]
    }

  def findEntities(kind: String, g: Graph): Set[Entity] = allEntities(g).filter(_.kind == kind)

  def addEntity(e: Entity, p: Position, g: Graph): Graph = {
    g += e
    attach(e, p, g)
  }

  def moveEntity(e: Entity, p: Position, g: Graph): Graph = {
    detach(e, p, g)
    e.shiftCluster(p).foreach(attach(e, _, g))
    g
  }

  def removeEntity(e: Entity, g: Graph): Graph = remove(e, g)

  def attach(e: Entity, p: Position, g: Graph): Graph = g += (e ~+> p) (Label.ATTACHMENT)

  def detach(e: Entity, p: Position, g: Graph): Graph = {
    g find (e ~+> p) (Label.ATTACHMENT) foreach { edge => g -= edge }
    g
  }

  def remove(node: Node, g: Graph): Graph = {
    g find node foreach { n =>
      n.edges.foreach(g -= _)
      g -= n
    }
    g
  }

}
