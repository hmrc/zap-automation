package uk.gov.hmrc

import org.json4s.JsonAST.JObject
import org.json4s.native.JsonParser
import org.scalatest.WordSpec
import uk.gov.hmrc.utils.{InsecureClient, TestHelper}

trait ZapTest extends WordSpec {

  //List of unwanted alerts by "param" and number of alerts field should be overridden where ZAPTest is extended
  val unwantedAlerts: List[String] = List.empty
  val numberOfUnexpectedAlerts: Int = 0

  val ZAP_URL: String = System.getProperty("zapUrl", "http://localhost:11000")
  val TEST_URL: String = System.getProperty("testUrl", "http://localhost:9000")
  val FAILURE_RISK_LEVEL: String = System.getProperty("failureRiskLevel", "HIGH")

  val FAIL_LEVELS: List[String] = List("LOW", "MEDIUM", "HIGH")
  val theClient = new InsecureClient

  def hasCallCompleted(url : String) : Boolean = {
    val theJson = JsonParser.parse(theClient.getRawResponse(url)._2).asInstanceOf[JObject]
    val theStatus = theJson.values("status").toString

    val theStatusProgress = s"$theStatus%"

    if(url contains "spider") {
      print(s"Spider test is $theStatusProgress complete\r")
    } else {
      print(s"Active Scan is $theStatusProgress complete\r")
    }

    if(theStatus == "100") true else false
  }


  "Zap" should {


    "not find any alerts of the specified risk level" in {

      //Start the spidering of the SA application
      theClient.getRawResponse(ZAP_URL + s"/JSON/spider/action/scan/?zapapiformat=JSON&url=$TEST_URL")

      //Check the status of the spider
      TestHelper.waitForCondition(hasCallCompleted(ZAP_URL + "/JSON/spider/view/status/?zapapiformat=JSON"), "Spider Timed Out", timeoutInSeconds = 600)

      //Start the active scan of the SA application
      theClient.getRawResponse(ZAP_URL + s"/JSON/ascan/action/scan/?zapapiformat=JSON&url=$TEST_URL&recurse=&inScopeOnly=")

      //Check status of the active scan
      TestHelper.waitForCondition(hasCallCompleted(ZAP_URL + "/JSON/ascan/view/status/?zapapiformat=JSON"), "Active Scanner Timed Out", timeoutInSeconds = 1800)

      //Get the alerts
      val theAlerts = theClient.get[ZapAlerts](ZAP_URL + s"/JSON/core/view/alerts/?zapapiformat=JSON&baseurl=$TEST_URL&start=&count=")
      //Filter for specified risk level
      val highAlerts: List[ZapAlert] = theAlerts.alerts.filter(alert => FAIL_LEVELS.indexOf(alert.risk.toUpperCase) >= FAIL_LEVELS.indexOf(FAILURE_RISK_LEVEL))
      //Filter out ignored alerts
      val activeAlerts: List[ZapAlert] = for(halert <-highAlerts if !unwantedAlerts.contains(halert.param)) yield halert
      //Check the filter removed correct number of alerts
      if(highAlerts.size - activeAlerts.size != numberOfUnexpectedAlerts) {
        throw new Exception(s"Number of ignored alerts was not equal to $numberOfUnexpectedAlerts")
      }

      //Display alerts - To add: If number of ignored alerts is wrong, then display all alerts.
      if(activeAlerts.nonEmpty){
        var color = Console.RESET

        var noOfAlerts: Int = 0
        //Output errors
        activeAlerts.foreach{ a : ZapAlert=>
          println("###############################")
          a.risk.toUpperCase match {
            case "HIGH" => color = Console.RED
            case "MEDIUM" => color = Console.YELLOW
            case _ => color = Console.RESET
          }
          println(color + s"url: ${a.url}")
          println(s"param: ${a.param}")
          println(s"alert: ${a.alert}")
          println(s"risk: ${a.risk}")
          println(s"description: ${a.description}")
          println(s"evidence: ${a.evidence}")
          println(s"reference: ${a.reference}")
          println(s"solution: ${a.solution}")
          println(Console.RESET + "###############################")
          noOfAlerts = noOfAlerts + 1

        }

        println(Console.BOLD + s"Number of security alerts: $noOfAlerts")
        //Fail Test
        fail(s"Zap found $FAILURE_RISK_LEVEL or higher risk ZAP alerts")
      }
    }
  }

}

case class ZapAlerts(alerts: List[ZapAlert])
case class ZapAlert(other: String, evidence: String, pluginId: String, cweid: String, confidence: String, wascid: String, description: String, messageId: String, url: String, reference: String, solution: String, alert: String, param: String, attack: String, name: String, risk: String, id: String)
