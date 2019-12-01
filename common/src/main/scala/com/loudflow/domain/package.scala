package com.loudflow

import sangria.schema.{Argument, StringType}

package object domain {

  val IdSchemaInputType = Argument("id", StringType, description = "Identifier")

}
