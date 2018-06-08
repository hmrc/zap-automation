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

import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.utils.{HttpClient, ZapConfiguration}
import uk.gov.hmrc.zap.{ZapAlert, ZapApi}

class AlertsSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]
    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
    val zapConfiguration = new ZapConfiguration(config)

    val zapApi = new ZapApi(zapConfiguration, httpClient)

  }

  "filterAlerts" should {

    "should filter out optimizely alerts when they are present and the ignoreOptimizely flag is true" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(evidence = "<script src=\"https://cdn.optimizely.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert(evidence = "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert(evidence = "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = "")
      )

      override lazy val config: Config = updateTestConfigWith("ignoreOptimizelyAlerts=true")

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
      filteredAlerts.size shouldBe 2

    }

    "should not filter out optimizely alerts when they are present and the ignoreOptimizely flag is false" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert("", "<script src=\"https://cdn.optimizely.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = "")
      )

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
      filteredAlerts.size shouldBe 3
    }

    "should not filter out optimizely alerts when they are not present and the ignoreOptimizely flag is true" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = ""),
        ZapAlert("", "<script src=\"https://cdn.otherevidence.com/public/7589613084/s/pta_tenant.js\"></script>", url = "", cweid = "")
      )
      override lazy val config: Config = updateTestConfigWith("ignoreOptimizelyAlerts=true")

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
      filteredAlerts.size shouldBe 3
    }

    "should filter out alerts that match cweid and url of the ignored alerts list where the url is exact" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://beccy.com/"),
        ZapAlert(cweid = "", url = ""),
        ZapAlert(cweid = "", url = "")
      )

      override lazy val config: Config = updateTestConfigWith("""alertsToIgnore=[{cweid: "16", url: "http://beccy.com/"}]""")

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
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

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
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

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
      filteredAlerts.size shouldBe 0
    }

    "should not filter out alerts that match url but not cweid of the ignored alerts list where the url is a regex" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "17", url = "http://localhost:9999/hello/YZ570921C/here")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=[{cweid: 16, url: "http://localhost:9999/hello/\\w{9}/here"}]""")

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
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

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
      filteredAlerts.size shouldBe 1
    }

    "should not filter out alerts that match cweid but not url of the ignored alerts list where the url is exact" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/SB363126A/here"),
        ZapAlert(cweid = "16", url = "http://localhost:9999/hello/YZ570921/here")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=[{cweid: 16, url: "http://localhost:9999/hello/SB363126A/here"}]""")

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
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

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
      filteredAlerts.size shouldBe 0
    }

    "should filter urls that include dots (domain separators)" in new TestSetup {
      val alerts: List[ZapAlert] = List[ZapAlert](
        ZapAlert(cweid = "16", url = "https://www.gstatic.com/chrome/intelligence/"),
        ZapAlert(cweid = "16", url = "https://www.gstatic.com/chrome/intelligence/anything-at-all")
      )

      override lazy val config: Config = updateTestConfigWith(
        """alertsToIgnore=[{cweid: 16, url: "https://www.gstatic.com/chrome/intelligence/.*"}]""")

      val filteredAlerts: List[ZapAlert] = zapApi.filterAlerts(alerts)
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
      zapApi.parsedAlerts should contain theSameElementsAs List(alert1)
    }
  }
}
