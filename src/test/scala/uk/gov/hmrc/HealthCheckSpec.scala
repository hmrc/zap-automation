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

import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import uk.gov.hmrc.utils.ZapConfiguration._
import uk.gov.hmrc.zap.ZapApi._
import uk.gov.hmrc.zap.ZapException

class HealthCheckSpec extends BaseSpec {

  describe("Health Check") {

    it("should be performed if healthCheck config is set to true") {

      updateTestConfigWith("debug.healthCheck=true")

      when(wsClientMock.getRequest(testUrl)).thenReturn((200, "the-response"))

      healthCheckTestUrl()
      verify(wsClientMock).getRequest(testUrl)
    }

    it("should not be performed if healthCheck config is set to false") {

      updateTestConfigWith("debug.healthCheck=false")

      when(wsClientMock.getRequest(testUrl)).thenReturn((200, "the-response"))

      healthCheckTestUrl()
      Mockito.verifyZeroInteractions(wsClientMock)
    }

    it("should fail if healthCheck response status code did not match 2xx or 3xx") {

      updateTestConfigWith("debug.healthCheck=true")

      when(wsClientMock.getRequest(testUrl)).thenReturn((400, "the-response"))
      try {
        healthCheckTestUrl()
      }
      catch {
        case e: ZapException => e.getMessage() shouldBe s"Health Check failed for test URL: $testUrl with status:400"
      }
    }

    it("should not fail if healthCheck response status code 2xx") {

      updateTestConfigWith("debug.healthCheck=true")

      when(wsClientMock.getRequest(testUrl)).thenReturn((200, "the-response"))
      healthCheckTestUrl()
    }

    it("should not fail if healthCheck response status code 3xx") {

      when(wsClientMock.getRequest(testUrl)).thenReturn((302, "the-response"))
      healthCheckTestUrl()
    }
  }
}
