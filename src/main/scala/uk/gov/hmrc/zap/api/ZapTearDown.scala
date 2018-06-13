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

package uk.gov.hmrc.zap.api

import uk.gov.hmrc.zap.client.ZapClient
import uk.gov.hmrc.zap.logger.ZapLogger.log

class ZapTearDown(zapClient: ZapClient) {

  def removeZapSetup(implicit context: ZapContext): Unit = {

    import zapClient._

    log.debug(s"Removing ZAP Context (${context.name}) Policy (${context.policy}), and all alerts.")

    callZapApi("/json/context/action/removeContext", "contextName" -> context.name)
    callZapApi("/json/ascan/action/removeScanPolicy", "scanPolicyName" -> context.policy)
    callZapApi("/json/core/action/deleteAllAlerts")
  }
}
