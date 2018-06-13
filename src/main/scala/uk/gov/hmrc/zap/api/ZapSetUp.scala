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

import java.util.UUID

import play.api.libs.json.Json
import uk.gov.hmrc.zap.client.ZapClient
import uk.gov.hmrc.zap.logger.ZapLogger.log

class ZapSetUp(zapClient: ZapClient) {

  import zapClient._
  import zapClient.zapConfiguration._

  def initialize(): ZapContext = {

    val contextName = UUID.randomUUID.toString
    val policyName = UUID.randomUUID.toString

    callZapApi("/json/ascan/action/addScanPolicy", "scanPolicyName" -> policyName)

    val response: String = callZapApi("/json/context/action/newContext", "contextName" -> contextName)
    val contextId = (Json.parse(response) \ "contextId").as[String]

    log.info(s"Initialized context. Id: $contextId, name: $contextName, policy: $policyName")
    ZapContext(contextId, contextName, policyName)
  }

  def setUpPolicy(implicit zapContext: ZapContext): Unit = {
    val allScannerIds = "0,1,2,3,4,5,6,7,41,42,43,10010,10011,10012,10015,10016,10017,10018,10019,10020,10021,10023,10024,10025,10026,10027,10028,10029,10030,10031,10032,10033,10034,10035,10036,10037,10038,10039,10040,10041,10042,10043,10044,10045,10046,10047,10048,10049,10050,10051,10052,10053,10054,10055,10056,10094,10095,10096,10097,10098,10099,10101,10102,10103,10104,10105,10106,10107,10200,10201,10202,20000,20001,20002,20003,20004,20005,20006,20010,20012,20014,20015,20016,20017,20018,20019,30001,30002,30003,40000,40001,40002,40003,40004,40005,40006,40007,40008,40009,40010,40011,40012,40013,40014,40015,40016,40017,40018,40019,40020,40021,40022,40023,40024,40025,40026,40027,40028,40029,90001,90011,90018,90019,90020,90021,90022,90023,90024,90025,90026,90027,90028,90029,90030,90033,100000"
    val scannersToDisableForUiTesting = "4,5,42,10000,10001,10013,10014,10022,20000,20001,20002,20003,20004,20005,20006,30001,30002,30003,40000,40001,40002,40006,40010,40011,40018,40020,40022,40027,40028,40029,90001,90029,90030"
    val scannersToEnableForApiTesting = "0,2,3,6,7,42,10010,10011,10012,10015,10016,10017,10019,10020,10021,10023,10024,10025,10026,10027,10032,10040,10045,10048,10095,10105,10202,20012,20014,20015,20016,20017,20018,20019,30001,30002,30003,40003,40008,40009,40012,40013,40014,40016,40017,40018,40019,40020,40021,40022,40023,50000,50001,90001,90011,90019,90020,90021,90022,90023,90024,90025,90026,90028,90029,90030,90033"

    if (!testingAnApi) {
      callZapApi("/json/ascan/action/disableScanners", "ids" -> scannersToDisableForUiTesting, "scanPolicyName" -> zapContext.policy)
    }
    else {
      callZapApi("/json/ascan/action/disableAllScanners", "scanPolicyName" -> zapContext.policy)
      callZapApi("/json/ascan/action/enableScanners", "ids" -> scannersToEnableForApiTesting, "scanPolicyName" -> zapContext.policy)
    }
  }

  def setUpContext(implicit zapContext: ZapContext): Unit = {
    callZapApi("/json/context/action/includeInContext", "contextName" -> zapContext.name, "regex" -> contextBaseUrl)
    callZapApi("/json/context/action/excludeAllContextTechnologies", "contextName" -> zapContext.name)

    if (desiredTechnologyNames.nonEmpty) {
      callZapApi("/json/context/action/includeContextTechnologies", "contextName" -> zapContext.name, "technologyNames" -> desiredTechnologyNames)
    }

    if (routeToBeIgnoredFromContext.nonEmpty) {
      callZapApi("/json/context/action/excludeFromContext", "contextName" -> zapContext.name, "regex" -> routeToBeIgnoredFromContext)
    }
  }
}


case class ZapContext(id: String, name: String, policy: String)