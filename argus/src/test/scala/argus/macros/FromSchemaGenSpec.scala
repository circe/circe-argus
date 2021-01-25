package io.circe.argus.macros

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FromSchemaGenSpec extends AnyFlatSpec with Matchers {

  "fromSchemaString" should "build nested schemas (and name them after the field name)" in {
    val code = FromSchemaGen.fromJsonString("""
      {
        "type": "object",
        "properties": {
          "a": {
            "type": "object",
            "properties": {
              "b" : { "type": "string" }
            }
          }
        }
      }
      """,
      "Foo",
      None,
      Some("mypackage")
    )

    val expected = """|package mypackage;
                      |
                      |object Foo {
                      |  class enum extends scala.annotation.StaticAnnotation;
                      |  class union extends scala.annotation.StaticAnnotation;
                      |  case class Root(a: Option[Root.A] = None);
                      |  object Root {
                      |    case class A(b: Option[String] = None)
                      |  }
                      |}""".stripMargin

    code should === (expected)
  }

  it should "build enum types" in {
    val code = FromSchemaGen.fromJsonString("""
      {
        "type": "object",
        "properties": {
          "country": { "enum": ["UK", "USA", "NZ"] }
        }
      }
      """,
      "Foo"
    )

    val expected = """|object Foo {
                      |  class enum extends scala.annotation.StaticAnnotation;
                      |  class union extends scala.annotation.StaticAnnotation;
                      |  case class Root(country: Option[Root.Country] = None);
                      |  object Root {
                      |    @enum sealed trait Country extends scala.Product with scala.Serializable {
                      |      def json: String
                      |    };
                      |    object CountryEnums {
                      |      case object UK extends Country with scala.Product with scala.Serializable {
                      |        val json: String = "\"UK\""
                      |      };
                      |      case object USA extends Country with scala.Product with scala.Serializable {
                      |        val json: String = "\"USA\""
                      |      };
                      |      case object NZ extends Country with scala.Product with scala.Serializable {
                      |        val json: String = "\"NZ\""
                      |      }
                      |    }
                      |  }
                      |}""".stripMargin

    code should === (expected)
  }
}
