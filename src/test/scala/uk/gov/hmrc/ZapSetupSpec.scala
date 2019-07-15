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
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import uk.gov.hmrc.zap.api.{Scanner, ZapContext, ZapSetUp}
import uk.gov.hmrc.zap.client.{HttpClient, ZapClient}
import uk.gov.hmrc.zap.config.ZapConfiguration

class ZapSetupSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]
    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
    val zapConfiguration = new ZapConfiguration(config)
    val zapClient = new ZapClient(zapConfiguration, httpClient)
    val zapSetUp = new ZapSetUp(zapClient)
  }

  "initialize Zap setup" should {

    "create a Zap context and policy" in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((200, "{\n\"contextId\": \"2\"\n}"))
      val zapContext: ZapContext = zapSetUp.initialize()
      zapContext.id shouldBe "2"
    }
  }

  "setUpContext" should {

    "set up the context for a given context name " in new TestSetup {

      import zapConfiguration._

      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapSetUp.setUpContext
      verify(httpClient).get(zapBaseUrl, "/json/context/action/includeInContext", "contextName" -> zapContext.name, "regex" -> contextBaseUrlRegex)
      verify(httpClient).get(zapBaseUrl, "/json/context/action/excludeAllContextTechnologies", "contextName" -> zapContext.name)
      verify(httpClient).get(zapBaseUrl, "/json/context/action/includeContextTechnologies", "contextName" -> zapContext.name, "technologyNames" -> desiredTechnologyNames)
    }
  }

  "setUpPolicy" should {

    "set up the policy with scanners meant for UI testing" in new TestSetup {
      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapSetUp.setUpPolicy
      verify(httpClient).get(eqTo(zapConfiguration.zapBaseUrl), eqTo("/json/ascan/action/disableScanners"), any())

    }

    "call the Zap API to set up the policy with scanners meant for API testing" in new TestSetup {
      override lazy val config: Config = updateTestConfigWith("testingAnApi=true")
      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapSetUp.setUpPolicy
      verify(httpClient).get(eqTo(zapConfiguration.zapBaseUrl), eqTo("/json/ascan/action/disableAllScanners"), any())
      verify(httpClient).get(eqTo(zapConfiguration.zapBaseUrl), eqTo("/json/ascan/action/enableScanners"), any())
    }
  }

  "setUpConnectionTimeout" should {

    "set up Zap connection timeout with provided timeout value" in new TestSetup {
      override lazy val config: Config = updateTestConfigWith("debug.connectionTimeout=10")
      when(httpClient.get(any(), any(), any())).thenReturn((200, """{"Result":"OK"}"""))

      zapSetUp.setConnectionTimeout()
      verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/core/action/setOptionTimeoutInSecs", "Integer" -> "10")
    }

    "set up Zap connection timeout with default timeout value when not provided" in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((200, """{"Result":"OK"}"""))

      zapSetUp.setConnectionTimeout()
      verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/core/action/setOptionTimeoutInSecs", "Integer" -> "20")
    }
  }

  "checkMissingScanners should be empty when all the required scanners are available and enabled" in new TestSetup {

    override lazy val config: Config = updateTestConfigWith("""scanners.passive = [{"id":"50001", "name":"testScanner"}]""".stripMargin)

    when(httpClient.get(any(), any(), any())).thenReturn((200, """{
                                                                 |   "scanners":[
                                                                 |      {
                                                                 |         "alertThreshold":"DEFAULT",
                                                                 |         "name":"Script Passive Scan Rules",
                                                                 |         "id":"50001",
                                                                 |         "enabled":"true",
                                                                 |         "quality":"release"
                                                                 |      },
                                                                 |      {
                                                                 |         "alertThreshold":"DEFAULT",
                                                                 |         "name":"Stats Passive Scan Rule",
                                                                 |         "id":"50003",
                                                                 |         "enabled":"true",
                                                                 |         "quality":"release"
                                                                 |      }
                                                                 |   ]
                                                                 |}""".stripMargin))

    val missingScanners: List[Scanner] = zapSetUp.checkScannerSetup()

    verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/pscan/view/scanners")
    verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/ascan/view/scanners")
    missingScanners shouldBe empty
  }

  "checkMissingScanners should return scannerIds that are available but not enabled" in new TestSetup {
    override lazy val config: Config = updateTestConfigWith("""scanners.passive = [{
                                                              |   "id":"50001",
                                                              |   "name":"testScanner"
                                                              |},
                                                              |{
                                                              |   "id":"50003",
                                                              |   "name":"disabledScanner"
                                                              |}]""".stripMargin)

    when(httpClient.get(any(), any(), any())).thenReturn((200,"""{
                                                                |   "scanners":[
                                                                |      {
                                                                |         "alertThreshold":"DEFAULT",
                                                                |         "name":"Script Passive Scan Rules",
                                                                |         "id":"50001",
                                                                |         "enabled":"true",
                                                                |         "quality":"release"
                                                                |      },
                                                                |      {
                                                                |         "alertThreshold":"DEFAULT",
                                                                |         "name":"Stats Passive Scan Rule",
                                                                |         "id":"50003",
                                                                |         "enabled":"false",
                                                                |         "quality":"release"
                                                                |      }
                                                                |   ]
                                                                |}""".stripMargin))


    val missingScanners: List[Scanner] = zapSetUp.checkScannerSetup()

    verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/pscan/view/scanners")
    verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/ascan/view/scanners")
    missingScanners.head.id shouldBe "50003"
    missingScanners.size shouldBe 1
  }


  "checkMissingScanners should return list of required scanners that are not configured" in new TestSetup {

    override lazy val config: Config = updateTestConfigWith("""scanners.passive=[{"id":"99999", "name":"Test Scanner"}]""")
    when(httpClient.get(any(), any(), any())).thenReturn((200,"""{
                                                                |   "scanners":[
                                                                |      {
                                                                |         "alertThreshold":"DEFAULT",
                                                                |         "name":"Script Passive Scan Rules",
                                                                |         "id":"50001",
                                                                |         "enabled":"true",
                                                                |         "quality":"release"
                                                                |      },
                                                                |      {
                                                                |         "alertThreshold":"DEFAULT",
                                                                |         "name":"Stats Passive Scan Rule",
                                                                |         "id":"50003",
                                                                |         "enabled":"true",
                                                                |         "quality":"release"
                                                                |      }
                                                                |   ]
                                                                |}""".stripMargin))
    val missingScanners: List[Scanner] = zapSetUp.checkScannerSetup()

    verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/pscan/view/scanners")
    verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/ascan/view/scanners")
    missingScanners.size shouldBe 1
    missingScanners.head.id shouldBe "99999"
    missingScanners.head.name shouldBe "Test Scanner"
  }

  "findZapVersion should return zap version" in new TestSetup {

    when(httpClient.get(any(), any(), any())).thenReturn((200,"""{"@version":"2.8.0"}""".stripMargin))
    val zapVersion: String = zapSetUp.findZapVersion

    verify(httpClient).get(zapConfiguration.zapBaseUrl, "/other/core/other/jsonreport")
    zapVersion shouldBe "2.8.0"
  }

  "findZapVersion should return ZAP_VERSION_NOT_FOUND when version not returned by ZAP API" in new TestSetup {

    when(httpClient.get(any(), any(), any())).thenReturn((200,"""{"@NoVersion":""}""".stripMargin))
    val zapVersion: String = zapSetUp.findZapVersion

    verify(httpClient).get(zapConfiguration.zapBaseUrl, "/other/core/other/jsonreport")
    zapVersion shouldBe "ZAP_VERSION_NOT_FOUND"
  }

}
