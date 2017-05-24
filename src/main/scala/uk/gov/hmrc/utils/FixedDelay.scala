package uk.gov.hmrc.utils

object FixedDelay {

  def apply(millis: Long): Unit = concurrent.blocking(Thread.sleep(millis))

}