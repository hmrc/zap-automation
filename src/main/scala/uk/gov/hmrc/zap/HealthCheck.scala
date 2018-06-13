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

package uk.gov.hmrc.zap

import uk.gov.hmrc.zap.client.{HttpClient, WsClient}
import uk.gov.hmrc.zap.logger.ZapLogger._

import scala.util.control.NonFatal

trait HealthCheck {

  val httpClient: HttpClient = WsClient

  def healthCheck(localHostUrl: String): Unit = {
    val localHostRegex = "http:\\/\\/localhost:\\d+".r
    val healthCheckHost = localHostRegex.findFirstIn(localHostUrl).get
    val healthCheckUrl = s"$healthCheckHost/ping/ping"

    log.info(s"Performing health check for the test URL with: $healthCheckUrl")

    val (status, _) = try {
      httpClient.get(healthCheckHost, "/ping/ping")

    }
    catch {
      case NonFatal(e) => throw ZapException(s"Health check failed for test URL: $healthCheckUrl with exception:${e.getMessage}")
    }

    status match {
      case 200 => ()
      case _ => throw ZapException(s"Health check failed for test URL: $healthCheckUrl with status:$status")
    }
  }
}