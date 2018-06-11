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
import uk.gov.hmrc.zap._

class ZapTearDownSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]
    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
    val zapConfiguration = new ZapConfiguration(config)
    val owaspZap = new OwaspZap(zapConfiguration, httpClient)
    val zapSetUp = new ZapSetUp(owaspZap)
  }

  "tearDown" should {

    "remove context, policy and alerts" in new TestSetup {

      import zapConfiguration._

      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      ZapTearDown(owaspZap, zapSetUp)
      verify(httpClient).get(eqTo(zapBaseUrl), eqTo("/json/context/action/removeContext"), "contextName" -> any())
      verify(httpClient).get(eqTo(zapBaseUrl), eqTo("/json/ascan/action/removeScanPolicy"), "scanPolicyName" -> any())
      verify(httpClient).get(eqTo(zapBaseUrl), eqTo("/json/core/action/deleteAllAlerts"), any())
    }
  }
}
