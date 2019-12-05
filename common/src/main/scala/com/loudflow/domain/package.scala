package com.loudflow

import sangria.schema.{Argument, StringType}

package object domain {

  val IdSchemaInputType: Argument[String] = Argument("id", StringType, description = "Identifier")

}
