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
package com.loudflow.domain.agent

import com.loudflow.domain.model.{ModelAction, ModelChange, ModelState}
import com.sun.tools.classfile.TypeAnnotation.Position
import org.slf4j.Logger

import scala.util.Random

trait Agent[S <: ModelState] {

  implicit protected def log: Logger

  /* ************************************************************************
     PUBLIC METHODS
  ************************************************************************ */

  def create(state: S, traceId: String): ModelAction
  def destroy(id: String, state: S, traceId: String): ModelAction
  def change(change: ModelChange, state: S, traceId: String): S
  def advance(id: String, random: Random, state: S, traceId: String): (Seq[ModelAction], Int)

}
