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

import java.util.UUID

import com.typesafe.config.Config
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.utils.ZapConfiguration._
import uk.gov.hmrc.utils._

import scala.collection.mutable.ListBuffer

object ZapApi {

  implicit val zapAlertReads: Reads[ZapAlert] = Json.reads[ZapAlert]

  var httpClient: HttpClient = WsClient
  var spiderScanCompleted : Boolean = false
  var activeScanCompleted : Boolean = false

  def alertsToIgnore():List[ZapAlertFilter] = {
    val listOfAlerts: List[Config] = ZapConfiguration.alertsToIgnore
    val listBuffer: ListBuffer[ZapAlertFilter] = new ListBuffer[ZapAlertFilter]

    listOfAlerts.foreach { af: Config =>
      listBuffer.append(ZapAlertFilter(af.getString("cweid"), af.getString("url")))
    }
    listBuffer.toList
  }

  def callZapApi(queryPath: String, params: (String, String)*): String = {
    val (status, response) = httpClient.get(zapBaseUrl, queryPath, params: _*)

    if (status != 200) {
      throw ZapException(s"Expected response code is 200, received:$status")
    }
    response
  }

  def hasCallCompleted(url: String): Boolean = {
    val jsonResponse = Json.parse(callZapApi(url))
    val status = (jsonResponse \ "status").as[String]
    if (status == "100") true else false
  }

  def createPolicy(): String = {
    val policyName = UUID.randomUUID.toString
    callZapApi("/json/ascan/action/addScanPolicy", "scanPolicyName" -> policyName)
    logger.info(s"Creating policy: $policyName")
    policyName
  }

  def setUpPolicy(policyName: String): Unit = {
    val allScannerIds = "0,1,2,3,4,5,6,7,41,42,43,10010,10011,10012,10015,10016,10017,10018,10019,10020,10021,10023,10024,10025,10026,10027,10028,10029,10030,10031,10032,10033,10034,10035,10036,10037,10038,10039,10040,10041,10042,10043,10044,10045,10046,10047,10048,10049,10050,10051,10052,10053,10054,10055,10056,10094,10095,10096,10097,10098,10099,10101,10102,10103,10104,10105,10106,10107,10200,10201,10202,20000,20001,20002,20003,20004,20005,20006,20010,20012,20014,20015,20016,20017,20018,20019,30001,30002,30003,40000,40001,40002,40003,40004,40005,40006,40007,40008,40009,40010,40011,40012,40013,40014,40015,40016,40017,40018,40019,40020,40021,40022,40023,40024,40025,40026,40027,40028,40029,90001,90011,90018,90019,90020,90021,90022,90023,90024,90025,90026,90027,90028,90029,90030,90033,100000"
    val scannersToDisableForUiTesting = "4,5,42,10000,10001,10013,10014,10022,20000,20001,20002,20003,20004,20005,20006,30001,30002,30003,40000,40001,40002,40006,40010,40011,40018,40020,40022,40027,40028,40029,90001,90029,90030"
    val scannersToEnableForApiTesting = "0,2,3,6,7,42,10010,10011,10012,10015,10016,10017,10019,10020,10021,10023,10024,10025,10026,10027,10032,10040,10045,10048,10095,10105,10202,20012,20014,20015,20016,20017,20018,20019,30001,30002,30003,40003,40008,40009,40012,40013,40014,40016,40017,40018,40019,40020,40021,40022,40023,50000,50001,90001,90011,90019,90020,90021,90022,90023,90024,90025,90026,90028,90029,90030,90033"

    if (!testingAnApi) {
      callZapApi("/json/ascan/action/disableScanners", "ids" -> scannersToDisableForUiTesting, "scanPolicyName" -> policyName)
    }
    else {
      callZapApi("/json/ascan/action/disableAllScanners", "scanPolicyName" -> policyName)
      callZapApi("/json/ascan/action/enableScanners", "ids" -> scannersToEnableForApiTesting, "scanPolicyName" -> policyName)
    }

  }

  def createContext(): Context = {
    val contextName = UUID.randomUUID.toString
    val response: String = callZapApi("/json/context/action/newContext", "contextName" -> contextName)
    val jsonResponse = Json.parse(response)
    val contextId = (jsonResponse \ "contextId").as[String]
    logger.info(s"Context Name: $contextName")
    logger.info(s"Context Id: $contextId")

    Context(contextName, contextId)
  }

  def setUpContext(contextName: String): Unit = {
    callZapApi("/json/context/action/includeInContext", "contextName" -> contextName, "regex" -> contextBaseUrl)
    callZapApi("/json/context/action/excludeAllContextTechnologies", "contextName" -> contextName)

    if (desiredTechnologyNames.nonEmpty) {
      callZapApi("/json/context/action/includeContextTechnologies", "contextName" -> contextName, "technologyNames" -> desiredTechnologyNames)
    }

    if (routeToBeIgnoredFromContext.nonEmpty) {
      callZapApi("/json/context/action/excludeFromContext", "contextName" -> contextName, "regex" -> routeToBeIgnoredFromContext)
    }
  }

  def tearDown(contextName: String, policyName: String): Unit = {
    callZapApi("/json/context/action/removeContext", "contextName" -> contextName)
    callZapApi("/json/ascan/action/removeScanPolicy", "scanPolicyName" -> policyName)
    callZapApi("/json/core/action/deleteAllAlerts")
  }


  def runAndCheckStatusOfSpider(contextName: String): Unit = {
    callZapApi("/json/spider/action/scan", "contextName" -> contextName, "url" -> testUrl)
    TestHelper.waitForCondition(hasCallCompleted("/json/spider/view/status"), "Spider Timed Out", timeoutInSeconds = 600)
    spiderScanCompleted = true
  }

  def runAndCheckStatusOfActiveScan(contextId: String, policyName: String): Unit = {
    if (activeScan) {
      logger.info(s"Triggering Active Scan.")
      callZapApi("/json/ascan/action/scan", "contextId" -> contextId, "scanPolicyName" -> policyName, "url" -> testUrl)
      TestHelper.waitForCondition(hasCallCompleted("/json/ascan/view/status"), "Active Scanner Timed Out", timeoutInSeconds = 1800)
      activeScanCompleted = true
    }
    else
      logger.info(s"Skipping Active Scan")
  }

  def filterAlerts(allAlerts: List[ZapAlert]): List[ZapAlert] = {
    val relevantAlerts = allAlerts.filterNot{zapAlert =>
      alertsToIgnore().exists(f => f.matches(zapAlert))
    }

    if(ignoreOptimizelyAlerts)
      relevantAlerts.filterNot(zapAlert => zapAlert.evidence.contains("optimizely"))
    else
      relevantAlerts
  }

  def testSucceeded(relevantAlerts: List[ZapAlert]): Boolean = {

    val failingAlerts = failureThreshold match {
      case "High" => relevantAlerts.filterNot(zapAlert => zapAlert.risk == "Informational" || zapAlert.risk == "Low" || zapAlert.risk == "Medium")
      case "Medium" => relevantAlerts.filterNot(zapAlert => zapAlert.risk == "Informational" || zapAlert.risk == "Low")
      case "Low" => relevantAlerts.filterNot(zapAlert => zapAlert.risk == "Informational")
      case _ => relevantAlerts.filterNot(zapAlert => zapAlert.risk == "Informational")
    }

    failingAlerts.isEmpty
  }

  def parsedAlerts: List[ZapAlert] = {
    val response: String = callZapApi("/json/core/view/alerts", "baseurl" -> alertsBaseUrl)
    val jsonResponse = Json.parse(response)
    (jsonResponse \ "alerts").as[List[ZapAlert]]
  }

  def healthCheckTestUrl(): Unit = {

    if (debugHealthCheck) {
      logger.info(s"Performing health check for the test URL with: $healthCheckUrl")
      val successStatusRegex = "(2..|3..)"
      val (status, response) = try {
        httpClient.getRequest(healthCheckUrl)
      }
      catch {
        case e: Throwable => throw ZapException(s"Health check failed for test URL: $healthCheckUrl with exception:${e.getMessage}")
      }

      if (!status.toString.matches(successStatusRegex))
        throw ZapException(s"Health Check failed for test URL: $healthCheckUrl with status:$status")
    }
    else {
      logger.info("Health Checking Test Url is disabled. This may result in incorrect test result.")
    }
  }

  lazy val healthCheckUrl: String = {
    val localHostRegex = "http:\\/\\/localhost:\\d+".r
    val host = localHostRegex.findFirstIn(testUrl).get
    s"$host/ping/ping"
  }

}

case class Context(name: String, id: String)

case class ZapException(s: String) extends Exception(s)
