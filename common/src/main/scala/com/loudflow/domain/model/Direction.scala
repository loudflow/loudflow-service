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

import play.api.libs.json._

sealed trait Direction {
  def demuxer: String
}

object Direction {

  implicit val readsDiscrete: Reads[Direction] = Reads { json =>
    (JsPath \ "demuxer").read[String].reads(json).flatMap {
      case "north" => JsSuccess(Direction.North)
      case "north-east" => JsSuccess(Direction.NorthEast)
      case "east" => JsSuccess(Direction.East)
      case "south-east" => JsSuccess(Direction.SouthEast)
      case "south" => JsSuccess(Direction.South)
      case "south-west" => JsSuccess(Direction.SouthWest)
      case "west" => JsSuccess(Direction.West)
      case "north-west" => JsSuccess(Direction.NorthWest)
      case "center-above" => JsSuccess(Direction.CenterAbove)
      case "north-above" => JsSuccess(Direction.NorthAbove)
      case "north-east-above" => JsSuccess(Direction.NorthEastAbove)
      case "east-above" => JsSuccess(Direction.EastAbove)
      case "south-east-above" => JsSuccess(Direction.SouthEastAbove)
      case "south-above" => JsSuccess(Direction.SouthAbove)
      case "south-west-above" => JsSuccess(Direction.SouthWestAbove)
      case "west-above" => JsSuccess(Direction.WestAbove)
      case "north-west-above" => JsSuccess(Direction.NorthWestAbove)
      case "center-below" => JsSuccess(Direction.CenterBelow)
      case "north-below" => JsSuccess(Direction.NorthBelow)
      case "north-east-below" => JsSuccess(Direction.NorthEastBelow)
      case "east-below" => JsSuccess(Direction.EastBelow)
      case "south-east-below" => JsSuccess(Direction.SouthEastBelow)
      case "south-below" => JsSuccess(Direction.SouthBelow)
      case "south-west-below" => JsSuccess(Direction.SouthWestBelow)
      case "west-below" => JsSuccess(Direction.WestBelow)
      case "north-west-below" => JsSuccess(Direction.NorthWestBelow)
      case other => JsError(s"Read Direction failed due to unknown type $other.")
    }
  }
  implicit val writesDiscrete: Writes[Direction] = Writes {
    case Direction.North => JsObject(Seq("demuxer" -> JsString("north")))
    case Direction.NorthEast => JsObject(Seq("demuxer" -> JsString("north-east")))
    case Direction.East => JsObject(Seq("demuxer" -> JsString("east")))
    case Direction.SouthEast => JsObject(Seq("demuxer" -> JsString("south-east")))
    case Direction.South => JsObject(Seq("demuxer" -> JsString("south")))
    case Direction.SouthWest => JsObject(Seq("demuxer" -> JsString("south-west")))
    case Direction.West => JsObject(Seq("demuxer" -> JsString("west")))
    case Direction.NorthWest => JsObject(Seq("demuxer" -> JsString("north-west")))
    case Direction.CenterAbove => JsObject(Seq("demuxer" -> JsString("center-above")))
    case Direction.NorthAbove => JsObject(Seq("demuxer" -> JsString("north-above")))
    case Direction.NorthEastAbove => JsObject(Seq("demuxer" -> JsString("north-east-above")))
    case Direction.EastAbove => JsObject(Seq("demuxer" -> JsString("east-above")))
    case Direction.SouthEastAbove => JsObject(Seq("demuxer" -> JsString("south-east-above")))
    case Direction.SouthAbove => JsObject(Seq("demuxer" -> JsString("south-above")))
    case Direction.SouthWestAbove => JsObject(Seq("demuxer" -> JsString("south-west-above")))
    case Direction.WestAbove => JsObject(Seq("demuxer" -> JsString("west-above")))
    case Direction.NorthWestAbove => JsObject(Seq("demuxer" -> JsString("north-west-above")))
    case Direction.CenterBelow => JsObject(Seq("demuxer" -> JsString("center-below")))
    case Direction.NorthBelow => JsObject(Seq("demuxer" -> JsString("north-below")))
    case Direction.NorthEastBelow => JsObject(Seq("demuxer" -> JsString("north-east-below")))
    case Direction.EastBelow => JsObject(Seq("demuxer" -> JsString("east-below")))
    case Direction.SouthEastBelow => JsObject(Seq("demuxer" -> JsString("south-east-below")))
    case Direction.SouthBelow => JsObject(Seq("demuxer" -> JsString("south-below")))
    case Direction.SouthWestBelow => JsObject(Seq("demuxer" -> JsString("south-west-below")))
    case Direction.WestBelow => JsObject(Seq("demuxer" -> JsString("west-below")))
    case Direction.NorthWestBelow => JsObject(Seq("demuxer" -> JsString("north-west-below")))
  }

  val cardinal: Set[Direction] = Set(North, East, South, West)
  val ordinal: Set[Direction] = Set(NorthEast, SouthEast, SouthWest, NorthWest)
  val compass: Set[Direction] = cardinal ++ ordinal
  val cardinal3D: Set[Direction] = Set(North, East, South, West, CenterAbove, NorthAbove, EastAbove, SouthAbove, WestAbove, CenterBelow, NorthBelow, EastBelow, SouthBelow, WestBelow)
  val ordinal3D: Set[Direction] = Set(NorthEast, SouthEast, SouthWest, NorthWest, NorthEastAbove, SouthEastAbove, SouthWestAbove, NorthWestAbove, NorthEastBelow, SouthEastBelow, SouthWestBelow, NorthWestBelow)
  val compass3D: Set[Direction] = cardinal3D ++ ordinal3D

  def stepInDirection(position: Position, direction: Direction, step: Double, xSpan: Option[Span[Double]] = None, ySpan: Option[Span[Double]] = None, zSpan: Option[Span[Double]] = None): Position = direction match {
    case North => Position(position.x, forward(position.y, step, ySpan), position.z)
    case NorthEast => Position(forward(position.x, step, xSpan), forward(position.y, step, ySpan), position.z)
    case East => Position(forward(position.x, step, xSpan), position.y, position.z)
    case SouthEast => Position(forward(position.x, step, xSpan), backward(position.y, step, ySpan), position.z)
    case South => Position(position.x, backward(position.y, step, ySpan), position.z)
    case SouthWest => Position(backward(position.x, step, xSpan), backward(position.y, step, ySpan), position.z)
    case West => Position(backward(position.x, step, xSpan), position.y, position.z)
    case NorthWest => Position(backward(position.x, step, xSpan), forward(position.y, step, ySpan), position.z)
    case CenterAbove => Position(position.x, position.y, forward(position.z, step, zSpan))
    case NorthAbove => Position(position.x, forward(position.y, step, ySpan), forward(position.z, step, zSpan))
    case NorthEastAbove => Position(forward(position.x, step, xSpan), forward(position.y, step, ySpan), forward(position.z, step, zSpan))
    case EastAbove => Position(forward(position.x, step, xSpan), position.y, forward(position.z, step, zSpan))
    case SouthEastAbove => Position(forward(position.x, step, xSpan), backward(position.y, step, ySpan), forward(position.z, step, zSpan))
    case SouthAbove => Position(position.x, backward(position.y, step, ySpan), forward(position.z, step, zSpan))
    case SouthWestAbove => Position(backward(position.x, step, xSpan), backward(position.y, step, ySpan), forward(position.z, step, zSpan))
    case WestAbove => Position(backward(position.x, step, xSpan), position.y, forward(position.z, step, zSpan))
    case NorthWestAbove => Position(backward(position.x, step, xSpan), forward(position.y, step, ySpan), forward(position.z, step, zSpan))
    case CenterBelow => Position(position.x, position.y, backward(position.z, step, zSpan))
    case NorthBelow => Position(position.x, forward(position.y, step, ySpan), backward(position.z, step, zSpan))
    case NorthEastBelow => Position(forward(position.x, step, xSpan), forward(position.y, step, ySpan), backward(position.z, step, zSpan))
    case EastBelow => Position(forward(position.x, step, xSpan), position.y, backward(position.z, step, zSpan))
    case SouthEastBelow => Position(forward(position.x, step, xSpan), backward(position.y, step, ySpan), backward(position.z, step, zSpan))
    case SouthBelow => Position(position.x, backward(position.y, step, ySpan), backward(position.z, step, zSpan))
    case SouthWestBelow => Position(backward(position.x, step, xSpan), backward(position.y, step, ySpan), backward(position.z, step, zSpan))
    case WestBelow => Position(backward(position.x, step, xSpan), position.y, backward(position.z, step, zSpan))
    case NorthWestBelow => Position(backward(position.x, step, xSpan), forward(position.y, step, ySpan), backward(position.z, step, zSpan))
  }
  def forward(value: Double, step: Double, bounds: Option[Span[Double]]): Double = bounds match {
    case Some(span) => Math.min(value + step, span.max)
    case None => value + step
  }
  def backward(value: Double, step: Double, bounds: Option[Span[Double]]): Double = bounds match {
    case Some(span) => Math.max(value - step, span.min)
    case None => value - step
  }

  final case object North extends Direction {
    val demuxer: String = "north"
  }
  final case object NorthEast extends Direction {
    val demuxer: String = "north-east"
  }
  final case object East extends Direction {
    val demuxer: String = "east"
  }
  final case object SouthEast extends Direction {
    val demuxer: String = "south-east"
  }
  final case object South extends Direction {
    val demuxer: String = "south"
  }
  final case object SouthWest extends Direction {
    val demuxer: String = "south-west"
  }
  final case object West extends Direction {
    val demuxer: String = "west"
  }
  final case object NorthWest extends Direction {
    val demuxer: String = "north-west"
  }

  final case object CenterAbove extends Direction {
    val demuxer: String = "center-above"
  }
  final case object NorthAbove extends Direction {
    val demuxer: String = "north-above"
  }
  final case object NorthEastAbove extends Direction {
    val demuxer: String = "north-east-above"
  }
  final case object EastAbove extends Direction {
    val demuxer: String = "east-above"
  }
  final case object SouthEastAbove extends Direction {
    val demuxer: String = "south-east-above"
  }
  final case object SouthAbove extends Direction {
    val demuxer: String = "south-above"
  }
  final case object SouthWestAbove extends Direction {
    val demuxer: String = "south-west-above"
  }
  final case object WestAbove extends Direction {
    val demuxer: String = "west-above"
  }
  final case object NorthWestAbove extends Direction {
    val demuxer: String = "north-west-above"
  }

  final case object CenterBelow extends Direction {
    val demuxer: String = "center-below"
  }
  final case object NorthBelow extends Direction {
    val demuxer: String = "north-below"
  }
  final case object NorthEastBelow extends Direction {
    val demuxer: String = "north-east-below"
  }
  final case object EastBelow extends Direction {
    val demuxer: String = "east-below"
  }
  final case object SouthEastBelow extends Direction {
    val demuxer: String = "south-east-below"
  }
  final case object SouthBelow extends Direction {
    val demuxer: String = "south-below"
  }
  final case object SouthWestBelow extends Direction {
    val demuxer: String = "south-west-below"
  }
  final case object WestBelow extends Direction {
    val demuxer: String = "west-below"
  }
  final case object NorthWestBelow extends Direction {
    val demuxer: String = "north-west-below"
  }

}
