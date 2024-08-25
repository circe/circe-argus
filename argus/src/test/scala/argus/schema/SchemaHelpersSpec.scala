package io.circe.argus.schema

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SchemaHelpersSpec extends AnyFlatSpec with Matchers {
  "SchemaHelper" should "resolve $refs in Json schemas" in {
    val res = SchemaHelpers.resolveRefsFromResources("/relative-uri/schema", "main.json")

    val expectedJson = """
    {
      "type" : "object",
      "description" : "Example from Everit's tests",
      "properties" : {
        "rect" : {
          "type" : "object",
          "properties" : {
            "a" : {
              "type" : "number"
            },
            "b" : {
              "type" : "number"
            },       
            "id" : {
              "type" : "string",
              "maxLength" : 10
            }
          }
        }
      }
    }
    """

    val Right(expected) = io.circe.parser.parse(expectedJson)

    res should ===(Right(expected))
  }
}