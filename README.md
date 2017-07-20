**zap-automation**


This is a library utilising the [ZAP](https://www.owasp.org/index.php/OWASP_Zed_Attack_Proxy_Project) API, 
with pre configured steps to run a spider attack and then an active scan.


### Run the unit tests for the library
```scala
sbt test
```

### Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "zap-automation" % "x.x.x"
```

### How to use the library

* You will likely want to have a way to run some tests from the UI of the service/application you are testing, so that ZAP can learn about the URLs it needs to test.
* You will also need to install and setup ZAP either locally, or on your build machine in order to use this library.
* You will need a way to run the library, we have done this by using this file:

```scala
package utils.Support

import uk.gov.hmrc.{ZapAlertFilter, ZapTest}

class ZapRunner extends ZapTest{

  /**
    * zapBaseUrl is a required field - you'll need to set it in this file, for your project to compile.
    * It will rarely need to be changed. We've included it as an overridable field
    * for flexibility and just in case.
    */
  override val zapBaseUrl: String = "xxx"

  /**
    * testUrl is a required field - you'll need to set it in this file, for your project to compile.
    * It needs to be the URL of the start page of your application (not just localhost:port).
    */
  override val testUrl: String = "xxx"

  /**
    * alertsBaseUrl is not a required field. This is the url that the zap-automation library
    * will use to filter out the alerts that are shown to you. Note that while Zap is doing
    * testing, it is likely to find alerts from other services that you don't own - for example
    * from logging in, therefore we recommend that you set this to be the base url for the
    * service you are interested in.
    */
  override val alertsBaseUrl: String = "xxx"

  /**
    * contextBaseUrl is not a required field. This url is added as the base url to your
    * context.
    * A context is a construct in Zap that limits the scope of any attacks run to a
    * particular domain (this doesn't mean that Zap won't find alerts on other services during the
    * browser test run).
    * This would usually be the base url of your service - eg http://localhost:xxxx.*
    */
  override val contextBaseUrl: String = "xxx.*"

  /**
    * desiredTechnologyNames is not a required field. We recommend you don't change this
    * field, as we've made basic choices for the platform. We made it overridable just in case
    * your service differs from the standards of the Platform.
    *
    * The technologies that you put here will limit the amount of checks that ZAP will do to
    * just the technologies that are relevant. The default technologies are set to
    * "OS,OS.Linux,Language,Language.Xml,SCM,SCM.Git".
    */
  //override val desiredTechnologyNames: String = ""
  
    /**
    * routesToBeIgnoredFromContext is not a required field. You may set this if you have any routes
    * that are part of your application, but you do not want tested. For example, if you had any
    * test-only routes, you could force Zap not to test them by adding them in here as a regex.
    */
  //override val routeToBeIgnoredFromContext: String = "xxx"

  /**
    * If, when you run the Zap tests, you find alerts that you have investigated and don't see as a problem
    * you can filter them out using this code, on the cweid and the url that the alert was found on.
    * The CWE ID is a Common Weakness Enumeration (http://cwe.mitre.org/data/index.html), you can
    * find this by looking at the alert output from your tests.
    * As part of the trial, please try
    * filtering out a few alerts and seeing if this functionality works for you.
    * Below you can see an example of how this might work.
    */
  val alertToBeIgnored1: ZapAlertFilter = ZapAlertFilter(cweid = "16", url = "xxx")
  override val alertsToIgnore: List[ZapAlertFilter] = List(alertToBeIgnored1)

}
```

* You’ll need to be able to create a new browser profile for ZAP, and switch your browser to this new profile. We’ve done this using this code:

```scala
  def createZapDriver: WebDriver = {
    val profile: FirefoxProfile = new FirefoxProfile
    profile.setAcceptUntrustedCertificates(true)
    profile.setPreference("network.proxy.type", 1)
    profile.setPreference("network.proxy.http", "localhost")
    profile.setPreference("network.proxy.http_port", 11000)
    profile.setPreference("network.proxy.share_proxy_settings", true)
    profile.setPreference("network.proxy.no_proxies_on", "")
    val options: FirefoxOptions = new FirefoxOptions
    options.setLegacy(true)
    val firefoxCapabilities: DesiredCapabilities = new DesiredCapabilities()
    firefoxCapabilities.setCapability(FirefoxDriver.PROFILE, profile)
    firefoxCapabilities.setCapability("marionette", false)
    options.addDesiredCapabilities(firefoxCapabilities)
    val capabilities = options.toDesiredCapabilities
    println("Running ZAP Firefox Driver")
    new FirefoxDriver(capabilities)
  }
  
    def createZapChromeDriver: WebDriver = {
    var options = new ChromeOptions()
    var capabilities = DesiredCapabilities.chrome()
    options.addArguments("test-type")
    options.addArguments("--proxy-server=http://localhost:11000")
    capabilities.setCapability(ChromeOptions.CAPABILITY, options)
    val driver = new ChromeDriver(capabilities)
    val caps = driver.getCapabilities
    val browserName = caps.getBrowserName
    val browserVersion = caps.getVersion
    println( "Browser name & version: "+ browserName+" "+browserVersion)
    driver
  }
```


### Run the ZAP tests on your machine

* Start your application locally
* Start ZAP from the command line:
* * Change directory to where ZAP is installed (default Mac installation is in the root Applications directory: /Applications)
* * Run this command: ZAP\ 2.6.0.app/Contents/Java/zap.sh -daemon -config api.disablekey=true -port 11000
* Run your acceptance tests pointing at your new ZAP profile. Our command to do this looks like this:
```sbt -Dbrowser=zap -Denvironment=Local ‘test-only Suites.RunZapTests’```


You need to make sure you run enough UI tests to hit all the urls that you want to run your ZAP tests on. This may be all of your tests or a subset, it’s up to you.
Run the penetration tests (using your new ZapRunner file) - our command to do this looks like this:
```sbt "testOnly utils.Support.ZapRunner"```

### How do we read the output of the tests?
Green build - no html report is created as there are no alerts to give you more information about. If you are surprised about getting a green build, if may be that you need to adjust the variables you are passing to the library, or you may not have run enough UI tests proxying through ZAP. If you have doubts please contact us. 
Red build - alerts are printed in the console and a html report is created on the workspace. Note that the report will be deleted each time a new build is started.


### Supported browsers
We have tested the library using Chrome and Firefox.


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    