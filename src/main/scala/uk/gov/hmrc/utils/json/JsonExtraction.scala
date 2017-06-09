/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
