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
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import uk.gov.hmrc.utils.{HttpClient, ZapConfiguration}
import uk.gov.hmrc.zap.{OwaspZap, ZapSetUp}

class ZapSetupSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]
    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
    val zapConfiguration = new ZapConfiguration(config)
    val owaspZap = new OwaspZap(zapConfiguration, httpClient)
    val zapSetUp = new ZapSetUp(owaspZap)
  }

  "createContext" should {

    "create a Zap context" in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((200, "{\n\"contextId\": \"2\"\n}"))
      zapSetUp.createContext()
      zapSetUp.contextId shouldBe "2"
    }
  }

  "setUpContext" should {

    "set up the context for a given context name " in new TestSetup {
      val contextName = "context1"

      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapSetUp.setUpContext(contextName)
      verify(httpClient).get(zapBaseUrl, "/json/context/action/includeInContext", "contextName" -> contextName, "regex" -> contextBaseUrl)
      verify(httpClient).get(zapBaseUrl, "/json/context/action/excludeAllContextTechnologies", "contextName" -> contextName)
      verify(httpClient).get(zapBaseUrl, "/json/context/action/includeContextTechnologies", "contextName" -> contextName, "technologyNames" -> desiredTechnologyNames)
    }
  }

  "createPolicy" should {

    "create a policy in Zap " in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapSetUp.createPolicy()

      verify(httpClient).get(zapConfiguration.zapBaseUrl, "/json/ascan/action/addScanPolicy", "scanPolicyName" -> zapSetUp.policyName)
    }
  }

  "setUpPolicy" should {

    "set up the policy with scanners meant for UI testing" in new TestSetup {

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapSetUp.setUpPolicy("policyName")
      verify(httpClient).get(eqTo(zapConfiguration.zapBaseUrl), eqTo("/json/ascan/action/disableScanners"), any())

    }

    "call the Zap API to set up the policy with scanners meant for API testing" in new TestSetup {

      override lazy val config: Config = updateTestConfigWith("testingAnApi=true")

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      zapSetUp.setUpPolicy("policyName")
      verify(httpClient).get(eqTo(zapConfiguration.zapBaseUrl), eqTo("/json/ascan/action/disableAllScanners"), any())
      verify(httpClient).get(eqTo(zapConfiguration.zapBaseUrl), eqTo("/json/ascan/action/enableScanners"), any())
    }
  }

}
