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

import cats.data.{State => StateMonad}
import org.slf4j.Logger

trait Model[S <: ModelState] {

  implicit def log: Logger

  def create(properties: ModelProperties): StateMonad[S, Unit]
  def destroy(): StateMonad[S, Unit]
  def add(entityType: EntityType.Value, kind: String): StateMonad[S, Unit]
  def add(entityType: EntityType.Value, kind: String, options: EntityOptions): StateMonad[S, Unit]
  def add(entityType: EntityType.Value, kind: String, options: EntityOptions, position: Position): StateMonad[S, Unit]
  def move(entityId: String): StateMonad[S, Unit]
  def move(entityId: String, position: Position): StateMonad[S, Unit]
  def remove(entityId: String): StateMonad[S, Unit]
}

object Model {

  def getStateMonad[S <: ModelState, T](value: T): StateMonad[S, T] = StateMonad.pure[S, T](value)
  def getStateMonad[S <: ModelState]: StateMonad[S, Unit] = getStateMonad[S, Unit](())

}
