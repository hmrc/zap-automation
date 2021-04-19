/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.zap.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.exception.RemotelyClosedException
import uk.gov.hmrc.zap.ZapException

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal

trait HttpClient {

  def get(zapBaseUrl: String, queryPath: String, params: (String, String)*): (Int, String)

}

object WsClient extends HttpClient {

  def asyncClient: StandaloneAhcWSClient = {
    implicit val system: ActorSystem             = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    StandaloneAhcWSClient()
  }

  def get(zapBaseUrl: String, queryPath: String, params: (String, String)*): (Int, String) = {

    val url      = s"$zapBaseUrl$queryPath"
    val client   = asyncClient
    val response =
      try Await.result(
        client
          .url(s"$url")
          .withHttpHeaders("ContentType" -> "application/json;charset=utf-8")
          .withQueryStringParameters(params: _*)
          .get(),
        60 seconds
      )
      catch {
        case ex: java.net.ConnectException =>
          throw ZapException(
            s"Request to endpoint: $url failed with exception: ${ex.getMessage}. Check if the service is running."
          )
        case ex: RemotelyClosedException   =>
          throw ZapException(
            s"Request to endpoint: $url failed with exception: ${ex.getMessage}. Check if the service can handle requests."
          )
        case NonFatal(ex)                  =>
          throw ZapException(s"Request to endpoint: $url failed with exception: ${ex.getMessage}.")
      }
    client.close()
    (response.status, response.body)
  }
}
