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

import scalax.collection.edge.Implicits._
import scalax.collection.GraphPredef._
import scalax.collection.mutable.{Graph => ScalaGraph}
import cats.effect.IO
import com.loudflow.domain.model._
import com.loudflow.domain.model.entity.Entity
import com.loudflow.util.Span
import org.slf4j.{Logger, LoggerFactory}

object GraphHelper {

  // private final val log: Logger = LoggerFactory.getLogger("GraphLogic")

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
    def id: String
  }

  /* ************************************************************************
     GRAPH
  ************************************************************************ */

  def recoverGraph(entities: Set[Entity], positions: Set[Position], attachments: Set[(String, String)], connections: Set[(String, String)]): Graph = {
    val g = buildPositionLayer(positions, connections)
    buildEntityLayer(entities, attachments, g)
  }

  def buildPositionLayer(positions: Set[Position], connections: Set[(String, String)]): Graph = {
    val g = emptyGraph
    positions.foreach(addPosition(_, g))
    connections.foreach(connection => {
      positions.find(_.id == connection._1).foreach(p => {
        positions.find(_.id == connection._2).foreach(connect(p, _, g))
      })
    })
    g
  }

  def buildPositionLayer(gridProperties: GridProperties): Graph = {
    val g = emptyGraph
    val xSpan = Span(1, gridProperties.xCount.toDouble)
    val ySpan = Span(1, gridProperties.yCount.toDouble)
    if (gridProperties.zCount > 0) build3DPositionLayer(gridProperties, xSpan, ySpan, g)
    else build2DPositionLayer(gridProperties, xSpan, ySpan, g)
  }

  private def build2DPositionLayer(gridProperties: GridProperties, xSpan: Span[Double], ySpan: Span[Double], g: Graph): Graph = {
    val positions = for {
      x <- 1 to gridProperties.xCount
      y <- 1 to gridProperties.yCount
    } yield Position(x, y)
    positions.foreach(addPosition(_, g))
    positions.foreach(p1 => {
      if (gridProperties.cardinalOnly) {
        Direction.cardinal.foreach(direction => {
          Direction.stepInDirection(p1, direction, 1, Some(xSpan), Some(ySpan)).foreach(p2 => {
            positions.find(p => p.x == p2.x && p.y == p2.y).foreach(connect(p1, _, g))
          })
        })
      } else
        Direction.compass.foreach(direction => {
          Direction.stepInDirection(p1, direction, 1, Some(xSpan), Some(ySpan)).foreach(p2 => {
            positions.find(p => p.x == p2.x && p.y == p2.y).foreach(connect(p1, _, g))
          })
        })
    })
    g
  }

  private def build3DPositionLayer(gridProperties: GridProperties, xSpan: Span[Double], ySpan: Span[Double], g: Graph): Graph = {
    val zSpan = Span(1, gridProperties.zCount.toDouble)
    val positions = for {
      x <- 1 to gridProperties.xCount
      y <- 1 to gridProperties.yCount
      z <- 1 to gridProperties.zCount
    } yield Position(x, y, z)
    positions.foreach(addPosition(_, g))
    positions.foreach(p => {
      if (gridProperties.cardinalOnly)
        Direction.cardinal3D.foreach(direction => {
          Direction.stepInDirection(p, direction, 1, Some(xSpan), Some(ySpan), Some(zSpan)).foreach(p2 => {
            positions.find(p => p.x == p2.x && p.y == p2.y && p.z == p2.z).foreach(connect(p, _, g))
          })
        })
      else
        Direction.compass3D.foreach(direction => {
          Direction.stepInDirection(p, direction, 1, Some(xSpan), Some(ySpan), Some(zSpan)).foreach(p2 => {
            positions.find(p => p.x == p2.x && p.y == p2.y && p.z == p2.z).foreach(connect(p, _, g))
          })
        })
    })
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

  def displayGridAsAscii(g: Graph, gridProperties: GridProperties, title: String, mapper: Option[Entity] => String): IO[Unit] = IO {
    if (gridProperties.zCount > 0) {
      for {
        z <- 1 to gridProperties.zCount
      } yield display2DGridAsAscii(g, gridProperties, title, Some(z), mapper)
    }
    else display2DGridAsAscii(g, gridProperties, title, None, mapper)
  }

  private def display2DGridAsAscii(g: Graph, gridProperties: GridProperties, title: String, z: Option[Int] = None, mapper: Option[Entity] => String): Unit = {
    println(title)
    z.foreach(value => println(s"LAYER $value"))
    val builder = StringBuilder.newBuilder
    (1 to gridProperties.xCount).foreach(_ => builder.append("+---"))
    val rowSeparator = "   " + builder.toString() + "+"
    builder.clear()
    builder.append("   ")
    (1 to gridProperties.xCount).foreach(col => builder.append("  " + col + " "))
    val colLabels = builder.toString()
    println(colLabels)
    for {
      y <- 1 to gridProperties.yCount
    } yield {
      println(rowSeparator)
      builder.clear()
      val row = gridProperties.yCount - y + 1
      if (row < 10) builder.append(row + "  |") else builder.append(row + " |")
      for {
        col <- 1 to gridProperties.xCount
      } yield {
        val position = z match {
          case Some(value) => Position(col, row, value)
          case None => Position(col, row)
        }
        builder.append(" " + mapper(findEntities(position, g).headOption) + " |")
      }
      builder.append(" " + row)
      println(builder)
    }
    println(rowSeparator)
    println(colLabels)
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

  private def filterPositions(nodes: Set[Node]): Set[Position] = nodes.collect { case value: Position => value }

  def addPosition(p: Position, g: Graph): Graph = g += p

  def allConnections(g: Graph): Set[EdgeLikeIn[g.NodeT]] = g.edges.map(_.edge).filter(_.label == Label.CONNECTION).toSet

  def findConnections(p: Position, g: Graph): Set[EdgeLikeIn[g.NodeT]] =
    g find p match {
      case Some(n) => n.edges.map(_.edge).filter(_.label == Label.CONNECTION).toSet
      case None => Set.empty
    }

  def findAttachments(p: Position, g: Graph): Set[EdgeLikeIn[g.NodeT]] =
    g find p match {
      case Some(n) => n.edges.map(_.edge).filter(_.label == Label.ATTACHMENT).toSet
      case None => Set.empty
    }

  def connect(p1: Position, p2: Position, g: Graph): Graph = {
    g find p1 foreach { n1 =>
      (g find p2).foreach(n2 => g += (n1.value ~+ n2.value)(Label.CONNECTION))
    }
    g
  }

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

  private def filterEntities(nodes: Set[Node]): Set[Entity] = nodes.collect { case value: Entity => value }

  def addEntity(e: Entity, p: Position, g: Graph): Graph = {
    g += e
    attach(e, p, g)
  }

  def moveEntity(e: Entity, p: Position, g: Graph): Graph = {
    detach(e, g)
    e.shiftCluster(p).foreach(attach(e, _, g))
    g
  }

  def removeEntity(e: Entity, g: Graph): Graph = remove(e, g)

  def allAttachments(g: Graph): Set[EdgeLikeIn[g.NodeT]] = g.edges.map(_.edge).filter(_.label == Label.ATTACHMENT).toSet

  def findAttachments(e: Entity, g: Graph): Set[EdgeLikeIn[g.NodeT]] =
    g find e match {
      case Some(n) => n.edges.map(_.edge).filter(_.label == Label.ATTACHMENT).toSet
      case None => Set.empty
    }

  def attach(e: Entity, p: Position, g: Graph): Graph = {
    (g find p).foreach(n => g += (e ~+> n.value)(Label.ATTACHMENT))
    g
  }

  def detach(e: Entity, g: Graph): Graph = {
    findPositions(e, g).foreach(detach(e, _, g))
    g
  }

  def detach(e: Entity, p: Position, g: Graph): Graph = {
    (g find (e ~+> p)(Label.ATTACHMENT)).foreach(edge => g -= edge)
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
