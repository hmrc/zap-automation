/*
 * Copyright 2020 HM Revenue & Customs
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

import scala.concurrent.Await
import scala.concurrent.duration._


trait HttpClient {

  def get(zapBaseUrl: String, queryPath: String, params: (String, String)*): (Int, String)

}

object WsClient extends HttpClient {

  def asyncClient: StandaloneAhcWSClient = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    StandaloneAhcWSClient()
  }

  def get(zapBaseUrl: String, queryPath: String, params: (String, String)*): (Int, String) = {

    val url = s"$zapBaseUrl$queryPath"
    val client = asyncClient
    val response = Await.result(client.url(s"$url")
      .withHttpHeaders("ContentType" -> "application/json;charset=utf-8")
      .withQueryStringParameters(params: _*)
      .get(), 60 seconds)

    client.close()

    (response.status, response.body)
  }
}