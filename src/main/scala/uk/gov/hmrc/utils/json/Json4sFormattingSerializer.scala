package uk.gov.hmrc.utils.json

import org.json4s._
import org.json4s.JsonAST.JString
import org.json4s.MappingException

//TODO: this code should die with scala-dropwizard

class Json4sFormattingSerializer[T](serializer: ObjectSerializer[T])(implicit manifest: Manifest[T]) extends CustomSerializer[T](format => (
  {
    case JString(str) =>
      try {
        serializer.deserialize(str)
      } catch {
        case e: Exception => throw new MappingException(e.getMessage, e)
      }
    case JNull => null.asInstanceOf[T]
  },
  {
    case value: T =>
      try {
        JString(serializer.serialize(value))
      } catch {
        case e: Exception => throw new MappingException(e.getMessage, e)
      }
  }
))