package com.loudflow.model.impl

import com.loudflow.domain.model.entity._
import com.loudflow.domain.model._
import com.loudflow.domain.model.graph.GraphModelState
import com.loudflow.model.impl.ModelCommand.{CommandReply, ReadReply}
import com.loudflow.util.{DoubleSpan, IntSpan}
import sangria.macros.derive._
import sangria.schema._

object ModelSchema {

  val IntSpanType: ObjectType[Unit, IntSpan] =
    deriveObjectType[Unit, IntSpan](
      ObjectTypeDescription("Data structure defining integer min/max limits with some helper functions."),
      DocumentField("min", "Minimum value of integer span."),
      DocumentField("max", "Maximum value of integer span."))

  val DoubleSpanType: ObjectType[Unit, DoubleSpan] =
    deriveObjectType[Unit, DoubleSpan](
      ObjectTypeDescription("Data structure defining double min/max limits with some helper functions."),
      DocumentField("min", "Minimum value of double span."),
      DocumentField("max", "Maximum value of double span."))

  val EntityCategoryEnum =
    EnumType (
      "EntityCategoryEnum",
      Some("Entity category."),
      List (
        EnumValue("AGENT", value = EntityCategory.AGENT, description = Some("Agent entity category.")),
        EnumValue("THING", value = EntityCategory.THING, description = Some("Thing entity category."))
      )
    )

  val PositionType: ObjectType[Unit, Position] =
    deriveObjectType[Unit, Position](
      ObjectTypeDescription("Data structure defining positions in model."),
      DocumentField("x", "X-coordinate of position."),
      DocumentField("y", "Y-coordinate of position."),
      DocumentField("z", "Z-coordinate of position."))

  val EntityOptionsType =
    ObjectType (
      "EntityOptionsType",
      "Data structure defining entity options.",
      fields[Unit, EntityOptions](
        Field("cluster", OptionType(ListType(PositionType)), description = Some("Entity cluster defined as list of positions."), resolve = { entityOptionsType =>
          entityOptionsType.value.cluster match {
            case Some(positionArray) => positionArray.toSeq
            case None => Seq.empty[Position]
          }
        }),
        Field("group", OptionType(IntType), description = Some("Entity group id."), resolve = _.value.group),
        Field("lifeSpan", OptionType(IntType), description = Some("Entity life span."), resolve = _.value.lifeSpan)
      )
    )

  val EntityType =
    ObjectType (
      "EntityType",
      "Data structure defining an entity.",
      fields[Unit, Entity](
        Field("id", StringType, description = Some("Entity identifier."), resolve = _.value.id),
        Field("kind", StringType, description = Some("Entity kind."), resolve = _.value.kind),
        Field("position", OptionType(PositionType), description = Some("Entity center position."), resolve = _.value.position),
        Field("options", EntityOptionsType, description = Some("Entity options."), resolve = _.value.options),
        Field("created", LongType, description = Some("Timestamp for when entity was created."), resolve = _.value.created),
      )
    )

  val PopulationPropertiesType =
    ObjectType (
      "PopulationPropertiesType",
      "Properties for population control in model.",
      fields[Unit, PopulationProperties](
        Field("range", IntSpanType, description = Some("Limits of model population."), resolve = _.value.range),
        Field("growth", OptionType(IntType), description = Some("Growth rate of model population."), resolve = _.value.growth),
        Field("lifeSpanRange", OptionType(IntSpanType), description = Some("Limits of life spans of entities in model."), resolve = _.value.lifeSpanRange)
      )
    )

  val ClusterPropertiesType =
    ObjectType (
      "ClusterPropertiesType",
      "Properties for defining entity clusters.",
      fields[Unit, ClusterProperties](
        Field("size", IntSpanType, description = Some("Limits of model population."), resolve = _.value.size),
        Field("step", IntType, description = Some("Growth rate of model population."), resolve = _.value.step),
        Field("locations", ListType(PositionType), description = Some("If not autogenerated, then relative positions defining cluster."), resolve = _.value.locations.toList),
        Field("is3D", BooleanType, description = Some("Is cluster 3d or not."), resolve = _.value.is3D),
        Field("autoGenerate", BooleanType, description = Some("Autogenerate flag for cluster."), resolve = _.value.autoGenerate)
      )
    )

  val MotionPropertiesType =
    ObjectType (
      "MotionPropertiesType",
      "Properties for defining motion behavior of entity.",
      fields[Unit, MotionProperties](
        Field("distance", IntType, description = Some("Maximum distance that entity can move in one turn."), resolve = _.value.distance)
      )
    )

  val ProximityPropertiesType =
    ObjectType (
      "ProximityPropertiesType",
      "Properties for defining proximity behavior of entity.",
      fields[Unit, ProximityProperties](
        Field("kind", StringType, description = Some("Entity kind."), resolve = _.value.kind),
        Field("distance", IntType, description = Some("Other entities cannot be closer than this distance."), resolve = _.value.distance)
      )
    )

  val ParticipantType =
    ObjectType (
      "ParticipantType",
      "Properties for defining interaction behavior of a participant entity.",
      fields[Unit, Participant](
        Field("kind", StringType, description = Some("Entity kind."), resolve = _.value.kind),
        Field("score", OptionType(IntType), description = Some("Score received by this entity for the interaction."), resolve = _.value.score),
        Field("scoreDecayRate", OptionType(IntType), description = Some("Decay rate of potential interaction score."), resolve = _.value.scoreDecayRate)
      )
    )

  val InteractionResultEnum =
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

  val InteractionPropertiesType =
    ObjectType (
      "InteractionPropertiesType",
      "Properties for defining entity-to-entity interaction behavior.",
      fields[Unit, InteractionProperties](
        Field("target", ParticipantType, description = Some("Target (resident) entity."), resolve = _.value.target),
        Field("trigger", OptionType(ParticipantType), description = Some("If applicable, entity which indirectly triggered the interaction."), resolve = _.value.trigger),
        Field("result", InteractionResultEnum, description = Some("Interaction result."), resolve = _.value.result),
        Field("score", OptionType(IntType), description = Some("Corresponding score for interaction."), resolve = _.value.score),
        Field("scoreDecayRate", OptionType(IntType), description = Some("Decay rate of potential interaction score."), resolve = _.value.scoreDecayRate)
      )
    )

  val EntityPropertiesType =
    ObjectType (
      "EntityPropertiesType",
      "Entity properties.",
      fields[Unit, EntityProperties](
        Field("kind", StringType, description = Some("Entity kind."), resolve = _.value.kind),
        Field("category", EntityCategoryEnum, description = Some("Entity category."), resolve = _.value.category),
        Field("description", OptionType(StringType), description = Some("Entity description."), resolve = _.value.description),
        Field("population", PopulationPropertiesType, description = Some("Entity population properties."), resolve = _.value.population),
        Field("motion", MotionPropertiesType, description = Some("Entity motion properties."), resolve = _.value.motion),
        Field("proximity", ListType(ProximityPropertiesType), description = Some("List of entity proximity properties."), resolve = _.value.proximity.toSeq),
        Field("interactions", ListType(InteractionPropertiesType), description = Some("List of entity interaction properties."), resolve = _.value.interactions.toSeq),
        Field("cluster", OptionType(ClusterPropertiesType), description = Some("Entity cluster properties."), resolve = _.value.cluster),
        Field("grouping", OptionType(IntType), description = Some("Entity grouping properties."), resolve = _.value.grouping)
      )
    )

  val ModelTypeEnum =
    EnumType (
      "ModelTypeEnum",
      Some("Type of model."),
      List (
        EnumValue("GRAPH", value = ModelType.GRAPH, description = Some("Graph-based model."))
      )
    )

  val GridPropertiesType =
    ObjectType (
      "GridPropertiesType",
      "Properties for defining a graph-based model with grid-based positions.",
      fields[Unit, GridProperties](
        Field("rows", IntType, description = Some("Number of rows in grid."), resolve = _.value.rows),
        Field("cols", IntType, description = Some("Number of columns in grid."), resolve = _.value.cols),
        Field("layers", IntType, description = Some("Number of layers if 3D grid."), resolve = _.value.layers),
        Field("cardinalOnly", BooleanType, description = Some("Flag for connecting grid positions in cardinal directions only."), resolve = _.value.cardinalOnly)
      )
    )

  val GraphPropertiesType =
    ObjectType (
      "GraphPropertiesType",
      "Properties for defining a graph-based model.",
      fields[Unit, GraphProperties](
        Field("grid", OptionType(GridPropertiesType), description = Some("Grid properties for graph."), resolve = _.value.grid)
      )
    )

  val ModelPropertiesType =
    ObjectType (
      "ModelPropertiesType",
      "Model properties.",
      fields[Unit, ModelProperties](
        Field("modelType", ModelTypeEnum, description = Some("Type of model."), resolve = _.value.modelType),
        Field("graph", OptionType(GraphPropertiesType), description = Some("Graph properties for model."), resolve = _.value.graph),
        Field("seed", LongType, description = Some("Random number generator seed for the model."), resolve = _.value.seed),
        Field("entities", ListType(EntityPropertiesType), description = Some("List of entity properties for each entity kind used in the model."), resolve = _.value.entities.toSeq)
      )
    )

  val ModelStateType =
    InterfaceType (
      "ModelStateType",
      "Model state.",
      fields[Unit, ModelState](
        Field("id", StringType, description = Some("Model identifier."), resolve = _.value.id),
        Field("seed", LongType, description = Some("Model seed."), resolve = _.value.seed),
        Field("properties", ModelPropertiesType, description = Some("Model properties."), resolve = _.value.properties),
        Field("entities", ListType(EntityType), description = Some("Model entities."), resolve = _.value.entities.toSeq)
      )
    )

  val GraphModelStateType =
    ObjectType (
      "GraphModelStateType",
      "Graph model state.",
      interfaces[Unit, GraphModelState](ModelStateType),
      fields[Unit, GraphModelState](
        Field("id", StringType, description = Some("Graph model identifier."), resolve = _.value.id),
        Field("seed", LongType, description = Some("Graph model seed."), resolve = _.value.seed),
        Field("properties", ModelPropertiesType, description = Some("Graph model properties."), resolve = _.value.properties),
        Field("entities", ListType(EntityType), description = Some("Graph model entities."), resolve = _.value.entities.toSeq),
        Field("positions", ListType(PositionType), description = Some("Graph model positions."), resolve = _.value.positions.toSeq)
      )
    )

  val CommandReplyType =
    ObjectType (
      "CommandReplyType",
      "Command accepted reply.",
      fields[Unit, CommandReply](
        Field("id", StringType, description = Some("Persistent entity identifier."), resolve = _.value.id),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("command", StringType, description = Some("Command name."), resolve = _.value.command)
      )
    )

  val ReadReplyType =
    ObjectType (
      "ReadReplyType",
      "Read command reply.",
      fields[Unit, ReadReply](
        Field("id", StringType, description = Some("Persistent entity identifier."), resolve = _.value.id),
        Field("traceId", StringType, description = Some("Trace identifier."), resolve = _.value.traceId),
        Field("state", ModelStateType, description = Some("Command name."), resolve = _.value.state)
      )
    )

  val IntSpanInputType: InputObjectType[IntSpan] =
    InputObjectType[IntSpan] (
      "IntSpanInputType",
      "Data structure defining integer min/max limits with some helper functions.",
      List(
        InputField("min", IntType, "Minimum value of integer span."),
        InputField("max", IntType, "Maximum value of integer span.")
      )
    )

  val PositionInputType: InputObjectType[Position] =
    InputObjectType[Position] (
      "PositionInputType",
      "Data structure defining positions in model.",
      List(
        InputField("x", FloatType, "X-coordinate of position."),
        InputField("y", FloatType, "Y-coordinate of position."),
        InputField("z", FloatType, "Z-coordinate of position.")
      )
    )

  val PopulationPropertiesInputType: InputObjectType[PopulationProperties] =
    InputObjectType[PopulationProperties] (
      "PopulationPropertiesInputType",
      "Properties for population control in model.",
      List(
        InputField("range", IntSpanInputType, "Limits of model population."),
        InputField("growth", OptionInputType(IntType), "Growth rate of model population."),
        InputField("lifeSpanRange", OptionInputType(IntSpanInputType), "Limits of life spans of entities in model.")
      )
    )

  val MotionPropertiesInputType: InputObjectType[MotionProperties] =
    InputObjectType[MotionProperties] (
      "MotionPropertiesInputType",
      "Properties for defining motion behavior of entity.",
      List(
        InputField("distance", IntType, "Maximum distance that entity can move in one turn.")
      )
    )

  val ProximityPropertiesInputType: InputObjectType[ProximityProperties] =
    InputObjectType[ProximityProperties] (
      "ProximityPropertiesInputType",
      "Properties for defining proximity behavior of entity.",
      List(
        InputField("kind", StringType, "Entity kind."),
        InputField("distance", IntType, "Other entities cannot be closer than this distance.")
      )
    )

  val ClusterPropertiesInputType: InputObjectType[ClusterProperties] =
    InputObjectType[ClusterProperties] (
      "ClusterPropertiesInputType",
      "Properties for defining entity clusters.",
      List(
        InputField("size", IntSpanInputType, "Limits of model population."),
        InputField("step", IntType, "Growth rate of model population."),
        InputField("locations", ListInputType(PositionInputType), "If not autogenerated, then relative positions defining cluster."),
        InputField("is3D", BooleanType, "Is cluster 3d or not."),
        InputField("autoGenerate", BooleanType, "Autogenerate flag for cluster.")
      )
    )

  val ParticipantInputType: InputObjectType[Participant] =
    InputObjectType[Participant] (
      "ParticipantInputType",
      "Properties for defining interaction behavior of a participant entity.",
      List(
        InputField("kind", StringType, "Entity kind."),
        InputField("score", OptionInputType(IntType), "Score received by this entity for the interaction."),
        InputField("scoreDecayRate", OptionInputType(IntType), "Decay rate of potential interaction score.")
      )
    )

  val InteractionPropertiesInputType: InputObjectType[InteractionProperties] =
    InputObjectType[InteractionProperties] (
      "InteractionPropertiesInputType",
      "Properties for defining entity-to-entity interaction behavior.",
      List(
        InputField("target", ParticipantInputType, "Target (resident) entity."),
        InputField("trigger", OptionInputType(ParticipantInputType), "If applicable, entity which indirectly triggered the interaction."),
        InputField("result", InteractionResultEnum, "Interaction result."),
        InputField("score", OptionInputType(IntType), "Corresponding score for interaction."),
        InputField("scoreDecayRate", OptionInputType(IntType), "Decay rate of potential interaction score.")
      )
    )

  val EntityPropertiesInputType: InputObjectType[EntityProperties] =
    InputObjectType[EntityProperties] (
      "EntityPropertiesInputType",
      "Entity properties.",
      List(
        InputField("kind", StringType, "Entity kind."),
        InputField("category", EntityCategoryEnum, "Entity category."),
        InputField("description", OptionInputType(StringType), "Entity description."),
        InputField("population", PopulationPropertiesInputType, "Entity population properties."),
        InputField("motion", MotionPropertiesInputType, "Entity motion properties."),
        InputField("proximity", ListInputType(ProximityPropertiesInputType), "List of entity proximity properties."),
        InputField("interactions", ListInputType(InteractionPropertiesInputType), "List of entity interaction properties."),
        InputField("cluster", OptionInputType(ClusterPropertiesInputType), "Entity cluster properties."),
        InputField("grouping", OptionInputType(IntType), "Entity grouping properties.")
      )
    )

  val GridPropertiesInputType: InputObjectType[GridProperties] =
    InputObjectType[GridProperties] (
      "GridPropertiesInputType",
      "Properties for defining a graph-based model with grid-based positions.",
      List(
        InputField("rows", IntType, "Number of rows in grid."),
        InputField("cols", IntType, "Number of columns in grid."),
        InputField("layers", IntType, "Number of layers if 3D grid."),
        InputField("cardinalOnly", BooleanType, "Flag for connecting grid positions in cardinal directions only.")
      )
    )

  val GraphPropertiesInputType: InputObjectType[GraphProperties] =
    InputObjectType[GraphProperties] (
      "GraphPropertiesInputType",
      "Properties for defining a graph-based model.",
      List(
        InputField("grid", OptionInputType(GridPropertiesInputType), "Grid properties for graph.")
      )
    )

  val ModelPropertiesInputType: InputObjectType[ModelProperties] =
    InputObjectType[ModelProperties] (
      "ModelPropertiesInputType",
      "Model properties.",
      List(
        InputField("modelType", ModelTypeEnum, "Type of model."),
        InputField("graph", OptionInputType(GraphPropertiesInputType), "Graph properties for model."),
        InputField("seed", LongType, "Random number generator seed for the model."),
        InputField("entities", ListInputType(EntityPropertiesInputType), "List of entity properties for each entity kind used in the model.")
      )
    )

  val IdInputType = Argument("id", StringType, description = "Identifier")

}
