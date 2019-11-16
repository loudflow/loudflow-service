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

import com.loudflow.domain.model.Graph.{G, buildEntityLayer, Label, emptyGraph}
import play.api.libs.json.{JsSuccess, Format, Reads, Writes, JsValue, Json}

final case class GraphState(id: String, properties: ModelProperties, graph: G = emptyGraph) extends ModelState {
  val demuxer = "graph"
  def entities: Set[Entity] = graph.nodes.map(_.value).collect{case value: Entity => value}.toSet
  def positions: Set[Position] = graph.nodes.map(_.value).collect{case value: Position => value}.toSet
  def attachments: Set[(String, String)] =
    graph.edges.map(_.edge).filter(_.label == Label.ATTACHMENT).map(edge => (edge.head.value.id, edge.last.value.id)).toSet
  def connections: Set[(String, String)] =
    graph.edges.map(_.edge).filter(_.label == Label.CONNECTION).map(edge => (edge.head.value.id, edge.last.value.id)).toSet
  def isEmpty: Boolean = graph.nodes.isEmpty
  def graphProperties: GraphProperties = properties.graph.get
  def gridProperties: Option[GridProperties] = properties.graph.flatMap(_.grid)
}

object GraphState {

  def apply(id: String, properties: ModelProperties): GraphState = new GraphState(id, properties)

  private def recoverGraph(entities: Set[Entity], positions: Set[Position], attachments: Set[(String, String)], connections: Set[(String, String)]): G = {
    val g = Graph.buildPositionLayer(positions, connections)
    buildEntityLayer(entities, attachments, g)
  }

  implicit val reads: Reads[GraphState] = (json: JsValue) => {
    val id = (json \ "id").as[String]
    val properties = (json \ "properties").as[ModelProperties]
    val entities = (json \ "entities").as[Set[Entity]]
    val positions = (json \ "positions").as[Set[Position]]
    val attachments = (json \ "attachments").as[Set[(String, String)]]
    val connections = (json \ "connections").as[Set[(String, String)]]
    val graph = recoverGraph(entities, positions, attachments, connections)
    JsSuccess(new GraphState(id, properties, graph))
  }

  implicit val writes: Writes[GraphState] = (state: GraphState) => Json.obj(
    "id" -> state.id,
    "properties" -> state.properties,
    "entities" -> state.entities,
    "positions" -> state.positions,
    "attachments" -> state.attachments,
    "connections" -> state.connections
  )

  implicit val format: Format[GraphState] = Format(reads, writes)

}
