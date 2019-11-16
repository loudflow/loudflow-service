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
package com.loudflow.exception

import com.lightbend.lagom.scaladsl.api.transport.{ExceptionMessage, TransportErrorCode, TransportException}

final case class ValidationError(key: String, message: String)

final case class ValidationException[T](validatedObject: T, message: String, errors: Set[ValidationError]) extends TransportException(TransportErrorCode.BadRequest, ValidationException.generateMessage(message, errors))

object ValidationException {

  def generateMessage(message: String, errors: Set[ValidationError]): ExceptionMessage = {
    val details = s"$message\n" + generateErrors(errors)
    new ExceptionMessage("ValidationException", details)
  }

  private def generateErrors(errors: Set[ValidationError]): String = {
    errors.map(error => s"${error.key}: ${error.message}\n").mkString("- ", "- ", "")
  }

}
