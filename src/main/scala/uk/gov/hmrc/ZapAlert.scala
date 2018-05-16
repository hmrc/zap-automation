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

case class ZapAlert(other: String = "",
                    method: String = "",
                    evidence: String = "",
                    pluginId: String = "",
                    cweid: String,
                    confidence: String = "",
                    wascid: String = "",
                    description: String = "",
                    messageId: String = "",
                    url: String,
                    reference: String = "",
                    solution: String = "",
                    alert: String = "",
                    param: String = "",
                    attack: String = "",
                    name: String = "",
                    risk: String = "",
                    id: String = "") {

  def riskShortName():String = {
    if (risk == "Informational") "Info"
    else risk
  }

  def references(): List[String] = {
    reference.split("\\n").toList
  }

  def severityScore(): String = {
    s"${riskCodes(risk)}-${confidenceCodes(confidence)}"
  }

  val riskCodes = Map("High"->"1",
                      "Medium"->"2",
                      "Low"->"3",
                      "Informational"->"4")

  val confidenceCodes = Map("High"->"1",
                            "Medium"->"2",
                            "Low"->"3")
}

