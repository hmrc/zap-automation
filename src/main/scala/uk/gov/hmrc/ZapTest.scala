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
import play.api.libs.json._
import org.scalatest.WordSpec
import java.io._
import scala.Console
import uk.gov.hmrc.utils.{InsecureClient, TestHelper}

trait ZapTest extends WordSpec {

  val alertsToIgnore: List[ZapAlertFilter] = List.empty
  val zapBaseUrl: String
  val testUrl: String
  lazy val theClient = new InsecureClient
  val contextBaseUrl: String = ".*"
  val alertsBaseUrl: String = ""
  var policyName: String = ""
  var context: Context = _
  val desiredTechnologyNames: String = "OS,OS.Linux,Language,Language.Xml,SCM,SCM.Git"
  val routeToBeIgnoredFromContext: String = ""
  implicit val zapAlertReads = Json.reads[ZapAlert]
  implicit val zapAlertsReads = Json.reads[ZapAlerts]

  def appendSlashToBaseUrlIfNeeded(): String = {
    if (!zapBaseUrl.endsWith("/")) zapBaseUrl + "/" else zapBaseUrl
  }

  def callZapApiTo(url: String): (Int, String) = {
    val completeUrl: String = appendSlashToBaseUrlIfNeeded() + url
    val theResponse = theClient.getRawResponse(completeUrl)
    val statusCode = theResponse._1
    if (statusCode != 200) fail(s"The ZAP API returned a $statusCode status when you called it using: $completeUrl")
    theResponse
  }

  def hasCallCompleted(url: String): Boolean = {
    val jsonResponse = Json.parse(callZapApiTo(url)._2)
    val status = (jsonResponse \ "status").as[String]
    if (status == "100") true else false
  }

  def createPolicy(): String = {
    val policyName = UUID.randomUUID.toString
    val createPolicy = s"json/ascan/action/addScanPolicy/?scanPolicyName=$policyName"
    callZapApiTo(createPolicy)
    policyName
  }

  def setUpPolicy(policyName: String): Unit = {
    val allScannerIds = "0,1,2,3,4,5,6,7,41,42,43,10010,10011,10012,10015,10016,10017,10018,10019,10020,10021,10023,10024,10025,10026,10027,10028,10029,10030,10031,10032,10033,10034,10035,10036,10037,10038,10039,10040,10041,10042,10043,10044,10045,10046,10047,10048,10049,10050,10051,10052,10053,10054,10055,10056,10094,10095,10096,10097,10098,10099,10101,10102,10103,10104,10105,10106,10107,10200,10201,10202,20000,20001,20002,20003,20004,20005,20006,20010,20012,20014,20015,20016,20017,20018,20019,30001,30002,30003,40000,40001,40002,40003,40004,40005,40006,40007,40008,40009,40010,40011,40012,40013,40014,40015,40016,40017,40018,40019,40020,40021,40022,40023,40024,40025,40026,40027,40028,40029,90001,90011,90018,90019,90020,90021,90022,90023,90024,90025,90026,90027,90028,90029,90030,90033,100000"
    val scannersToDisable = "4,5,42,10000,10001,10013,10014,10022,20000,20001,20002,20003,20004,20005,20006,30001,30002,30003,40000,40001,40002,40006,40010,40011,40018,40020,40022,40027,40028,40029,90001,90029,90030"

    val disableScanners = s"json/ascan/action/disableScanners/?ids=$scannersToDisable&scanPolicyName=$policyName"
    callZapApiTo(disableScanners)
  }

  def createContext(): Context = {
    val contextName = UUID.randomUUID.toString
    val createContext = s"json/context/action/newContext/?contextName=$contextName"
    val jsonResponse = Json.parse(callZapApiTo(createContext)._2)
    val contextId = (jsonResponse \ "contextId").as[String]

    Context(contextName, contextId)
  }

  def setUpContext(contextName: String): Unit = {
    val limitContextToABaseUrl = s"json/context/action/includeInContext/?contextName=$contextName&regex=$contextBaseUrl"
    callZapApiTo(limitContextToABaseUrl)

    val excludeAllTechnologiesFromContext = s"json/context/action/excludeAllContextTechnologies/?contextName=$contextName"
    callZapApiTo(excludeAllTechnologiesFromContext)

    if(desiredTechnologyNames.nonEmpty) {
      val includeDesiredTechnologiesInContext = s"json/context/action/includeContextTechnologies/?contextName=$contextName&technologyNames=$desiredTechnologyNames"
      callZapApiTo(includeDesiredTechnologiesInContext)
    }

    if(routeToBeIgnoredFromContext.nonEmpty) {
      val excludeRouteFromContext = s"json/context/action/excludeFromContext/?contextName=$contextName&regex=$routeToBeIgnoredFromContext"
      callZapApiTo(excludeRouteFromContext)
    }

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
    bw.write("<html>\n")
    bw.write("<head><style>table {\n      font-family: arial, sans-serif;\n    border-collapse: collapse;\n    width: 100%;\n    }td, th {\n    border: 1px solid #dddddd;\n    text-align: left;\n    padding: 8px;\n    }\n\n </style>")
    bw.write("<title>HMRC Digital: Custom ZAP Report</title>\n")
    bw.write("<h1>HMRC ZAP Report</h1>\n")
    bw.write("</head>\n")
    bw.write("<body>\n <table>\n<tr> <th> Alert Attribute </th> <th> Details </th></tr>")

    bw.write(text)
    bw.write("</table>\n</body>\n")
    bw.write("</html>\n")
    bw.close()
    file
  }

  def reportAlerts(relevantAlerts: List[ZapAlert]): Unit = {
    var text: String = ""

    relevantAlerts.foreach { alert: ZapAlert =>
      var alertColour = "White"
      if (alert.risk == "Low") {alertColour = "yellow"}
      else if (alert.risk == "Medium") {alertColour = "orange"}
      else if (alert.risk == "High") {alertColour = "red"}

      text = text + "<tr bgcolor=\""+alertColour+"\"><td colspan=2><b>" + alert.risk + "</b></td></tr><tr> <td>URL </td><td>" + alert.url + "</td></tr> <tr> <td> Description </td><td>" + alert.description + "</td> </tr><tr><td> Evidence </td><td>" + xml.Utility.escape(alert.evidence) + "</td></tr>"

      println("***********************************")
      println(s"URL:         ${alert.url}")
      println(s"CWE ID:      ${alert.cweid}")
      println(s"Alert Name:  ${alert.alert}")
      println(s"Description: ${alert.description}")
      println(s"Risk Level:  ${alert.risk}")
      println(s"Evidence:    ${alert.evidence.take(100).trim}...")
    }

    val filePath = writeHtmlReportToFile(text).getCanonicalPath
    println(Console.BOLD + s"For more information on alerts, please see the html report at $filePath")

  }

  def reads(json: JsValue): ZapAlert = ZapAlert(
    (json \ "other").as[String],
    (json \ "evidence").as[String],
    (json \ "pluginId").as[String],
    (json \ "cweid").as[String],
    (json \ "confidence").as[String],
    (json \ "wascid").as[String],
    (json \ "description").as[String],
    (json \ "messageId").as[String],
    (json \ "url").as[String],
    (json \ "reference").as[String],
    (json \ "solution").as[String],
    (json \ "alert").as[String],
    (json \ "param").as[String],
    (json \ "attack").as[String],
    (json \ "name").as[String],
    (json \ "risk").as[String],
    (json \ "id").as[String]
  )

  def filterAlerts(): List[ZapAlert] = {
    val getAlerts = s"json/core/view/alerts/?baseurl=$alertsBaseUrl"
    val jsonResponse = Json.parse(callZapApiTo(getAlerts)._2)
    val allAlerts = (jsonResponse \ "alerts").as[List[ZapAlert]]

    val relevantAlerts = allAlerts.filterNot{zapAlert =>
      val filter = zapAlert.getFilter
      alertsToIgnore.contains(filter)
    }

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
      tearDown(context.name, policyName)
    }
  }
}

case class ZapAlerts(alerts: List[ZapAlert])
case class ZapAlert(other: String = "", evidence: String = "", pluginId: String = "", cweid: String, confidence: String = "", wascid: String = "", description: String = "", messageId: String = "", url: String, reference: String = "", solution: String = "", alert: String = "", param: String = "", attack: String = "", name: String = "", risk: String = "", id: String = "") {
  def getFilter = ZapAlertFilter(cweid, url)
}
case class ZapAlertFilter(cweid: String, url: String)
case class Context(name: String, id: String)
