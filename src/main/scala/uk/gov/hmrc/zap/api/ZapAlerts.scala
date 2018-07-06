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

package uk.gov.hmrc.zap.api

import com.typesafe.config.Config
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.zap.client.ZapClient

import scala.collection.mutable.ListBuffer


class ZapAlerts(zapClient: ZapClient) {

  import zapClient._
  import zapClient.zapConfiguration._

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
    var alerts: List[ZapAlert] = List.empty
    if (alertUrlsToReport.isEmpty) {
      alerts = getAlerts()
    }
    else {
      alertUrlsToReport.foreach { alertUrl => alerts = getAlerts(alertUrl) ::: alerts }
    }
    alerts
  }

  private def getAlerts(baseUrl: String = ""): List[ZapAlert] = {
    val response: String = callZapApi("/json/core/view/alerts", "baseurl" -> baseUrl)
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

case class ZapAlert(other: String = "",
                    method: String = "",
                    evidence: String = "",
                    pluginId: String = "",
                    cweid: String,
                    confidence: String = "",
                    wascid: String = "",
                    description: String = "",
                    messageId: String = "",
                    url: String,
                    reference: String = "",
                    solution: String = "",
                    alert: String = "",
                    param: String = "",
                    attack: String = "",
                    name: String = "",
                    risk: String = "",
                    id: String = "") {

  def riskShortName():String = {
    if (risk == "Informational") "Info"
    else risk
  }

  def references(): List[String] = {
    reference.split("\\n").toList
  }

  def severityScore(): String = {
    s"${riskCodes(risk)}-${confidenceCodes(confidence)}"
  }

  val riskCodes = Map("High"->"1",
    "Medium"->"2",
    "Low"->"3",
    "Informational"->"4")

  val confidenceCodes = Map("High"->"1",
    "Medium"->"2",
    "Low"->"3")
}


case class ZapAlertFilter(cweid: String, url: String) {
  def matches(zapAlert: ZapAlert): Boolean = {
    zapAlert.url.matches(url) && zapAlert.cweid.equals(cweid)
  }
}


