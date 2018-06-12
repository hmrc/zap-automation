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

object ZapTestStatus {

  def isTestSucceeded(relevantAlerts: List[ZapAlert], failureThreshold: String): Boolean = {

    val failingAlerts = failureThreshold match {
      case "High" => relevantAlerts.filterNot(zapAlert => zapAlert.risk == "Informational" || zapAlert.risk == "Low" || zapAlert.risk == "Medium")
      case "Medium" => relevantAlerts.filterNot(zapAlert => zapAlert.risk == "Informational" || zapAlert.risk == "Low")
      case "Low" => relevantAlerts.filterNot(zapAlert => zapAlert.risk == "Informational")
      case _ => relevantAlerts.filterNot(zapAlert => zapAlert.risk == "Informational")
    }
    failingAlerts.isEmpty
  }
}
