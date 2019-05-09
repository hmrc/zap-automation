/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.zap.client.ZapClient
import uk.gov.hmrc.zap.logger.ZapLogger.log

class ZapSetUp(zapClient: ZapClient) {

  import zapClient._
  import zapClient.zapConfiguration._

  lazy val checkMissingScanners: List[Scanner] = checkScannerSetup()

  def initialize(): ZapContext = {

    val contextName = UUID.randomUUID.toString
    val policyName = UUID.randomUUID.toString

    callZapApi("/json/ascan/action/addScanPolicy", "scanPolicyName" -> policyName)

    val response: String = callZapApi("/json/context/action/newContext", "contextName" -> contextName)
    val contextId = (Json.parse(response) \ "contextId").as[String]

    log.info(s"Initialized context. Id: $contextId, name: $contextName, policy: $policyName")
    ZapContext(contextId, contextName, policyName)
  }

  def checkScannerSetup(): List[Scanner] = {
    val pscanResponse = callZapApi("/json/pscan/view/scanners")
    val ascanResponse = callZapApi("/json/ascan/view/scanners")

    val missingScanners: List[Scanner] =
      passiveScanners.filterNot(requiredScannerId => enabledScannerIds(pscanResponse).contains(requiredScannerId.id)) ++
        activeScanners.filterNot(requiredScannerId => enabledScannerIds(ascanResponse).contains(requiredScannerId.id))

    if (missingScanners.nonEmpty) {
      log.warn("\033[31mThe below required scanners are not enabled. This will affect ZAP alerts\033[0m")
      missingScanners.foreach {
        scanner =>
          log.warn(
            s"""\033[31m
             Scanner ID   : ${scanner.id}
             Name         : ${scanner.name}
             Scanner Type : ${scanner.scannerType} \033[0m""")
      }
    }
    missingScanners
  }

  private def enabledScannerIds(zapScannerResponse: String): List[String] = {
    Json.parse(zapScannerResponse)("scanners").as[List[JsValue]]
      .filter(scanner => scanner("enabled").as[String] == "true")
      .map(scanner => scanner("id").as[String])
  }


  def setUpPolicy(implicit zapContext: ZapContext): Unit = {
    val scannersToDisableForUiTesting = "42,30001,30002,30003,40018,40020,40022,90001"
    val scannersToEnableForApiTesting = "0,2,3,6,7,42,10010,10011,10012,10015,10016,10017,10019,10020,10021,10023,10024,10025,10026,10027,10032,10040,10045,10048,10095,10105,10202,20012,20014,20015,20016,20017,20018,20019,30001,30002,30003,40003,40008,40009,40012,40013,40014,40016,40017,40018,40019,40020,40021,40022,40023,50000,50001,90001,90011,90019,90020,90021,90022,90023,90024,90025,90028"

    if (!testingAnApi) {
      callZapApi("/json/ascan/action/disableScanners", "ids" -> scannersToDisableForUiTesting, "scanPolicyName" -> zapContext.policy)
    }
    else {
      callZapApi("/json/ascan/action/disableAllScanners", "scanPolicyName" -> zapContext.policy)
      callZapApi("/json/ascan/action/enableScanners", "ids" -> scannersToEnableForApiTesting, "scanPolicyName" -> zapContext.policy)
    }
  }

  def setUpContext(implicit zapContext: ZapContext): Unit = {
    callZapApi("/json/context/action/includeInContext", "contextName" -> zapContext.name, "regex" -> contextBaseUrlRegex)
    callZapApi("/json/context/action/excludeAllContextTechnologies", "contextName" -> zapContext.name)

    if (desiredTechnologyNames.nonEmpty) {
      callZapApi("/json/context/action/includeContextTechnologies", "contextName" -> zapContext.name, "technologyNames" -> desiredTechnologyNames)
    }

    if (routeToBeIgnoredFromContext.nonEmpty) {
      callZapApi("/json/context/action/excludeFromContext", "contextName" -> zapContext.name, "regex" -> routeToBeIgnoredFromContext)
    }
  }

  def setConnectionTimeout(): Unit = {
    callZapApi("/json/core/action/setOptionTimeoutInSecs", "Integer" -> s"$connectionTimeout")
    log.info(s"Zap Connection Timeout set to $connectionTimeout seconds")
  }
}

case class ZapContext(id: String, name: String, policy: String)

case class Scanner(id: String, name: String, scannerType: String)
