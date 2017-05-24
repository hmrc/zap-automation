package uk.gov.hmrc.utils

import com.sun.jersey.api.client.{Client, ClientResponse}
import org.json4s.jackson.Serialization._
import javax.ws.rs.core.MediaType
import javax.net.ssl.{HostnameVerifier, SSLContext, SSLSession, X509TrustManager}

import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.client.urlconnection.HTTPSProperties
import java.security.cert.X509Certificate

import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter

class InsecureClient {

  implicit val defaultFormats = json.JsonExtraction.formats

  private val jsonContentType = MediaType.APPLICATION_JSON + ";charset=utf-8"

  private val sslContext = SSLContext.getInstance("SSL")
  sslContext.init(null, Array(new InsecureTrustManager), null)

  private val config = new DefaultClientConfig()

  config.getProperties.put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
    new HostnameVerifier() {
      override def verify(s: String, sslSession: SSLSession): Boolean = true
    },
    sslContext
  ))


  private val client = Client.create(config)
  client.addFilter(new GZIPContentEncodingFilter(false))

  def get[A](url: String)(implicit m: Manifest[A]): A = {
    val response = client.resource(url).accept(jsonContentType).get[ClientResponse](classOf[ClientResponse])
    val responseBody = response.getEntity(classOf[String])
    println(s"TARGET = $url")
    response.getStatus match {
      case 200 => read[A](responseBody)
      case _ => throw new RuntimeException(s"Unexpected response from $url, status code ${response.getStatus} : $responseBody")
    }
  }

  def getRawResponse(url: String, contentType: String = jsonContentType)(implicit m: Manifest[String]): (Int, String) = {
    val response = client.resource(url).accept(contentType).get[ClientResponse](classOf[ClientResponse])
    val responseBody = response.getEntity(classOf[String])
    val status = response.getStatus
    (status, responseBody)
  }
}

class InsecureTrustManager extends X509TrustManager {
  def checkClientTrusted(p1: Array[X509Certificate], p2: String) {}

  def checkServerTrusted(p1: Array[X509Certificate], p2: String) {}

  def getAcceptedIssuers: Array[X509Certificate] = Array()
}

