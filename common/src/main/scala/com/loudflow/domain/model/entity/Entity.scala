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
package com.loudflow.domain.model.entity

import java.time.Instant
import java.util.UUID

import com.loudflow.domain.model.graph.GraphHelper.Node
import com.loudflow.domain.model.{Direction, EntityAddedChange, Position}
import com.loudflow.util.{JavaRandom, shuffle}
import play.api.libs.json._
import sangria.schema.{Field, InputField, InputObjectType, IntType, ListInputType, ListType, LongType, ObjectType, OptionInputType, OptionType, StringType, fields}

final case class Entity
(
  id: String,
  kind: String,
  position: Option[Position],
  options: EntityOptions,
  created: Long
) extends Node {
  require(if (options.cluster.isDefined) options.cluster.get.length > 1 else true, "Invalid argument 'options.cluster' for Entity.")
  require(if (options.group.isDefined) options.group.get > 0 else true, "Invalid argument 'options.group' for Entity.")
  require(if (options.lifeSpan.isDefined) options.lifeSpan.get > 0 else true, "Invalid argument 'options.group' for Entity.")
  require(created > 0L, "Invalid argument 'created' for Entity.")
  def shiftCluster(): Array[Position] = options.cluster match {
    case Some(cluster) => position match {
        case Some(p) => Entity.shiftCluster(p, cluster)
        case None => cluster
      }
    case None => position match {
      case Some(p) => Array(p)
      case None => Array.empty
    }
  }
  def shiftCluster(position: Position): Array[Position] = options.cluster match {
    case Some(cluster) => Entity.shiftCluster(position, cluster)
    case None => Array(position)
  }
  override def hashCode: Int = id.##
  override def equals(other: Any): Boolean = other match {
    case that: Node => this.id == that.id
    case _ => false
  }
}

object Entity {

  implicit val format: Format[Entity] = Json.format

  def apply(kind: String, position: Position, options: EntityOptions): Entity = new Entity(UUID.randomUUID().toString, kind, Some(position), options, Instant.now().toEpochMilli)
  def apply(kind: String, options: EntityOptions): Entity = new Entity(UUID.randomUUID().toString, kind, None, options, Instant.now().toEpochMilli)

  def generateCluster(properties: ClusterProperties, random: JavaRandom): Array[Position] =
    if (properties.autoGenerate) {
      val size = properties.size.pick(random)
      val cluster = (1 to size).foldLeft(List(Position(0, 0)))((acc, _) => {
        val last = acc.head
        if (properties.is3D) {
          shuffle(Direction.cardinal3D.flatMap(direction => {
            Direction.stepInDirection(last, direction, properties.step)
          }).toSeq.diff(acc), random).head :: acc
        } else {
          shuffle(Direction.cardinal.flatMap(direction => {
            Direction.stepInDirection(last, direction, properties.step)
          }).toSeq.diff(acc), random).head :: acc
        }
      }).toArray
      generateCluster(cluster)
    } else generateCluster(properties.locations)

  def generateCluster(cluster: Array[Position]): Array[Position] = {
    val center = cluster.map(location1 => {
      (location1, cluster.map(_.distanceFrom(location1)).sum)
    }).minBy(_._2)._1
    cluster.map(_.shift(center))
  }

  def shiftCluster(position: Position, cluster: Array[Position]): Array[Position] = cluster.map(shiftClusterPoint(position, _))

  def shiftClusterPoint(position: Position, clusterPoint: Position): Position = Position(position.x + clusterPoint.x, position.y + clusterPoint.y, position.z + clusterPoint.z)

  def recoverCluster(positions: Array[Position], center: Position): Array[Position] = positions.length match {
    case 0 => Array.empty
    case 1 => Array(recoverClusterPoint(positions(0), center))
    case _ => positions.map(recoverClusterPoint(_, center))
  }

  def recoverClusterPoint(position: Position, center: Position): Position = Position(position.x - center.x, position.y - center.y, position.z - center.z)

  val SchemaType =
    ObjectType (
      "EntityType",
      "Data structure defining an entity.",
      fields[Unit, Entity](
        Field("id", StringType, description = Some("Entity identifier."), resolve = _.value.id),
        Field("kind", StringType, description = Some("Entity kind."), resolve = _.value.kind),
        Field("position", OptionType(Position.SchemaType), description = Some("Entity center position."), resolve = _.value.position),
        Field("options", EntityOptions.SchemaType, description = Some("Entity options."), resolve = _.value.options),
        Field("created", LongType, description = Some("Timestamp for when entity was created."), resolve = _.value.created),
      )
    )

}

final case class EntityOptions(cluster: Option[Array[Position]] = None, group: Option[Int] = None, lifeSpan: Option[Int] = None)
object EntityOptions {
  implicit val format: Format[EntityOptions] = Json.format
  def apply(properties: EntityProperties, random: JavaRandom): EntityOptions = {
    val cluster = properties.cluster.map(Entity.generateCluster(_, random))
    val group = properties.grouping.map(g => random.nextInt(g + 1))
    val lifeSpan = properties.population.lifeSpanRange.map(_.pick(random))
    EntityOptions(cluster, group, lifeSpan)
  }
  val SchemaType =
    ObjectType (
      "EntityOptionsType",
      "Data structure defining entity options.",
      fields[Unit, EntityOptions](
        Field("cluster", OptionType(ListType(Position.SchemaType)), description = Some("Entity cluster defined as list of positions."), resolve = { entityOptionsType =>
          entityOptionsType.value.cluster match {
            case Some(positionArray) => positionArray.toSeq
            case None => Seq.empty[Position]
          }
        }),
        Field("group", OptionType(IntType), description = Some("Entity group id."), resolve = _.value.group),
        Field("lifeSpan", OptionType(IntType), description = Some("Entity life span."), resolve = _.value.lifeSpan)
      )
    )
  val SchemaInputType: InputObjectType[EntityOptions] =
    InputObjectType[EntityOptions] (
      "EntityOptionsInputType",
      "Data structure defining entity options.",
      List(
        InputField("cluster", OptionInputType(ListInputType(Position.SchemaInputType)), "Entity cluster defined as list of positions."),
        InputField("group", OptionInputType(IntType), "Entity group id."),
        InputField("lifeSpan", OptionInputType(IntType), "Entity life span.")
      )
    )
}
