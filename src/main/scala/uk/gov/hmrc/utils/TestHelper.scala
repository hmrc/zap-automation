package uk.gov.hmrc.utils

class TestHelper

object TestHelper {

  def waitForCondition(condition: => Boolean, exceptionMessage: String, timeoutInSeconds: Int = 5) {
    val endTime = System.currentTimeMillis + timeoutInSeconds * 1000
    while (System.currentTimeMillis < endTime) {
      if (condition) {
        return
      }
      FixedDelay(100)
    }
    throw new Exception(exceptionMessage)
  }

}
