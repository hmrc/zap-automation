/*
 * Copyright 2017 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.gov.hmrc.utils.InsecureClient
import ArgumentMatchers.{eq => eqTo}
import org.scalatest.exceptions.TestFailedException

class ZapTestSpec extends FunSpec with Matchers with MockitoSugar {

  val insecureClientMock = mock[InsecureClient]

  val zapTest = new ZapTest {
    override lazy val theClient: InsecureClient = insecureClientMock
    override val zapBaseUrl: String = "http://zap.url.com"
    override val testUrl: String = "something"
  }

  describe("callZapApiTo") {

    it("should add a slash to the url, if it's not present") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "the-response"))

      zapTest.callZapApiTo("someUrl")
      verify(insecureClientMock).getRawResponse(eqTo("http://zap.url.com/someUrl"), any())(any())
    }

    it("should return a status code and a response") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "the-response"))

      val (code, response) = zapTest.callZapApiTo("someUrl")
      code shouldBe(200)
      response shouldBe("the-response")
    }

    it("should fail the test when the status code is not a 200") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((404, "the-response"))
      try {
        zapTest.callZapApiTo("someInvalidUrl")
      } catch {
        case e: TestFailedException => e.getMessage() shouldBe("The ZAP API returned a 404 status when you called it using: someInvalidUrl")
      }

    }
  }


  describe ("hasCallCompleted"){

    it("should return true if status is 100"){
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "{\n\"status\": \"100\"\n}"))
      val answer = zapTest.hasCallCompleted("someUrl")
      answer shouldBe(true)
    }

    it("should return false if status is not 100"){
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "{\n\"status\": \"99\"\n}"))
      val answer = zapTest.hasCallCompleted("someUrl")
      answer shouldBe(false)
    }
  }

  describe("createContext") {
    it("should return the context ID") {
      when(insecureClientMock.getRawResponse(any(), any())(any())).thenReturn((200, "{\n\"contextId\": \"2\"\n}"))

      val context: Context = zapTest.createContext()
      context.id shouldBe("2")
    }
  }

}
