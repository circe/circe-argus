package io.circe.argus

/**
 * Supports abstraction over Argus-generated types that have a schema string associated with them.
 */
trait HasSchemaSource[A] {
  def value: String
}

object HasSchemaSource {
  def apply[A](implicit instance: HasSchemaSource[A]): HasSchemaSource[A] = instance

  def instance[A](source: String): HasSchemaSource[A] = new HasSchemaSource[A] {
    def value: String = source
  }
}
