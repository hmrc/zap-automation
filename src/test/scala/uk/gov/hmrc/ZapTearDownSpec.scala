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
import uk.gov.hmrc.zap.api.{ZapContext, ZapSetUp, ZapTearDown}
import uk.gov.hmrc.zap.client.{HttpClient, ZapClient}
import uk.gov.hmrc.zap.config.ZapConfiguration

class ZapTearDownSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]
    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
    val zapConfiguration = new ZapConfiguration(config)
    val zapClient = new ZapClient(zapConfiguration, httpClient)
    val zapSetUp = new ZapSetUp(zapClient)
  }

  "tearDown" should {

    "remove context, policy and alerts" in new TestSetup {

      import zapConfiguration._

      private implicit lazy val zapContext: ZapContext = ZapContext(id = "1", name = "name", policy = "policy")
      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      new ZapTearDown(zapClient).removeZapSetup
      verify(httpClient).get(zapBaseUrl, "/json/context/action/removeContext", "contextName" -> zapContext.name)
      verify(httpClient).get(zapBaseUrl, "/json/ascan/action/removeScanPolicy", "scanPolicyName" -> zapContext.policy)
      verify(httpClient).get(eqTo(zapBaseUrl), eqTo("/json/core/action/deleteAllAlerts"), any())
      verify(httpClient).get(eqTo(zapBaseUrl), eqTo("/json/core/action/shutdown"), any())
    }
  }
}
