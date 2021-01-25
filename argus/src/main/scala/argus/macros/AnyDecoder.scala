package io.circe.argus.macros

import scala.reflect.api.Universe

private[macros] object AnyDecoder {
  def anyDecoder[U <: Universe](u: U): u.Tree = {
    import u._
    q"""
      def anyDecoder: Decoder[Any] = Decoder.instance((h: HCursor) => h.focus.get match {
        case n if n.isNull =>    null
        case n if n.isNumber =>  n.as[Double]
        case b if b.isBoolean => b.as[Boolean]
        case s if s.isString =>  s.as[String]
        case o if o.isObject =>
          o.as[Map[String, Any]](Decoder.decodeMapLike[String, Any, Map](KeyDecoder.decodeKeyString, anyDecoder, implicitly))
        case a if a.isArray =>   a.as[List[Any]](Decoder.decodeList[Any](anyDecoder))
      })
    """
  }
}
