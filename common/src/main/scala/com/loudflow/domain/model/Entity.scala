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

import java.time.Instant
import scala.util.Random
import play.api.libs.json._
import com.wix.accord.dsl._
import com.wix.accord.transform.ValidationTransform

import com.loudflow.domain.model.Graph.Node

final case class Entity
(
  entityType: EntityType.Value,
  kind: String,
  options: EntityOptions,
  created: Long
) extends Node {
  def shiftCluster(): Array[Position] = options.cluster match {
    case Some(cluster) => options.position match {
        case Some(p) => Cluster.shiftCluster(p, cluster)
        case None => cluster
      }
    case None => options.position match {
      case Some(p) => Array(p)
      case None => Array.empty
    }
  }
  def shiftCluster(position: Position): Array[Position] = options.cluster match {
    case Some(cluster) => Cluster.shiftCluster(position, cluster)
    case None => Array(position)
  }
}

object Entity {

  implicit val format: Format[Entity] = Json.format

  implicit val propertiesValidator: ValidationTransform.TransformedValidator[Entity] = validator { properties =>
    if (properties.options.cluster.isDefined) properties.options.cluster.get.length should be > 1
    properties.options.group.each should be > 0
    properties.options.lifeSpan.each should be > 0
    properties.options.score.each should be > 0
    properties.created should be > 0L
  }

  def apply(entityType: EntityType.Value, kind: String, options: EntityOptions): Entity = new Entity(entityType, kind, options, Instant.now().toEpochMilli)

  def generateCluster(properties: ClusterProperties, random: Random): Array[Position] =
    if (properties.is3D)
      if (properties.autoGenerate) Cluster.generateCluster(properties, random)
      else Cluster.generateCluster(properties.locations)
    else if (properties.autoGenerate) Cluster.generateCluster(properties, random)
    else Cluster.generateCluster(properties.locations)

}

final case class EntityOptions(cluster: Option[Array[Position]] = None, group: Option[Int] = None, lifeSpan: Option[Int] = None, score: Option[Int] = None, position: Option[Position] = None)
object EntityOptions { implicit val format: Format[EntityOptions] = Json.format }
