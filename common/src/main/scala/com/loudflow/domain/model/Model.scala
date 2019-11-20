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

import cats.effect.IO

trait Model extends Graph {

  def create(id: String, properties: ModelProperties): ModelState = properties.modelType match {
    case ModelType.Graph => createGraph(id, properties)
  }

  def destroy(state: ModelState): ModelState = state match {
    case s: GraphState => destroyGraph(s)
  }

  def update(change: ModelChange, state: ModelState): ModelState = state match {
    case s: GraphState => updateGraph(change, s)
  }

  def add(kind: String, options: Option[EntityOptions] = None, position: Option[Position] = None, state: ModelState): ModelState = state match {
    case s: GraphState => addToGraph(kind, options, position, s)
  }

  def move(entityId: String, position: Option[Position], state: ModelState): ModelState = state match {
    case s: GraphState => moveInGraph(entityId, position, s)
  }

  def remove(entityId: String, state: ModelState): ModelState = state match {
    case s: GraphState => removeFromGraph(entityId, s)
  }

  def asciiDisplay(mapper: Option[Entity] => String, state: ModelState): IO[Unit] = state match {
    case s: GraphState => asciiDisplayGraph(mapper, s)
  }

}
