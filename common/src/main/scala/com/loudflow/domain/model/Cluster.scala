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

import com.loudflow.util.JavaRandom

import com.loudflow.util.shuffle

final case class Cluster(positions: Array[Position])
object Cluster {

  def shiftCluster(position: Position, cluster: Array[Position]): Array[Position] = cluster.map(shiftClusterPoint(position, _))

  def recoverCluster(positions: Array[Position], center: Position): Array[Position] = positions.length match {
    case 0 => Array.empty
    case 1 => Array(recoverClusterPoint(positions(0), center))
    case _ => positions.map(recoverClusterPoint(_, center))
  }

  def shiftClusterPoint(position: Position, clusterPoint: Position): Position = Position(position.x + clusterPoint.x, position.y + clusterPoint.y, position.z + clusterPoint.z)

  def recoverClusterPoint(position: Position, center: Position): Position = Position(position.x - center.x, position.y - center.y, position.z - center.z)

  def generateCluster(properties: ClusterProperties, random: JavaRandom): Array[Position] = {
    val size = properties.size.pick(random)
    val cluster = (1 to size).foldLeft(List(Position(0, 0)))((acc, _) => {
      val last = acc.head
      if (properties.is3D) {
        shuffle(Direction.cardinal3D.map(direction => {
          Direction.stepInDirection(last, direction, properties.step)
        }).toSeq.diff(acc), random).head :: acc
      } else {
        shuffle(Direction.cardinal.map(direction => {
          Direction.stepInDirection(last, direction, properties.step)
        }).toSeq.diff(acc), random).head :: acc
      }
    }).toArray
    generateCluster(cluster)
  }

  def generateCluster(cluster: Array[Position]): Array[Position] = {
    val center = cluster.map(location1 => {
      (location1, cluster.map(_.distanceFrom(location1)).sum)
    }).minBy(_._2)._1
    cluster.map(_.shift(center))
  }

}
