# zap-automation
This scala library is built for use in a [Scalatest](http://www.scalatest.org/) Suite, and provides an abstraction above the [OWASP ZAP API](https://www.owasp.org/index.php/OWASP_Zed_Attack_Proxy_Project) which allows for simple configurable execution of spider and active scans. The zap-automation library also produces a report summarising the alerts captured during scans, and can be tuned to fail your test run depending on the severity of the vulnerabilities found.

## Configuring a test to use zap-automation
The below step-by-step guide assumes a running OWASP ZAP instance has already captured traffic with which to launch at attack scan.  If you don't have this, the following pages might help:
- [Starting OWASP ZAP](wiki/WIP:-Managing-ZAP-Sessions-from-the-command-line)
- [Proxying your WebDriver Tests through ZAP](wiki/WIP:-Managing-ZAP-Sessions-from-the-command-line)

### 1. Update your sbt build
In you `build.sbt` file, add the following: 

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "zap-automation" % "x.x.x"
```

### 2. Create the zap-automation configuration
In your test suite's `application.conf` create a `zap-automation-config` configuration object.  See the [default configuration](src/main/resources/reference.conf) file for detail on each configuration option. 

An simple example can be found [here](examples/singleConfigExample/resources/singleConfigExampleApplication.conf). 

You can also make use of the functionality available via [typesafe configuration](https://github.com/lightbend/config) if you would like to implement multiple security tests as part of the same test suite.  Example configuration [here](examples/multipleConfigExample/resources/multipleConfigExampleApplication.conf).
  
### 3. Create your Test
Create a test run in your test suite by extending the ZapTest trait of the zap-automation library. The test **must** extend one of ScalaTest's [testing styles](http://www.scalatest.org/user_guide/selecting_a_style). 

The test **must** also override [ZapConfiguration](src/main/scala/uk/gov/hmrc/zap/config/ZapConfiguration.scala) and call the triggerZapScan() method.

A detailed example is available [here](examples/singleConfigExample/SingleConfigExampleRunner.scala) for reference.

### 4. Execute the test
Execute the attack scans with sbt using test created in the previous test.  For example, if you created a test named `utils.support.ZapRunner` then the following command should work:

```sbt "testOnly utils.Support.ZapRunner"```

The output of a successful run will look something like this:
![successful run](/images/console-successful-run.png)

## How do we read the output of the tests?
A HTML report is created at `target/zap-reports/ZapReport.html` irrespective of whether or not vulnerabilities were found.  

The report contains the following sections:
- **Summary of Alerts**: a summary of the vulnerabilities found
- **Summary of Scans**: which of the scans executed (passive/spider/active)
- **Failure Threshold**: the configured failure threshold
- **Alert Details**: detail on each vulnerability/alert recorded 

Alert detail description:


| Key | Description | 
| --- | --- | 
| Low (Medium)  | Low is the Risk Code  and Medium is the Confidence Level. Risk Code is the risk of each type of vulnerability found. Confidence represents ZAP's "sureness" about the finding.| 
| URL      | The Url in which the alert was identified      |  
| Scanner ID | Id of the scanner. The passive and active scanners for your zap installation can be found at http://localhost:11000/HTML/pscan/view/scanners/ and http://localhost:11000/HTML/ascan/view/scanners/       |   
| CWE Id | [Common Weakness Enumeration (CWE™) Id](https://cwe.mitre.org/about/faq.html).      |   
| Method | HTTP method      |   
| Parameter | Parameter used for the test      |   
| Evidence | Evidence for the alert      |   
| Description | Description of the alert      |   
| Solution | Solution for the alert      |   
| Reference(s) | Future use      |   
| Internal References(s) | Future use      |   

## Development
### Run the unit tests for the library
```scala
sbt test
```

### Debugging
The library provides various debug flags for testing locally .....

### Issues
Please raise issues and feedback in this [github project](issues/)

## License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
 
