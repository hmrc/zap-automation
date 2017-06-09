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

import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import org.joda.time.{DateTime, LocalDate}
import org.joda.time.format.ISODateTimeFormat
import org.json4s.{CustomSerializer, JDecimal, JNull}
import org.json4s.JsonAST.JInt
import scala.math.BigDecimal

trait Serializers {

}

trait ObjectSerializer[T] {
  def serialize(value: T): String
  def deserialize(stringRepresentation: String): T
}

object Json4sIntToBigDecimalSerializer extends CustomSerializer[BigDecimal](
  format => (
    {
      case JInt(value: BigInt) => BigDecimal(value)
      case JNull => null.asInstanceOf[BigDecimal]
    },
    {
      case value: BigDecimal => JDecimal(value)
    }
  ))

object dateTimeSerializer extends ObjectSerializer[DateTime] {

  private val format = ISODateTimeFormat.dateTime.withZoneUTC

  def deserialize(str: String): DateTime = try {
    format.parseDateTime(str)
  } catch {
    case e: IllegalArgumentException =>
      throw new DeserializationException(s"Unable to parse '$str' to type 'DateTime', expected a valid value with format: yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  }

  def serialize(value: DateTime): String = format.print(value)

}

object localDateSerializer extends ObjectSerializer[LocalDate] {

  private val localDateRegex = """^(\d\d\d\d)-(\d\d)-(\d\d)$""".r

  def deserialize(str: String): LocalDate = str match {
    case localDateRegex(y, m, d) =>
      try {
        new LocalDate(y.toInt, m.toInt, d.toInt)
      } catch {
        case e: IllegalArgumentException => throw new DeserializationException(parseError(str))
      }
    case _ => throw new DeserializationException(parseError(str))
  }

  def serialize(value: LocalDate): String = "%04d-%02d-%02d".format(value.getYear, value.getMonthOfYear, value.getDayOfMonth)

  private def parseError(str: String) = s"Unable to parse '$str' to type 'LocalDate', expected a valid value with format: yyyy-MM-dd"
}

object json4sDateTimeSerializer extends Json4sFormattingSerializer[DateTime](dateTimeSerializer)
object json4sLocalDateSerializer extends Json4sFormattingSerializer[LocalDate](localDateSerializer)
