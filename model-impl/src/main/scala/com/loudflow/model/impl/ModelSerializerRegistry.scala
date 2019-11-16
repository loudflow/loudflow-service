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

import scala.collection.immutable.Seq
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object ModelSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[CreateModel],
    JsonSerializer[DestroyModel],
    JsonSerializer[ReadModel],
    JsonSerializer[AddEntity],
    JsonSerializer[RemoveEntity],
    JsonSerializer[MoveEntity],
    JsonSerializer[PickEntity],
    JsonSerializer[DropEntity],
    JsonSerializer[ModelCreated],
    JsonSerializer[ModelDestroyed],
    JsonSerializer[EntityAdded],
    JsonSerializer[EntityRemoved],
    JsonSerializer[EntityMoved],
    JsonSerializer[EntityPicked],
    JsonSerializer[EntityDropped]
  )
}
