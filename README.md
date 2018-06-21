# zap-automation
This scala library provides an abstraction above the [ZAP](https://www.owasp.org/index.php/OWASP_Zed_Attack_Proxy_Project) API which allows for simple configurable execution of spider and active scans.  The zap-automation library also produces a report summarising the alerts captured during scans, and can be tuned to fail your test run depending on the severity of the vulnerabilities found.


## Capturing traffic with OWASP ZAP
For Zap to check security vulnerabilities of your application, it needs to know the various endpoints and flow of the application.  This can be achieved by using Zap as a proxying tool. When the Application Under Test uses Zap to proxy its requests, Zap performs a non invasive passive scan checking for vulnerabilities.

### 1. Start ZAP with a named session
You can start zap with the following command:
`<path-to-zap-installation>/zap -daemon -config api.disablekey=true -port 11000`

If you would like to preseve a zap session as a baseline to launch attacks against your application, then add the `-dir` and `-newsession` command line options:
`<path-to-zap-installation>/zap -daemon -config api.disablekey=true -port 11000 -dir <path-to-session> -newsession <session-name> `

### 2. Configure your tests to proxy via ZAP
You will need to configure WebDriver to proxy via Zap in your test like so:
   ```scala
       val profile: FirefoxProfile = new FirefoxPrfile
       profile.setAcceptUntrustedCertificates(true)
       profile.setPreference("network.proxy.type", 1)
       profile.setPreference("network.proxy.http", "localhost")
       profile.setPreference("network.proxy.http_port", 11000)
       profile.setPreference("network.proxy.share_proxy_settings", true)
       profile.setPreference("network.proxy.no_proxies_on", "")
       
       var options = new ChromeOptions()
       options.addArguments("test-type")
       options.addArguments("--proxy-server=http://localhost:11000")
       
//       TODO: Add proxy details for API
   ``` 
Now run your WebDriver tests as you would normally.

## Using this library
### 1. In your SBT build add
```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "zap-automation" % "x.x.x"
```

### 2. Create the zap-automation configuration
In your test suite's application.conf create a zap-config object as shown in the example [here](examples/singleConfigExample/resources/singleConfigExampleApplication.conf). If you have multiple 
security tests as part of the same suite and use different config for each of them, then refer to this example [here](examples/multipleConfigExample/resources/multipleConfigExampleApplication.conf).
  
### 3. Create your Test
Create a runner in your test suite by extending the ZapTest trait of the zap-automation library. The runner is required 
to extend a testSuite. This can be done by extending any of ScalaTest's [testing styles](http://www.scalatest.org/user_guide/selecting_a_style). 
The Zap runner is also expected to override the ZapConfiguration and call the triggerZapScan() method to trigger the scan.
A detailed example is available [here](examples/singleConfigExample/SingleConfigExampleRunner.scala).

### 4. Start OWASP ZAP referencing a zap session
If the ZAP instance that you proxied your test traffic through is still running, you can skip this step.  If you've previously saved a named ZAP session and would like to start a fresh instance of ZAP referencing that session, use the following command:

`<path-to-zap-installation>/zap.sh -daemon -config api.disablekey=true -port 11000 -dir <path-to-session> -session <session-name>`

### 5. Execute the test
* Run your acceptance tests pointing at your new ZAP browser profile

Note: You need to make sure you run enough UI tests to hit all the urls that you want to run your ZAP tests on. This may be all of your tests or a subset, it’s up to you.

Once the acceptance tests are completed, run the penetration tests (using your new ZapRunner file). If the ZapRunner is under
utils.Support package, then the command to run the Zap tests will looks like this:
```sbt "testOnly utils.Support.ZapRunner"```



## How do we read the output of the tests?
An html report is created at /target/zap-reports/ZapReport.html irrespective of the test outcome. Even when there are no alerts found, a report is generated indicating the summary of scans ran and the failureThreshold set. If you are surprised about getting a green build, it may be that you need to adjust the variables you are passing to the library, or you may not have run enough UI tests proxying through ZAP. 

| Key        | Description           | 
| ------------- |:-------------| 
| Low (Medium)  | Low is the Risk Code  and Medium is the Confidence Level. Risk Code is the risk of each type of vulnerability found. Confidence represents ZAP's "sureness" about the finding.| 
| URL      | The Url in which the alert was identified      |  
| Scanner ID | Id of the scanner. The passive and active scanners for your zap installation can be found at http://localhost:11000/HTML/pscan/view/scanners/ and http://localhost:11000/HTML/ascan/view/scanners/       |   
| CWE Id| [Common Weakness Enumeration (CWE™) Id](https://cwe.mitre.org/about/faq.html).      |   
| Method| HTTP method      |   
| Parameter| Parameter used for the test      |   
| Evidence| Evidence for the alert      |   
| Description| Description of the alert      |   
| Solution| Solution for the alert      |   
| Reference(s)| Future use      |   
| Internal References(s)| Future use      |   

## Supported browsers
We have tested the library using Chrome and Firefox.

## Development
### Run the unit tests for the library
```scala
sbt test
```

### Debugging
The library provides various debug flags for testing locally .....

### Issues
Please raise issues/feedback here

### License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    
