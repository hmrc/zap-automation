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

package uk.gov.hmrc

import java.util.UUID
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonParser
import org.scalatest.WordSpec
import java.io._
import scala.Console
import uk.gov.hmrc.utils.{InsecureClient, TestHelper}

trait ZapTest extends WordSpec {

  //Todo: Change the json library: do this after we create a library

  val alertsToIgnore: List[ZapAlertFilter] = List.empty
  val zapBaseUrl: String
  val testUrl: String
  lazy val theClient = new InsecureClient
  val contextBaseUrl: String = ".*"
  val alertsBaseUrl: String = ""
  var policyName: String = ""
  var context: Context = null
  val desiredTechnologyNames: String = "OS,OS.Linux,Language,Language.Xml,SCM,SCM.Git"

  def appendSlashToBaseUrlIfNeeded(): String = {
    if (!zapBaseUrl.endsWith("/")) zapBaseUrl + "/" else zapBaseUrl
  }

  def callZapApiTo(url: String): (Int, String) = {
    val baseUrl: String = appendSlashToBaseUrlIfNeeded()
    val theResponse = theClient.getRawResponse(baseUrl + url)
    val statusCode = theResponse._1
    if (statusCode != 200) fail(s"The ZAP API returned a $statusCode status when you called it using: $url")
    theResponse
  }

  def hasCallCompleted(url: String): Boolean = {
    val theJson = JsonParser.parse(callZapApiTo(url)._2).asInstanceOf[JObject]
    val theStatus = theJson.values("status").toString
    if (theStatus == "100") true else false
  }

  def createPolicy(): String = {
    val policyName = UUID.randomUUID.toString
    val createPolicy = s"json/ascan/action/addScanPolicy/?scanPolicyName=$policyName"
    callZapApiTo(createPolicy)
    policyName
  }

  def setUpPolicy(policyName: String): Unit = {
    val allScannerIds = "0,1,2,3,4,5,6,7,41,42,43,10010,10011,10012,10015,10016,10017,10018,10019,10020,10021,10023,10024,10025,10026,10027,10028,10029,10030,10031,10032,10033,10034,10035,10036,10037,10038,10039,10040,10041,10042,10043,10044,10045,10046,10047,10048,10049,10050,10051,10052,10053,10054,10055,10056,10094,10095,10096,10097,10098,10099,10101,10102,10103,10104,10105,10106,10107,10200,10201,10202,20000,20001,20002,20003,20004,20005,20006,20010,20012,20014,20015,20016,20017,20018,20019,30001,30002,30003,40000,40001,40002,40003,40004,40005,40006,40007,40008,40009,40010,40011,40012,40013,40014,40015,40016,40017,40018,40019,40020,40021,40022,40023,40024,40025,40026,40027,40028,40029,90001,90011,90018,90019,90020,90021,90022,90023,90024,90025,90026,90027,90028,90029,90030,90033,100000"
    val disableScanners = s"json/ascan/action/disableScanners/?ids=$allScannerIds&scanPolicyName=$policyName"
    val enableScanners = s"json/ascan/action/enableScanners/?ids=0,1,2&scanPolicyName=$policyName"
    //callZapApiTo(disableScanners)
    //callZapApiTo(enableScanners)
  }

  def createContext(): Context = {
    val contextName = UUID.randomUUID.toString
    val createContext = s"json/context/action/newContext/?contextName=$contextName"
    val createContextResponse = JsonParser.parse(callZapApiTo(createContext)._2).asInstanceOf[JObject]
    val contextId = createContextResponse.values("contextId").toString
    new Context(contextName, contextId)
  }

  def setUpContext(contextName: String): Unit = {
    val limitContextToABaseUrl = s"json/context/action/includeInContext/?contextName=$contextName&regex=$contextBaseUrl"
    callZapApiTo(limitContextToABaseUrl)
    val excludeAllTechnologiesFromContext = s"json/context/action/excludeAllContextTechnologies/?contextName=$contextName"
    callZapApiTo(excludeAllTechnologiesFromContext)
    val includeDesiredTechnologiesInContext = s"json/context/action/includeContextTechnologies/?contextName=$contextName&technologyNames=$desiredTechnologyNames"
    callZapApiTo(includeDesiredTechnologiesInContext)
  }

  def tearDown(contextName: String, policyName: String): Unit = {
    val removeContext = s"json/context/action/removeContext/?contextName=$contextName"
    callZapApiTo(removeContext)

    val removePolicy = s"json/ascan/action/removeScanPolicy/?scanPolicyName=$policyName"
    callZapApiTo(removePolicy)

    val removeAlerts = s"json/core/action/deleteAllAlerts"
    callZapApiTo(removeAlerts)
  }

  def runAndCheckStatusOfSpider(contextName: String): Unit = {
    val runSpiderScan = s"json/spider/action/scan/?url=$testUrl&contextName=$contextName"
    callZapApiTo(runSpiderScan)

    TestHelper.waitForCondition(hasCallCompleted("json/spider/view/status"), "Spider Timed Out", timeoutInSeconds = 600)
  }

  def runAndCheckStatusOfActiveScan(contextId: String, policyName: String): Unit = {
    val runActiveScan = s"json/ascan/action/scan/?url=$testUrl&contextId=$contextId&scanPolicyName=$policyName"
    callZapApiTo(runActiveScan)

    TestHelper.waitForCondition(hasCallCompleted("json/ascan/view/status"), "Active Scanner Timed Out", timeoutInSeconds = 1800)
  }

  def writeHtmlReportToFile(text: String): File = {
    val file = new File("ZapReport.html")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(text)
    bw.close()
    file
  }

  def reportAlerts(relevantAlerts: List[ZapAlert]): Unit = {
    relevantAlerts.foreach { alert: ZapAlert =>
      println("***********************************")
      println(s"URL:         ${alert.url}")
      println(s"CWE ID:      ${alert.cweid}")
      println(s"Alert Name:  ${alert.alert}")
      println(s"Description: ${alert.description}")
      println(s"Risk Level:  ${alert.risk}")
      println(s"Evidence:    ${alert.evidence.take(100).trim}...")
    }

    val getHtmlReport = "OTHER/core/other/htmlreport"
    val text = callZapApiTo(getHtmlReport)._2
    val filePath = writeHtmlReportToFile(text).getCanonicalPath
    println(Console.BOLD + s"For more information on alerts, please see the html report at $filePath")

  }

  def filterAlerts(): List[ZapAlert] = {
    val baseUrl = appendSlashToBaseUrlIfNeeded()
    val allAlerts: List[ZapAlert] = theClient.get[ZapAlerts](baseUrl + s"json/core/view/alerts/?baseurl=$alertsBaseUrl").alerts
    val relevantAlerts = allAlerts.filterNot(zapAlert => alertsToIgnore.contains(zapAlert.getFilter))
    relevantAlerts
  }

  "Setting up the policy and context" should {
    "complete successfully" in {
      policyName = createPolicy()
      setUpPolicy(policyName)
      context = createContext()
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
      val relevantAlerts = filterAlerts()
      if (relevantAlerts.nonEmpty) {
        reportAlerts(relevantAlerts)
        fail(s"Zap found some new alerts - see above!")
      }
    }
  }

  "Tearing down the policy, context and alerts" should {
    "complete successfully" in {
      //tearDown(context.name, policyName)
    }
  }
}

case class ZapAlerts(alerts: List[ZapAlert])
case class ZapAlert(other: String = "", evidence: String = "", pluginId: String = "", cweid: String, confidence: String = "", wascid: String = "", description: String = "", messageId: String = "", url: String, reference: String = "", solution: String = "", alert: String = "", param: String = "", attack: String = "", name: String = "", risk: String = "", id: String = "") {
  def getFilter = ZapAlertFilter(cweid, url)
}
case class ZapAlertFilter(cweid: String, url: String)
case class Context(name: String, id: String)
