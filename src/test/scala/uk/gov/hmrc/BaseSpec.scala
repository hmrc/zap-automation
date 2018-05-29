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

import com.typesafe.config.ConfigFactory
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import uk.gov.hmrc.utils.HttpClient
import uk.gov.hmrc.utils.ZapConfiguration._
import uk.gov.hmrc.zap.ZapApi._



class BaseSpec extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  val wsClientMock: HttpClient = mock[HttpClient]

  httpClient = wsClientMock

  override protected def beforeEach(): Unit = {
    Mockito.reset(wsClientMock)
  }

  def updateTestConfigWith(config: String): Unit = {
    zapConfig =  ConfigFactory.parseString(config).
      withFallback(
        ConfigFactory.parseResources("test.conf").getConfig("zap-automation-config")
      )
  }
}