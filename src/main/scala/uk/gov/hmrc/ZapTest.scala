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

package uk.gov.hmrc

import java.io.{BufferedWriter, File, FileWriter}
import java.util.UUID

import com.typesafe.config.Config
import org.scalatest.WordSpec
import org.slf4j.Logger
import play.api.libs.json._
import uk.gov.hmrc.utils.{LoadConfig, TestHelper, WsClient, ZapLogger}

import scala.util.Try

trait ZapTest extends WordSpec {

  val logger: Logger = ZapLogger.logger
  val zapConfig: Config = LoadConfig.extractedConfig

  /**
    * If, when you run the Zap tests, you find alerts that you have investigated and don't see as a problem
    * you can filter them out by adding to this list, using the cweid and the url that the alert was found on.
    * The CWE ID is a Common Weakness Enumeration (http://cwe.mitre.org/data/index.html), you can
    * find this by looking at the alert output from your tests.
    */
  val alertsToIgnore: List[ZapAlertFilter] = List.empty

  /**
    * Required field. It will rarely need to be changed. We've included it as an overridable
    * field for flexibility and just in case.
    */
  val zapBaseUrl: String

  /**
    * Required field. It needs to be the URL of the start page of your application (not
    * just localhost:port).
    */
  val testUrl: String
  lazy val theClient = new WsClient()

  /**
    * Not a required field. This url is added as the base url to your context.
    * A context is a construct in Zap that limits the scope of any attacks run to a
    * particular domain (this doesn't mean that Zap won't find alerts on other services during the
    * browser test run).
    * This would usually be the base url of your service - eg http://localhost:xxxx.*
    */
  val contextBaseUrl: String = ".*"

  /**
    * Not a required field. This value, if set to true, will ignore all alerts from optimizely.
    */
  val ignoreOptimizelyAlerts: Boolean = false

  /**
    * Not a required field. This is the url that the zap-automation library
    * will use to filter out the alerts that are shown to you. Note that while Zap is doing
    * testing, it is likely to find alerts from other services that you don't own - for example
    * from logging in, therefore we recommend that you set this to be the base url for the
    * service you are interested in.
    */
  val alertsBaseUrl: String = ""
  var policyName: String = ""
  var context: Context = _

  /**
    * Not a required field. We recommend you don't change this field, as we've made basic choices
    * for the platform. We made it overridable just in case your service differs from the
    * standards of the Platform.
    *
    * The technologies that you put here will limit the amount of checks that ZAP will do to
    * just the technologies that are relevant. The default technologies are set to
    * "OS,OS.Linux,Language,Language.Xml,SCM,SCM.Git".
    */
  val desiredTechnologyNames: String = "OS,OS.Linux,Language,Language.Xml,SCM,SCM.Git"

  /**
    * Not a required field. You may set this if you have any routes that are part of your
    * application, but you do not want tested. For example, if you had any test-only routes, you
    * could force Zap not to test them by adding them in here as a regex.
    */
  val routeToBeIgnoredFromContext: String = ""

  /**
    * Not a required field. You should set this to be true if you are testing an API.
    * By default this assumes you are testing a UI and therefore is defaulted to be false.
    */
  val testingAnApi: Boolean = false

  implicit val zapAlertReads: Reads[ZapAlert] = Json.reads[ZapAlert]

  def callZapApi(queryPath: String, params: (String, String)*): String = {
    val (status, response) = theClient.get(zapBaseUrl, queryPath, params: _*)

    if (status != 200) {
      fail(s"Expected response code is 200, received:$status")
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
  }

  def runAndCheckStatusOfActiveScan(contextId: String, policyName: String): Unit = {
    val isActiveScanRequired = zapConfig.getBoolean("activeScan")

    if (isActiveScanRequired) {

      logger.info(s"Active Scan Config: is set to: $isActiveScanRequired. Triggering Active Scan.")

      callZapApi("/json/ascan/action/scan", "contextId" -> contextId, "scanPolicyName" -> policyName, "url" -> testUrl)
      TestHelper.waitForCondition(hasCallCompleted("/json/ascan/view/status"), "Active Scanner Timed Out", timeoutInSeconds = 1800)
    }
    else
      logger.info(s"Active Scan Config: is set to: $isActiveScanRequired. Active Scan is NOT triggered.")
  }

  def reportAlerts(relevantAlerts: List[ZapAlert]): Unit = {
    val file = new File("ZapReport.html")
    val writer = new BufferedWriter(new FileWriter(file))

    writer.write(report.html.index(relevantAlerts).toString())
    writer.close()
    logger.info(s"HTML Report generated: file://${file.getAbsolutePath}")
  }

  def filterAlerts(allAlerts: List[ZapAlert]): List[ZapAlert] = {

    val relevantAlerts = allAlerts.filterNot{zapAlert =>
      alertsToIgnore.exists(f => f.matches(zapAlert))
    }

    if(ignoreOptimizelyAlerts)
      relevantAlerts.filterNot(zapAlert => zapAlert.evidence.contains("optimizely"))
    else
      relevantAlerts
  }

  def testSucceeded(relevantAlerts: List[ZapAlert]): Boolean = {

    val failingAlerts = zapConfig.getString("failureThreshold") match {
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

  "Setting up the policy and context" should {
    "complete successfully" in {
      policyName = createPolicy()
      logger.info(s"Creating policy: $policyName")
      setUpPolicy(policyName)
      context = createContext()
      logger.info(s"Creating context: $context")
      setUpContext(context.name)
    }
  }

  "Kicking off the scans" should {
    "complete successfully" in {
      runAndCheckStatusOfSpider(context.name)
      runAndCheckStatusOfActiveScan(context.id, policyName)
    }
  }

  "Inspecting the alerts" should {
    "not find any unknown alerts" in {
      val relevantAlerts = filterAlerts(parsedAlerts)
      reportAlerts(relevantAlerts)
      withClue ("Zap found some new alerts - see above!") {
        assert(testSucceeded(relevantAlerts))
      }
    }
  }

  "Tearing down the policy, context and alerts" should {
    "complete successfully" in {
      if(!Try(System.getProperty("zap.skipTearDown").toBoolean).getOrElse(false)){
        logger.debug(s"Removing ZAP Context (${context.name}) Policy ($policyName), and all alerts.")
        tearDown(context.name, policyName)
      } else {
        logger.debug("Skipping Tear Down")
      }
    }
  }
}

case class Context(name: String, id: String)
