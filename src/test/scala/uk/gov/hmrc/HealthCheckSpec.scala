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
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.exceptions.TestFailedException

class HealthCheckSpec extends BaseSpec {

  describe("Health Check") {

    it("should be performed if healthCheck config is set to true") {
      when(wsClientMock.getRequest(zapTest.testUrl)).thenReturn((200, "the-response"))

      zapTest.healthCheckTestUrl()
      verify(wsClientMock).getRequest(zapTest.testUrl)
    }

    it("should not be performed if healthCheck config is set to false") {

      val zapTest = new StubbedZapTest {
        logger.info("Overriding default healthCheck config for false")
        override val zapConfig: Config = ConfigFactory.parseString("debug.healthCheck=false")
      }
      when(wsClientMock.getRequest(zapTest.testUrl)).thenReturn((200, "the-response"))

      zapTest.healthCheckTestUrl()
      Mockito.verifyZeroInteractions(wsClientMock)
    }

    it("should fail if healthCheck response status code did not match 2xx or 3xx") {

      when(wsClientMock.getRequest(zapTest.testUrl)).thenReturn((400, "the-response"))
      try {
        zapTest.healthCheckTestUrl()
      }
      catch {
        case e: TestFailedException => e.getMessage() shouldBe s"Health Check failed for test URL: ${zapTest.testUrl} with status:400"
      }
    }

    it("should not fail if healthCheck response status code 2xx") {

      when(wsClientMock.getRequest(zapTest.testUrl)).thenReturn((200, "the-response"))
      zapTest.healthCheckTestUrl()
    }

    it("should not fail if healthCheck response status code 3xx") {

      when(wsClientMock.getRequest(zapTest.testUrl)).thenReturn((302, "the-response"))
      zapTest.healthCheckTestUrl()
    }
  }
}
