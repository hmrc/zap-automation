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
import org.mockito.Mockito.when
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.zap.client.HttpClient
import uk.gov.hmrc.zap.config.ZapConfiguration
import uk.gov.hmrc.zap.{OwaspZap, ZapAlert, ZapException}

class OwaspZapSpec extends BaseSpec {

  trait TestSetup {
    val httpClient: HttpClient = mock[HttpClient]

    implicit val zapAlertReads: Reads[ZapAlert] = Json.reads[ZapAlert]
    lazy val config: Config = ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")

    val zapConfiguration = new ZapConfiguration(config)
    val zapApi = new OwaspZap(zapConfiguration, httpClient)

  }


  "callZapApiTo" should {

    "return a response" in new TestSetup {
      when(httpClient.get(zapConfiguration.zapBaseUrl, "/someUrl")).thenReturn((200, "the-response"))

      val response: String = zapApi.callZapApi("/someUrl")
      response shouldBe "the-response"
    }

    "fail the test when the status code is not a 200" in new TestSetup {
      when(httpClient.get(zapConfiguration.zapBaseUrl, "/someInvalidUrl")).thenReturn((400, "the-response"))
      try {
        zapApi.callZapApi("/someInvalidUrl")
      }
      catch {
        case e: ZapException => e.getMessage shouldBe "Expected response code is 200 for /someInvalidUrl, received:400"
      }
    }
  }
}
