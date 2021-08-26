package io.circe.argus.schema

import io.circe.{ACursor, Json, JsonNumber, JsonObject}
import io.circe.parser.parse
import cats.instances.all._
import cats.syntax.all._
import scala.io.Source

object SchemaHelpers {
  def resolveRefs(schema: Json, subschemas: Map[String, Json]): ResolutionResult =
    schema.foldWith(Resolver(schema, subschemas))

  def resolveRefsFromResources(dir: String, main: String): ResolutionResult = {
    val parts = dir.split("/")
    val files = getFilenamesRec(parts.init.mkString("/"), parts.last)

    val schemas = files.flatMap { path =>
      val contents = Source.fromInputStream(getClass.getResourceAsStream(path)).mkString
      parse(contents).map((path.drop(dir.length + 1), _)).toOption
    }.toMap

    resolveRefs(schemas(main), schemas)
  }
  

  def getFilenamesRec(base: String, path: String): List[String] = {
    val resource = getClass.getResource(base + "/" + path)
    val file = new java.io.File(resource.getPath)

    if (file.exists) {
      if (file.isDirectory) {
        file.listFiles.toList.flatMap(f => getFilenamesRec(base + "/" + file.getName, f.getName))
      } else {
        List(base + "/" + file.getName)
      }
    } else {
      Nil
    }
  }

  case class ResolutionError(msg: String) extends RuntimeException(msg)

  type ResolutionResult = Either[ResolutionError, Json]

  case class Resolver(self: Json, subschemas: Map[String, Json])
    extends Json.Folder[ResolutionResult] {
    def onNull: ResolutionResult = Right(Json.Null)
    def onNumber(value: JsonNumber): ResolutionResult = Right(Json.fromJsonNumber(value))
    def onBoolean(value: Boolean): ResolutionResult = Right(Json.fromBoolean(value))
    def onString(value: String): ResolutionResult = Right(Json.fromString(value))
    def onArray(value: Vector[Json]): ResolutionResult = value.traverse(_.foldWith(this)).map(Json.fromValues(_))
    def onObject(value: JsonObject): ResolutionResult = value.toVector.traverse {
      case ("$ref", value) => resolve(value)
      case (other, value) => value.foldWith(this).map(json => Vector((other, json)))
    }.map(nested => Json.fromFields(nested.flatten))

    private def failType() = ResolutionError("$ref field value must be a string")
    private def failInvalidRef(ref: String) = ResolutionError("Invalid $ref: " + ref) 
    private def failUnknownResource(resource: String) = ResolutionError("Unknown resource: " + resource)
    private def failInvalidPath(path: String) = ResolutionError("Invalid path: " + path)

    private def extract(schema: Json, path: String): Either[ResolutionError, Vector[(String, Json)]] =
      path.split("/").tail.foldLeft(schema.hcursor: ACursor)(_.downField(_))
        .focus.toRight(failInvalidPath(path)).flatMap { resolved =>
          resolved.asObject.toRight(failInvalidPath(path)).map(_.toVector)
        }

    private def resolve(value: Json): Either[ResolutionError, Vector[(String, Json)]] =
      value.asString.toRight(failType()).flatMap { ref =>
        ref.split("#") match {
          case Array("", path) => extract(self, path)
          case Array(subschema, path) =>
            subschemas.get(subschema).toRight(failUnknownResource(subschema)).flatMap { schema =>
              schema.foldWith(Resolver(schema, this.subschemas)).flatMap { processed =>
                extract(processed, path)
              }
            }
          case _ => Left(failInvalidRef(ref))
        }
      }
  }
}