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

package uk.gov.hmrc

import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.zap.ZapReport._
import uk.gov.hmrc.zap.api.{ScanCompleted, ScanNotCompleted, Scanner, ZapAlert}
import uk.gov.hmrc.zap.client.HttpClient
import uk.gov.hmrc.zap.config.ZapConfiguration

import scala.xml.{Elem, Node, NodeSeq, XML}

class ZapReportSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]

    implicit val zapAlertReads: Reads[ZapAlert] = Json.reads[ZapAlert]
    val alerts: List[ZapAlert] = Json.parse(alertJson).as[List[ZapAlert]]
    val threshold = "AUniqueThreshold"
    val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
    val zapConfiguration = new ZapConfiguration(config)
    val missingScanners: List[Scanner] = List()
  }

  "html report" should {
    "should contain the failure threshold so that " in new TestSetup {
      val reportHtmlAsString: String = generateHtmlReport(alerts, threshold, spiderScanStatus = ScanCompleted,
        activeScanStatus = ScanNotCompleted, missingScanners)

      reportHtmlAsString should include("AUniqueThreshold")
    }

    "should contain the correct alert count by risk in the Summary of Alerts table" in new TestSetup {
      val reportHtmlAsString: String = generateHtmlReport(alerts, "AUniqueThreshold",
        spiderScanStatus = ScanCompleted, activeScanStatus = ScanNotCompleted, missingScanners)
      val reportXml: Elem = XML.loadString(reportHtmlAsString)

      getByAtt(reportXml, "id", "summary-high-count").text shouldBe "1"
      getByAtt(reportXml, "id", "summary-medium-count").text shouldBe "1"
      getByAtt(reportXml, "id", "summary-low-count").text shouldBe "1"
      getByAtt(reportXml, "id", "summary-info-count").text shouldBe "1"
    }

    "should show the correct scan status in the Summary of Scan table when spiderScan and activeScan is not completed" in new TestSetup {
      val reportHtmlAsString: String = generateHtmlReport(alerts, "AUniqueThreshold",
        spiderScanStatus = ScanNotCompleted, activeScanStatus = ScanNotCompleted, missingScanners)
      val reportXml: Elem = XML.loadString(reportHtmlAsString)

      getByAtt(reportXml, "id", "passive-scan").text shouldBe "Run"
      getByAtt(reportXml, "id", "spider-scan").text shouldBe "Not Run"
      getByAtt(reportXml, "id", "active-scan").text shouldBe "Not Run"
    }

    "should show the correct scan status in the Summary of Scan table when spiderScan and ActiveScan is completed" in new TestSetup {
      val reportHtmlAsString: String = generateHtmlReport(alerts, "AUniqueThreshold",
        spiderScanStatus = ScanCompleted, activeScanStatus = ScanCompleted, missingScanners)
      val reportXml: Elem = XML.loadString(reportHtmlAsString)

      getByAtt(reportXml, "id", "passive-scan").text shouldBe "Run"
      getByAtt(reportXml, "id", "spider-scan").text shouldBe "Run"
      getByAtt(reportXml, "id", "active-scan").text shouldBe "Run"
    }

    "should display the details of 4 alerts" in new TestSetup {
      val reportHtmlAsString: String = generateHtmlReport(alerts, "AUniqueThreshold",
        spiderScanStatus = ScanCompleted, activeScanStatus = ScanCompleted, missingScanners)
      val reportXml: Elem = XML.loadString(reportHtmlAsString)

      getByAtt(reportXml, "type", "alert-details").size shouldBe 4
    }

    "should display missing scanners when all required scanners are not configured" in new TestSetup {
      override val missingScanners: List[Scanner] =
        List(Scanner("9999", "TestScanner", "Passive Scan"), Scanner("10000", "TestScanner", "Passive Scan"))

      val reportHtmlAsString: String = generateHtmlReport(alerts, "AUniqueThreshold",
        spiderScanStatus = ScanCompleted, activeScanStatus = ScanCompleted, missingScanners)
      val reportXml: Elem = XML.loadString(reportHtmlAsString)

      getByAtt(reportXml, "id", "missing-scanners-h3").size shouldBe 1
      getByAtt(reportXml, "id", "missing-scanners-header").size shouldBe 1
      getByAtt(reportXml, "type", "missing-scanners").size shouldBe 2
    }

    "should not display missing scanners list when all required scanners configured" in new TestSetup {
      val reportHtmlAsString: String = generateHtmlReport(alerts, "AUniqueThreshold",
        spiderScanStatus = ScanCompleted, activeScanStatus = ScanCompleted, missingScanners)
      val reportXml: Elem = XML.loadString(reportHtmlAsString)

      getByAtt(reportXml, "type", "missing-scanners").size shouldBe 0
      getByAtt(reportXml, "id", "missing-scanners-h3").size shouldBe 0
      getByAtt(reportXml, "id", "missing-scanners-header").size shouldBe 0
    }

  }

  def getByAtt(e: Elem, att: String, value: String): NodeSeq = {
    def filterAttribute(node: Node, att: String, value: String) = (node \ ("@" + att)).text == value

    e \\ "_" filter { n => filterAttribute(n, att, value) }
  }

  val alertJson: String =
    """
      [{
      "sourceid": "3",
      "other": "",
      "method": "GET",
      "evidence": "no-store",
      "pluginId": "10049",
      "cweid": "524",
      "confidence": "Medium",
      "wascid": "13",
      "description": "A short description",
      "messageId": "1",
      "url": "http://some.url",
      "reference": "https://tools.ietf.org/html/rfc7234",
      "solution": "A suggested solution",
      "alert": "Non-Storable Content",
      "param": "",
      "attack": "",
      "name": "Non-Storable Content",
      "risk": "High",
      "id": "1"
      },
      {
      "sourceid": "3",
      "other": "",
      "method": "GET",
      "evidence": "no-store",
      "pluginId": "10049",
      "cweid": "524",
      "confidence": "Medium",
      "wascid": "13",
      "description": "A short description",
      "messageId": "1",
      "url": "http://some.url",
      "reference": "https://tools.ietf.org/html/rfc7234",
      "solution": "A suggested solution",
      "alert": "Non-Storable Content",
      "param": "",
      "attack": "",
      "name": "Non-Storable Content",
      "risk": "Medium",
      "id": "2"
      },
      {
      "sourceid": "3",
      "other": "",
      "method": "GET",
      "evidence": "no-store",
      "pluginId": "10049",
      "cweid": "524",
      "confidence": "Medium",
      "wascid": "13",
      "description": "A short description",
      "messageId": "1",
      "url": "http://some.url",
      "reference": "https://tools.ietf.org/html/rfc7234",
      "solution": "A suggested solution",
      "alert": "Non-Storable Content",
      "param": "",
      "attack": "",
      "name": "Non-Storable Content",
      "risk": "Low",
      "id": "3"
      },
      {
      "sourceid": "3",
      "other": "",
      "method": "GET",
      "evidence": "no-store",
      "pluginId": "10049",
      "cweid": "524",
      "confidence": "Medium",
      "wascid": "13",
      "description": "A short description",
      "messageId": "1",
      "url": "http://some.url",
      "reference": "https://tools.ietf.org/html/rfc7234",
      "solution": "A suggested solution",
      "alert": "Non-Storable Content",
      "param": "",
      "attack": "",
      "name": "Non-Storable Content",
      "risk": "Informational",
      "id": "4"
    }]
    """
}
