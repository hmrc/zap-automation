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

package uk.gov.hmrc.zap.client

import uk.gov.hmrc.zap.ZapException
import uk.gov.hmrc.zap.config.ZapConfiguration


class ZapClient(val zapConfiguration: ZapConfiguration, httpClient: HttpClient = WsClient) {

  def callZapApi(queryPath: String, params: (String, String)*): String = {

    val (status, response) = httpClient.get(zapConfiguration.zapBaseUrl, queryPath, params: _*)

    if (status != 200) {
      throw ZapException(s"Expected response code is 200 for $queryPath, received:$status")
    }
    response
  }
}
