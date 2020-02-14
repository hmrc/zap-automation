/*
 * Copyright 2020 HM Revenue & Customs
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
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when, times}
import uk.gov.hmrc.zap.api.{ZapAlert, ZapAlerts}
import uk.gov.hmrc.zap.client.{HttpClient, ZapClient}
import uk.gov.hmrc.zap.config.ZapConfiguration

class ZapAlertsSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]
    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
    val zapConfiguration = new ZapConfiguration(config)
    val zapClient = new ZapClient(zapConfiguration, httpClient)
    val zapAlerts = new ZapAlerts(zapClient)
  }

  "filterAlerts" should {

    "should filter out optimizely alerts when they are present and the ignoreOptimizely flag is true" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(evidence = "<script src=\"https://cdn.optimizely.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert(evidence = "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert(evidence = "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = "")
      )

      override lazy val config: Config = updateTestConfigWith("ignoreOptimizelyAlerts=true")

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 2

    }

    "should not filter out optimizely alerts when they are present and the ignoreOptimizely flag is false" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert("", "<script src=\"https://cdn.optimizely.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = "")
      )

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 3
    }

    "should not filter out optimizely alerts when they are not present and the ignoreOptimizely flag is true" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = "")
      )
      override lazy val config: Config = updateTestConfigWith("ignoreOptimizelyAlerts=true")

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 3
    }

    "should filter out alerts that match cweid and url of the ignored alerts list where the url is exact" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://beccy.com/"),
        ZapAlert(cweid = "", url = ""),
        ZapAlert(cweid = "", url = "")
      )

      override lazy val config: Config = updateTestConfigWith("""alertsToIgnore=[{cweid: "16", url: "http://beccy.com/"}]""")

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 2
    }

    "should filter out alerts that match cweid and url of the ignored alerts list where the url is a regex" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/888/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/8888/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/1/here")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=[{cweid: "16", url: "http://localhost:9999/hello/\\w{3}/here"}]""")

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 2
    }

    "should filter out multiple alerts that match both url and cweid of the ignored alerts list" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "17", url = "http://localhost:9999/hello/YZ570921C4/here"),
        ZapAlert(cweid = "18", url = "http://localhost:9999/hello/here")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=[{cweid: 16, url: "http://localhost:9999/hello/\\w{9}/here"},
          {cweid: 17, url: "http://localhost:9999/hello/\\w{10}/here"},
          {cweid: 18, url: "http://localhost:9999/hello/here"}]""")

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 0
    }

    "should only show alerts for scanners that match scanners" in new TestSetup {

      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here", pluginId = "50001"),
        ZapAlert(cweid = "17", url = "http://localhost:9999/hello/YZ570921C4/here", pluginId = "90034"),
        ZapAlert(cweid = "18", url = "http://localhost:9999/hello/here", pluginId = "90035"),
        ZapAlert(cweid = "19", url = "http://localhost:9999/hello/here", pluginId = "90036")
      )

      override lazy val config: Config = updateTestConfigWith(
        """scanners.passive    = [{"id":"50001", "name":"Test Scanner"}]""".stripMargin)

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 1
      filteredAlerts.head.pluginId shouldBe "50001"
    }

    "should not filter out alerts that match url but not cweid of the ignored alerts list where the url is a regex" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "17", url = "http://localhost:9999/hello/YZ570921C/here")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=[{cweid: 16, url: "http://localhost:9999/hello/\\w{9}/here"}]""")

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 1
    }

    "should not filter out alerts that match cweid but not url of the ignored alerts list where the url is a regex" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/YZ570921C/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/YZ570921/here")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=[{cweid: 16, url: "http://localhost:9999/hello/\\w{9}/here"}]""")

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 1
    }

    "should not filter out alerts that match cweid but not url of the ignored alerts list where the url is exact" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/YZ570921/here")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=[{cweid: 16, url: "http://localhost:9999/hello/SB363126A/here"}]""")

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 1
    }


    "should filter urls that include question marks (url parameters)" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:99991/hello/SB363126A/optional/something-else?param1=1234"),
        ZapAlert(cweid = "16", url = "http://localhost:99991/hello/SB363126A/optional/something-else?param1=1234"),
        ZapAlert(cweid = "16", url = "http://localhost:99991/hello/SB363126A/something-else?param1=1234")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=
          |[{cweid: 16, url: "http://localhost:99991/hello/SB363126A(/optional)?/something-else\\?param1=1234"}]
          |""".stripMargin)

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 0
    }

    "should filter urls that include dots (domain separators)" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "https://www.gstatic.com/chrome/intelligence/"),
        ZapAlert(cweid = "16", url = "https://www.gstatic.com/chrome/intelligence/anything-at-all")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=[{cweid: 16, url: "https://www.gstatic.com/chrome/intelligence/.*"}]""")

      val filteredAlerts: List[ZapAlert] = zapAlerts.filterAlerts(alerts)
      filteredAlerts.size shouldBe 0
    }
  }

  "parseAlerts" should {

    "should parse alerts from the Zap API" in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((200,
        """{
                                                                                      "alerts": [
                                                                                      {
                                                                                      "sourceid": "",
                                                                                      "other": "Other text",
                                                                                      "method": "",
                                                                                      "evidence": "",
                                                                                      "pluginId": "",
                                                                                      "cweid": "",
                                                                                      "confidence": "",
                                                                                      "wascid": "",
                                                                                      "description": "",
                                                                                      "messageId": "",
                                                                                      "url": "",
                                                                                      "reference": "",
                                                                                      "solution": "",
                                                                                      "alert": "",
                                                                                      "param": "",
                                                                                      "attack": "",
                                                                                      "name": "",
                                                                                      "risk": "",
                                                                                      "id": ""
                                                                                      }
                                                                                      ]
                                                                                      }"""))

      val alert1: ZapAlert = ZapAlert("Other text", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")

      zapAlerts.parsedAlerts should contain theSameElementsAs List(alert1)
      verify(httpClient, times(1)).get(any(), any(), any())
    }

    "should call zap alerts endpoint for every url in alertUrlsToReport config" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith(
        """alertUrlsToReport = [
          |    "http://localhost:1234",
          |    "http://localhost:5678"]""")

      when(httpClient.get(any(), eqTo("/json/alert/view/alerts"), any())).thenReturn((200,
      """{
                                                                                      "alerts": [
                                                                                      {
                                                                                      "sourceid": "",
                                                                                      "other": "Other text",
                                                                                      "method": "",
                                                                                      "evidence": "",
                                                                                      "pluginId": "",
                                                                                      "cweid": "",
                                                                                      "confidence": "",
                                                                                      "wascid": "",
                                                                                      "description": "",
                                                                                      "messageId": "",
                                                                                      "url": "",
                                                                                      "reference": "",
                                                                                      "solution": "",
                                                                                      "alert": "",
                                                                                      "param": "",
                                                                                      "attack": "",
                                                                                      "name": "",
                                                                                      "risk": "",
                                                                                      "id": ""
                                                                                      }
                                                                                      ]
                                                                                      }"""))
      zapAlerts.parsedAlerts.size shouldBe 2
      verify(httpClient, times(2)).get(any(), any(), any())
    }

    "should call zap alerts endpoint once when alertUrlsToReport config is []" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("""alertUrlsToReport = []""")

      when(httpClient.get(any(), eqTo("/json/alert/view/alerts"), any())).thenReturn((200,
        """{
                                                                                      "alerts": [
                                                                                      {
                                                                                      "sourceid": "",
                                                                                      "other": "Other text",
                                                                                      "method": "",
                                                                                      "evidence": "",
                                                                                      "pluginId": "",
                                                                                      "cweid": "",
                                                                                      "confidence": "",
                                                                                      "wascid": "",
                                                                                      "description": "",
                                                                                      "messageId": "",
                                                                                      "url": "",
                                                                                      "reference": "",
                                                                                      "solution": "",
                                                                                      "alert": "",
                                                                                      "param": "",
                                                                                      "attack": "",
                                                                                      "name": "",
                                                                                      "risk": "",
                                                                                      "id": ""
                                                                                      }
                                                                                      ]
                                                                                      }"""))
      zapAlerts.parsedAlerts.size shouldBe 1
      verify(httpClient, times(1)).get(any(), any(), any())
    }
  }

  "applyCustomRisk" should {

    "apply custom risk level to the configured pluginId" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("""customRiskConf = [
      {pluginId: "322420643", risk: "High"},
      {pluginId: "10000", risk: "Medium"}
      ]""")

      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", pluginId = "322420643",  url = "http://localhost:99991/hello/SB363126A/optional/something-else?param1=1234", risk = "Low"),
        ZapAlert(cweid = "16", pluginId = "100090",  url = "http://localhost:99991/hello/SB363126A/optional/something-else?param1=1234", risk = "Low"),
        ZapAlert(cweid = "16", pluginId = "10000",  url = "http://localhost:99991/hello/SB363126A/optional/something-else?param1=1234", risk = "Low")
      )

      val relevantAlerts: List[ZapAlert] = alerts.map(zapAlerts.applyRiskLevel)
      relevantAlerts.find(alert => alert.pluginId.equals("322420643")).get.risk shouldBe "High"
      relevantAlerts.find(alert => alert.pluginId.equals("100090")).get.risk shouldBe "Low"
      relevantAlerts.find(alert => alert.pluginId.equals("10000")).get.risk shouldBe "Medium"
    }
  }
}
