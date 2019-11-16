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
package com.loudflow.model.impl

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import cats.data.{State => StateMonad}
import com.loudflow.domain.model.ModelState

trait ModelPersistentEntity[S <: ModelState] extends PersistentEntity {

  override type Command = ModelCommand
  override type Event = ModelEvent
  override type State = Option[S]

  override def initialState: Option[S] = None

  override def behavior: Behavior = {
    case Some(_) => extant
    case None => void
  }

  def void: Actions
  def extant: Actions

  protected def newState[T](stateMonad: StateMonad[S, T], currentState: State): State = currentState.map(stateMonad.run(_).value._1)

}
