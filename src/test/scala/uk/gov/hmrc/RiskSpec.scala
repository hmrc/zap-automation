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

import org.scalatest.{FunSpec, Matchers}

class RiskSpec extends FunSpec with Matchers{

  describe("risk ordering"){
    it("should have low as smallest"){
      val smaller = Risk.toRisk("low") < Risk.MEDIUM
      smaller shouldBe true
    }

    it("should ignore risks on the same level"){
      val ignored = Risk.toRisk("low") < Risk.LOW
      ignored shouldBe false
    }

  }

}
