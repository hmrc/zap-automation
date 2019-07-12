/*
 * Copyright 2019 HM Revenue & Customs
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

import java.io.{BufferedWriter, File, FileWriter}

import uk.gov.hmrc.zap.api.{ScanStatus, Scanner, ZapAlert}
import uk.gov.hmrc.zap.logger.ZapLogger._

object ZapReport {

  def generateHtmlReport(relevantAlerts: List[ZapAlert], failureThreshold: String, spiderScanStatus: ScanStatus,
                         activeScanStatus: ScanStatus, missingScanners: List[Scanner], zapVersion: String): String = {
    report.html.index(relevantAlerts, failureThreshold, spiderScanStatus, activeScanStatus, missingScanners, zapVersion).toString()
  }

  def writeToFile(report: String): Unit = {
    val directory: File = new File("target/zap-reports")
    if (!directory.exists()) {
      directory.mkdir()
    }
    val file: File = new File(s"${directory.getAbsolutePath}/ZapReport.html")
    val writer = new BufferedWriter(new FileWriter(file))
    writer.write(report)
    writer.close()
    log.info(s"HTML Report generated: file://${file.getAbsolutePath}")
  }
}
