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
import org.mockito.Matchers.any
import org.mockito.Mockito.{doThrow, verify, when}
import uk.gov.hmrc.zap.client.HttpClient
import uk.gov.hmrc.zap.config.ZapConfiguration
import uk.gov.hmrc.zap.{HealthCheck, ZapException}


class HealthCheckSpec extends BaseSpec {


  trait TestSetup extends HealthCheck {
    override val httpClient: HttpClient = mock[HttpClient]
    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
    val zapConfiguration = new ZapConfiguration(config)
  }

  "Health Check" should {

    "be performed if healthCheck config is set to true" in new TestSetup {
      val healthCheckHost: String = "http:\\/\\/localhost:\\d+".r.findFirstIn(zapConfiguration.testUrl).get
      when(httpClient.get(any(), any(), any())).thenReturn((200, "the-response"))

      healthCheck(zapConfiguration.testUrl)

      verify(httpClient).get(healthCheckHost, "/ping/ping")
    }

    "fail if healthCheck response status code did not match 200" in new TestSetup {
      when(httpClient.get(any(), any(), any())).thenReturn((400, "the-response"))

      intercept[ZapException](healthCheck(zapConfiguration.testUrl))
    }

    "throw a ZapException if a non-fatal exception occurs" in new TestSetup {
      val exception = new RuntimeException("some non-fatal exception")
      doThrow(exception).when(httpClient).get(any(), any(), any())

      intercept[ZapException](healthCheck(zapConfiguration.testUrl))
    }

    "non handle fatal exceptions" in new TestSetup {
      val fatalException = new OutOfMemoryError
      doThrow(fatalException).when(httpClient).get(any(), any(), any())

      intercept[OutOfMemoryError](healthCheck(zapConfiguration.testUrl))
    }

  }
}
