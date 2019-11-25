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

import com.lightbend.lagom.scaladsl.api.transport.{TransportErrorCode, TransportException}
import sangria.execution.{ErrorWithResolver, QueryAnalysisError}

final case class GraphQLException(message: String, override val errorCode: TransportErrorCode = TransportErrorCode.InternalServerError) extends TransportException(errorCode, message)
object GraphQLException {
  def apply(error: ErrorWithResolver): GraphQLException = error match {
    case err: QueryAnalysisError => new GraphQLException(err.resolveError.toString, TransportErrorCode.BadRequest)
    case err => new GraphQLException(err.resolveError.toString)
  }
}
