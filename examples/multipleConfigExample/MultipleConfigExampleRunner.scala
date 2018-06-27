package uk.gov.service.tax.ddcnls

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.WordSpec
import uk.gov.hmrc.zap.ZapTest
import uk.gov.hmrc.zap.config.ZapConfiguration

class MultipleConfigExampleRunner extends WordSpec with ZapTest {

  val defaultConfig: Config =  ConfigFactory.load()

  val customConfig: Config = defaultConfig.getConfig("paye")
    .withFallback(defaultConfig.getConfig("zap-automation-config"))

  override val zapConfiguration: ZapConfiguration = new ZapConfiguration(customConfig)

  "Kicking off the zap scan" should {
    "should complete successfully" in {
      triggerZapScan()
    }
  }
}