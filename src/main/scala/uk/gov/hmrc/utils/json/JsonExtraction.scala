package uk.gov.hmrc.utils.json

import java.net.{URI, URL}

import org.json4s.JsonAST.JString
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{CustomSerializer, DefaultFormats, Formats, JNull}

object JsonExtraction {

  val formats = DefaultFormats + json4sDateTimeSerializer + json4sLocalDateSerializer + UriSerializer + UrlSerializer + Json4sIntToBigDecimalSerializer

  def apply[A](body: String)(implicit m: Manifest[A], formats: Formats = formats): A = extractResponse[A](body)

  private def extractResponse[A](body: String)(implicit m: Manifest[A], format: Formats = formats): A = Option(body) match {
    case Some(b) if b.length > 0 =>
      try {
        parse(b, useBigDecimalForDouble = true).extract
      } catch {
        case t: Throwable =>
          throw t
      }
    case _ => throw new IllegalArgumentException("A string value is required for transformation")
  }

  case object UriSerializer extends CustomSerializer[URI](format => ( {
    case JString(uri) => URI.create(uri)
    case JNull => null
  }, {
    case uri: URI => JString(uri.toString)
  }
  ))

  case object UrlSerializer extends CustomSerializer[URL](format => ( {
    case JString(url) => new URL(url)
    case JNull => null
  }, {
    case url: URL => JString(url.toString)
  }
  ))

}