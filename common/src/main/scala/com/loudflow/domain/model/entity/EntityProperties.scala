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

import com.loudflow.domain.model.Position
import com.loudflow.util.IntSpan
import play.api.libs.json._
import sangria.schema.{BooleanType, EnumType, EnumValue, Field, InputField, InputObjectType, IntType, ListInputType, ListType, ObjectType, OptionInputType, OptionType, StringType, fields}

final case class EntityProperties
(
  kind: String,
  category: EntityCategory.Value,
  description: Option[String],
  population: PopulationProperties,
  motion: MotionProperties,
  proximity: Set[ProximityProperties],
  interactions: Set[InteractionProperties],
  cluster: Option[ClusterProperties],
  grouping: Option[Int]
) {
  require(grouping.isDefined && grouping.get > 0, "Invalid argument 'grouping' for EntityProperties.")
  def interactionProperties(target: String): Set[InteractionProperties] = interactions.filter(_.target.kind == target)
}
object EntityProperties {
  implicit val format: Format[EntityProperties] = Json.format
  val SchemaType =
    ObjectType (
      "EntityPropertiesType",
      "Entity properties.",
      fields[Unit, EntityProperties](
        Field("kind", StringType, description = Some("Entity kind."), resolve = _.value.kind),
        Field("category", EntityCategory.SchemaType, description = Some("Entity category."), resolve = _.value.category),
        Field("description", OptionType(StringType), description = Some("Entity description."), resolve = _.value.description),
        Field("population", PopulationProperties.SchemaType, description = Some("Entity population properties."), resolve = _.value.population),
        Field("motion", MotionProperties.SchemaType, description = Some("Entity motion properties."), resolve = _.value.motion),
        Field("proximity", ListType(ProximityProperties.SchemaType), description = Some("List of entity proximity properties."), resolve = _.value.proximity.toSeq),
        Field("interactions", ListType(InteractionProperties.SchemaType), description = Some("List of entity interaction properties."), resolve = _.value.interactions.toSeq),
        Field("cluster", OptionType(ClusterProperties.SchemaType), description = Some("Entity cluster properties."), resolve = _.value.cluster),
        Field("grouping", OptionType(IntType), description = Some("Entity grouping properties."), resolve = _.value.grouping)
      )
    )
  val SchemaInputType: InputObjectType[EntityProperties] =
    InputObjectType[EntityProperties] (
      "EntityPropertiesInputType",
      "Entity properties.",
      List(
        InputField("kind", StringType, "Entity kind."),
        InputField("category", EntityCategory.SchemaType, "Entity category."),
        InputField("description", OptionInputType(StringType), "Entity description."),
        InputField("population", PopulationProperties.SchemaInputType, "Entity population properties."),
        InputField("motion", MotionProperties.SchemaInputType, "Entity motion properties."),
        InputField("proximity", ListInputType(ProximityProperties.SchemaInputType), "List of entity proximity properties."),
        InputField("interactions", ListInputType(InteractionProperties.SchemaInputType), "List of entity interaction properties."),
        InputField("cluster", OptionInputType(ClusterProperties.SchemaInputType), "Entity cluster properties."),
        InputField("grouping", OptionInputType(IntType), "Entity grouping properties.")
      )
    )
}

object EntityCategory extends Enumeration {
  type EntityCategory = Value
  val AGENT: EntityCategory.Value = Value
  val THING: EntityCategory.Value = Value
  implicit val format: Format[EntityCategory.Value] = Json.formatEnum(this)
  val SchemaType =
    EnumType (
      "EntityCategoryEnum",
      Some("Entity category."),
      List (
        EnumValue("AGENT", value = EntityCategory.AGENT, description = Some("Agent entity category.")),
        EnumValue("THING", value = EntityCategory.THING, description = Some("Thing entity category."))
      )
    )
}

final case class PopulationProperties(range: IntSpan, growth: Option[Int], lifeSpanRange: Option[IntSpan]) {
  require(growth.isDefined && growth.get >= 0, "Invalid argument 'growth' for PopulationProperties..")
  require(lifeSpanRange.isDefined && lifeSpanRange.get.min > 0, "Invalid argument 'lifeSpanRange' for PopulationProperties..")
}
object PopulationProperties {
  implicit val format: Format[PopulationProperties] = Json.format
  val SchemaType =
    ObjectType (
      "PopulationPropertiesType",
      "Properties for population control in model.",
      fields[Unit, PopulationProperties](
        Field("range", IntSpan.SchemaType, description = Some("Limits of model population."), resolve = _.value.range),
        Field("growth", OptionType(IntType), description = Some("Growth rate of model population."), resolve = _.value.growth),
        Field("lifeSpanRange", OptionType(IntSpan.SchemaType), description = Some("Limits of life spans of entities in model."), resolve = _.value.lifeSpanRange)
      )
    )
  val SchemaInputType: InputObjectType[PopulationProperties] =
    InputObjectType[PopulationProperties] (
      "PopulationPropertiesInputType",
      "Properties for population control in model.",
      List(
        InputField("range", IntSpan.SchemaInputType, "Limits of model population."),
        InputField("growth", OptionInputType(IntType), "Growth rate of model population."),
        InputField("lifeSpanRange", OptionInputType(IntSpan.SchemaInputType), "Limits of life spans of entities in model.")
      )
    )
}

final case class ClusterProperties(size: IntSpan = IntSpan(0, 0), step: Int = 0, locations: Array[Position] = Array.empty, is3D: Boolean = false, autoGenerate: Boolean = true) {
  require(autoGenerate && size.min > 0, "Invalid argument 'size.min' for ClusterProperties. For auto-generated clusters, minimum size must be greater than zero.")
  require(autoGenerate && step > 0, "Invalid argument 'step' for ClusterProperties. For auto-generated clusters, step must be greater than zero.")
  require(!autoGenerate && locations.nonEmpty, "Invalid argument 'locations' for ClusterProperties. For pre-defined clusters, locations cannot be empty.")
}
object ClusterProperties {
  implicit val format: Format[ClusterProperties] = Json.format
  val SchemaType =
    ObjectType (
      "ClusterPropertiesType",
      "Properties for defining entity clusters.",
      fields[Unit, ClusterProperties](
        Field("size", IntSpan.SchemaType, description = Some("Limits of model population."), resolve = _.value.size),
        Field("step", IntType, description = Some("Growth rate of model population."), resolve = _.value.step),
        Field("locations", ListType(Position.SchemaType), description = Some("If not autogenerated, then relative positions defining cluster."), resolve = _.value.locations.toList),
        Field("is3D", BooleanType, description = Some("Is cluster 3d or not."), resolve = _.value.is3D),
        Field("autoGenerate", BooleanType, description = Some("Autogenerate flag for cluster."), resolve = _.value.autoGenerate)
      )
    )
  val SchemaInputType: InputObjectType[ClusterProperties] =
    InputObjectType[ClusterProperties] (
      "ClusterPropertiesInputType",
      "Properties for defining entity clusters.",
      List(
        InputField("size", IntSpan.SchemaInputType, "Limits of model population."),
        InputField("step", IntType, "Growth rate of model population."),
        InputField("locations", ListInputType(Position.SchemaInputType), "If not autogenerated, then relative positions defining cluster."),
        InputField("is3D", BooleanType, "Is cluster 3d or not."),
        InputField("autoGenerate", BooleanType, "Autogenerate flag for cluster.")
      )
    )
}

final case class MotionProperties(distance: Int) {
  require(distance > 0, "Invalid argument 'distance' for MotionProperties.")
}
object MotionProperties {
  implicit val format: Format[MotionProperties] = Json.format
  val SchemaType =
    ObjectType (
      "MotionPropertiesType",
      "Properties for defining motion behavior of entity.",
      fields[Unit, MotionProperties](
        Field("distance", IntType, description = Some("Maximum distance that entity can move in one turn."), resolve = _.value.distance)
      )
    )
  val SchemaInputType: InputObjectType[MotionProperties] =
    InputObjectType[MotionProperties] (
      "MotionPropertiesInputType",
      "Properties for defining motion behavior of entity.",
      List(
        InputField("distance", IntType, "Maximum distance that entity can move in one turn.")
      )
    )
}

final case class ProximityProperties(kind: String, distance: Int) {
  require(distance >= 0, "Invalid argument 'distance' for ProximityProperties.")
}
object ProximityProperties {
  implicit val format: Format[ProximityProperties] = Json.format
  val SchemaType =
    ObjectType (
      "ProximityPropertiesType",
      "Properties for defining proximity behavior of entity.",
      fields[Unit, ProximityProperties](
        Field("kind", StringType, description = Some("Entity kind."), resolve = _.value.kind),
        Field("distance", IntType, description = Some("Other entities cannot be closer than this distance."), resolve = _.value.distance)
      )
    )
  val SchemaInputType: InputObjectType[ProximityProperties] =
    InputObjectType[ProximityProperties] (
      "ProximityPropertiesInputType",
      "Properties for defining proximity behavior of entity.",
      List(
        InputField("kind", StringType, "Entity kind."),
        InputField("distance", IntType, "Other entities cannot be closer than this distance.")
      )
    )
}

final case class InteractionProperties(target: Participant, trigger: Option[Participant], result: InteractionResult.Value = InteractionResult.BLOCK_ACTOR, score: Option[Int] = None, scoreDecayRate: Option[Int] = None)
object InteractionProperties {
  implicit val format: Format[InteractionProperties] = Json.format
  val SchemaType =
    ObjectType (
      "InteractionPropertiesType",
      "Properties for defining entity-to-entity interaction behavior.",
      fields[Unit, InteractionProperties](
        Field("target", Participant.SchemaType, description = Some("Target (resident) entity."), resolve = _.value.target),
        Field("trigger", OptionType(Participant.SchemaType), description = Some("If applicable, entity which indirectly triggered the interaction."), resolve = _.value.trigger),
        Field("result", InteractionResult.SchemaType, description = Some("Interaction result."), resolve = _.value.result),
        Field("score", OptionType(IntType), description = Some("Corresponding score for interaction."), resolve = _.value.score),
        Field("scoreDecayRate", OptionType(IntType), description = Some("Decay rate of potential interaction score."), resolve = _.value.scoreDecayRate)
      )
    )
  val SchemaInputType: InputObjectType[InteractionProperties] =
    InputObjectType[InteractionProperties] (
      "InteractionPropertiesInputType",
      "Properties for defining entity-to-entity interaction behavior.",
      List(
        InputField("target", Participant.SchemaInputType, "Target (resident) entity."),
        InputField("trigger", OptionInputType(Participant.SchemaInputType), "If applicable, entity which indirectly triggered the interaction."),
        InputField("result", InteractionResult.SchemaType, "Interaction result."),
        InputField("score", OptionInputType(IntType), "Corresponding score for interaction."),
        InputField("scoreDecayRate", OptionInputType(IntType), "Decay rate of potential interaction score.")
      )
    )
}

final case class Participant(kind: String, score: Option[Int], scoreDecayRate: Option[Int])
object Participant {
  implicit val format: Format[Participant] = Json.format
  val SchemaType =
    ObjectType (
      "ParticipantType",
      "Properties for defining interaction behavior of a participant entity.",
      fields[Unit, Participant](
        Field("kind", StringType, description = Some("Entity kind."), resolve = _.value.kind),
        Field("score", OptionType(IntType), description = Some("Score received by this entity for the interaction."), resolve = _.value.score),
        Field("scoreDecayRate", OptionType(IntType), description = Some("Decay rate of potential interaction score."), resolve = _.value.scoreDecayRate)
      )
    )
  val SchemaInputType: InputObjectType[Participant] =
    InputObjectType[Participant] (
      "ParticipantInputType",
      "Properties for defining interaction behavior of a participant entity.",
      List(
        InputField("kind", StringType, "Entity kind."),
        InputField("score", OptionInputType(IntType), "Score received by this entity for the interaction."),
        InputField("scoreDecayRate", OptionInputType(IntType), "Decay rate of potential interaction score.")
      )
    )
}

object InteractionResult extends Enumeration {
  type InteractionResult = Value
  val SHARE: InteractionResult.Value = Value
  val BLOCK_ACTOR: InteractionResult.Value = Value
  val MOVE_TARGET: InteractionResult.Value = Value
  val REMOVE_ACTOR: InteractionResult.Value = Value
  val REMOVE_TARGET: InteractionResult.Value = Value
  val REMOVE_BOTH: InteractionResult.Value = Value
  implicit val format: Format[InteractionResult.Value] = Json.formatEnum(this)
  val SchemaType =
    EnumType (
      "InteractionResultEnum",
      Some("Result of interaction."),
      List (
        EnumValue("SHARE", value = InteractionResult.SHARE, description = Some("Both entities can share same space.")),
        EnumValue("BLOCK_ACTOR", value = InteractionResult.BLOCK_ACTOR, description = Some("Actor is blocked from entering space.")),
        EnumValue("MOVE_TARGET", value = InteractionResult.MOVE_TARGET, description = Some("Target (resident) is moved (pushed) away from space.")),
        EnumValue("REMOVE_ACTOR", value = InteractionResult.REMOVE_ACTOR, description = Some("Actor is removed from model (killed).")),
        EnumValue("REMOVE_TARGET", value = InteractionResult.REMOVE_TARGET, description = Some("Target (resident) is removed from model (killed).")),
        EnumValue("REMOVE_BOTH", value = InteractionResult.REMOVE_BOTH, description = Some("Both entities are removed from model (killed)."))
      )
    )
}
