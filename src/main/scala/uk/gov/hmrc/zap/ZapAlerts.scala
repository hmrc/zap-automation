/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.zap

import com.typesafe.config.Config
import play.api.libs.json.{Json, Reads}

import scala.collection.mutable.ListBuffer


class ZapAlerts(owaspZap: OwaspZap) {

  import owaspZap._
  import owaspZap.zapConfiguration._

  implicit val zapAlertReads: Reads[ZapAlert] = Json.reads[ZapAlert]

  def filterAlerts(allAlerts: List[ZapAlert]): List[ZapAlert] = {
    val relevantAlerts = allAlerts.filterNot { zapAlert =>
      alertsToIgnore().exists(f => f.matches(zapAlert))
    }

    if (ignoreOptimizelyAlerts)
      relevantAlerts.filterNot(zapAlert => zapAlert.evidence.contains("optimizely"))
    else
      relevantAlerts
  }


  def parsedAlerts: List[ZapAlert] = {
    val response: String = callZapApi("/json/core/view/alerts", "baseurl" -> alertsBaseUrl)
    val jsonResponse = Json.parse(response)
    (jsonResponse \ "alerts").as[List[ZapAlert]]
  }

  def alertsToIgnore(): List[ZapAlertFilter] = {
    val listOfAlerts: List[Config] = zapConfiguration.alertsToIgnore
    val listBuffer: ListBuffer[ZapAlertFilter] = new ListBuffer[ZapAlertFilter]

    listOfAlerts.foreach { af: Config =>
      listBuffer.append(ZapAlertFilter(af.getString("cweid"), af.getString("url")))
    }
    listBuffer.toList
  }
}
