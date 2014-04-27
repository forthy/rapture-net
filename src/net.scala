/**********************************************************************************************\
* Rapture Net Library                                                                          *
* Version 0.9.0                                                                                *
*                                                                                              *
* The primary distribution site is                                                             *
*                                                                                              *
*   http://rapture.io/                                                                         *
*                                                                                              *
* Copyright 2010-2014 Jon Pretty, Propensive Ltd.                                              *
*                                                                                              *
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file    *
* except in compliance with the License. You may obtain a copy of the License at               *
*                                                                                              *
*   http://www.apache.org/licenses/LICENSE-2.0                                                 *
*                                                                                              *
* Unless required by applicable law or agreed to in writing, software distributed under the    *
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,    *
* either express or implied. See the License for the specific language governing permissions   *
* and limitations under the License.                                                           *
\**********************************************************************************************/
package rapture.net

import rapture.io._
import rapture.uri._
import rapture.mime._
import rapture.core._
import rapture.crypto._

import language.existentials

import java.io._
import java.net._

object HttpMethods {
  
  private val methods = new scala.collection.mutable.HashMap[String, Method]
  
  sealed class Method(val string: String) {
    
    def unapply(r: String) = r == string
    override def toString = string
    
    methods += string -> this
  }

  trait FormMethod { this: Method => }

  def method(s: String) = methods(s)

  val Get = new Method("GET") with FormMethod
  val Put = new Method("PUT")
  val Post = new Method("POST") with FormMethod
  val Delete = new Method("DELETE")
  val Trace = new Method("TRACE")
  val Options = new Method("OPTIONS")
  val Head = new Method("HEAD")
  val Connect = new Method("CONNECT")
  val Patch = new Method("PATCH")

}

class HttpResponse(val headers: Map[String, List[String]], val status: Int, is: InputStream) {
  def input[Data](implicit ib: InputBuilder[InputStream, Data], rts: Rts):
      rts.Wrap[Input[Data], Exception]=
    rts.wrap(ib.input(is)(raw))
}

trait PostType[-C] {
  def contentType: Option[MimeTypes.MimeType]
  def sender(content: C): Input[Byte]
}

/** Common methods for `HttpUrl`s */
trait NetUrl extends Url[NetUrl] with Uri {
  
  import javax.net.ssl._
  import javax.security.cert._
  
  private[rapture] def javaConnection: HttpURLConnection =
    new URL(toString).openConnection().asInstanceOf[HttpURLConnection]
  
  private val trustAllCertificates = {
    Array[TrustManager](new X509TrustManager {
      override def getAcceptedIssuers(): Array[java.security.cert.X509Certificate] = null
      
      def checkClientTrusted(certs: Array[java.security.cert.X509Certificate], authType: String):
          Unit = ()
      
      def checkServerTrusted(certs: Array[java.security.cert.X509Certificate], authType: String):
          Unit = ()
    })
  }
  
  private val sslContext = SSLContext.getInstance("SSL")
  sslContext.init(null, trustAllCertificates, new java.security.SecureRandom())

  private val allHostsValid = new HostnameVerifier {
    def verify(hostname: String, session: SSLSession) = true
  }

  def hostname: String
  def port: Int
  def ssl: Boolean
  def canonicalPort: Int

  private val base64 = new Base64Codec(endPadding = true)

  def schemeSpecificPart = "//"+hostname+(if(port == canonicalPort) "" else ":"+port)+pathString

  /** Sends an HTTP put to this URL.
    *
    * @param content the content to put to the URL
    * @param authenticate the username and password to provide for basic HTTP authentication,
    *        defaulting to no authentication.
    * @return the HTTP response from the remote host */
  def put[C: PostType, T](
    content: C,
    timeout: T = null,
    authenticate: Option[(String, String)] = None,
    ignoreInvalidCertificates: Boolean = false,
    httpHeaders: Map[String, String] = Map(),
    followRedirects: Boolean = true)
  (implicit rts: Rts, ts: TimeSystem[_, T]): rts.Wrap[HttpResponse, HttpExceptions] = {
    
    val timeoutValue = Option(timeout).getOrElse(?[TimeSystem[_, T]].duration(0L, 10000L))
    
    post(content, timeoutValue, authenticate, ignoreInvalidCertificates, httpHeaders, "PUT",
        followRedirects)(?[PostType[C]], rts, ts)
  }

  def head[T](timeout: T = null,
    authenticate: Option[(String, String)] = None,
    ignoreInvalidCertificates: Boolean = false,
    httpHeaders: Map[String, String] = Map(),
    followRedirects: Boolean = true)
  (implicit rts: Rts, ts: TimeSystem[_, T]): rts.Wrap[HttpResponse, HttpExceptions] = {
    val timeoutValue = Option(timeout).getOrElse(?[TimeSystem[_, T]].duration(0L, 10000L))
    
    post(None, timeoutValue, authenticate, ignoreInvalidCertificates, httpHeaders, "HEAD",
        followRedirects)(?[PostType[None.type]], rts, ts)
  }

  def get[T](
    timeout: T = null,
    authenticate: Option[(String, String)] = None,
    ignoreInvalidCertificates: Boolean = false,
    httpHeaders: Map[String, String] = Map(),
    followRedirects: Boolean = true)
  (implicit rts: Rts, ts: TimeSystem[_, T]): rts.Wrap[HttpResponse, HttpExceptions] = {

    val timeoutValue = Option(timeout).getOrElse(?[TimeSystem[_, T]].duration(0L, 10000L))
    
    post(None, timeoutValue, authenticate, ignoreInvalidCertificates, httpHeaders, "GET",
        followRedirects)(?[PostType[None.type]], rts, ts)
  }

  def size[T](timeout: T = null.asInstanceOf[T])(implicit rts: Rts, ts: TimeSystem[_, T]):
      rts.Wrap[Long, HttpExceptions] = rts.wrap {
    head(timeout)(raw, ts).headers.get("Content-Length").get.head.toLong
  }

  /** Sends an HTTP post to this URL.
    *
    * @param content the content to post to the URL
    * @param authenticate the username and password to provide for basic HTTP authentication,
    *        defaulting to no authentication.
    * @return the HTTP response from the remote host */
  def post[C: PostType, T](
    content: C,
    timeout: T = null,
    authenticate: Option[(String, String)] = None,
    ignoreInvalidCertificates: Boolean = false,
    httpHeaders: Map[String, String] = Map(),
    method: String = "POST",
    followRedirects: Boolean = true)
  (implicit rts: Rts, ts: TimeSystem[_, T]): rts.Wrap[HttpResponse, HttpExceptions] =
    rts wrap {
      val timeoutValue = Option(timeout).getOrElse(?[TimeSystem[_, T]].duration(0L, 10000L))
      implicit val errorHandler = raw

      // FIXME: This will produce a race condition if creating multiple URL connections with
      // different values for followRedirects in parallel
      HttpURLConnection.setFollowRedirects(followRedirects)
      val conn: URLConnection = new URL(toString).openConnection()
      conn.setConnectTimeout(implicitly[TimeSystem.ByDuration[T]].fromDuration(timeoutValue).toInt)
      conn match {
        case c: HttpsURLConnection =>
          if(ignoreInvalidCertificates) {
            c.setSSLSocketFactory(sslContext.getSocketFactory)
            c.setHostnameVerifier(allHostsValid)
          }
          c.setRequestMethod(method)
          if(content != None) c.setDoOutput(true)
          c.setUseCaches(false)
        case c: HttpURLConnection =>
          c.setRequestMethod(method)
          if(content != None) c.setDoOutput(true)
          c.setUseCaches(false)
      }

      if(authenticate.isDefined) conn.setRequestProperty("Authorization",
          "Basic "+base64.encode((authenticate.get._1+":"+
          authenticate.get._2).getBytes("UTF-8")).mkString)

      ?[PostType[C]].contentType map { ct =>
        conn.setRequestProperty("Content-Type", ct.name)
      }
      for((k, v) <- httpHeaders) conn.setRequestProperty(k, v)

      if(content != None)
        ensuring(OutputStreamBuilder.output(conn.getOutputStream)) { out =>
          ?[PostType[C]].sender(content) > out
        } (_.close())

      import scala.collection.JavaConversions._

      val statusCode = conn match {
        case c: HttpsURLConnection => c.getResponseCode()
        case c: HttpURLConnection => c.getResponseCode()
      }
      
      val is = try conn.getInputStream() catch {
        case e: IOException => conn match {
          case c: HttpsURLConnection => c.getErrorStream()
          case c: HttpURLConnection => c.getErrorStream()
        }
      }
      
      new HttpResponse(mapAsScalaMap(conn.getHeaderFields()).toMap.mapValues(_.to[List]),
          statusCode, is)
    }
}

class HttpQueryParametersBase[U, T <: Iterable[U]] extends QueryType[Path[_], T] {
  def extras(existing: AfterPath, q: T): AfterPath =
    existing + ('?' -> ((q.map({ case (k: Symbol, v: String) =>
      k.name.urlEncode+"="+v.urlEncode
    }).mkString("&")) -> 1.0))
}

/** Represets a URL with the http scheme */
class HttpUrl(val pathRoot: NetPathRoot[HttpUrl], elements: Seq[String], afterPath: AfterPath,
    val ssl: Boolean) extends Url[HttpUrl](elements, afterPath) with NetUrl with
    PathUrl[HttpUrl] { thisHttpUrl =>
  
  def makePath(ascent: Int, xs: Seq[String], afterPath: AfterPath) =
    new HttpUrl(pathRoot, elements, afterPath, ssl)
  
  def hostname = pathRoot.hostname
  def port = pathRoot.port
  def canonicalPort = if(ssl) 443 else 80

  override def equals(that: Any) = that match {
    case that: HttpUrl =>
      pathRoot == that.pathRoot && ssl == that.ssl && afterPath == that.afterPath &&
          elements == that.elements
    case _ => false
  }

  override def hashCode =
    pathRoot.hashCode ^ elements.to[List].hashCode ^ afterPath.hashCode ^ ssl.hashCode
}

trait NetPathRoot[+T <: Url[T] with NetUrl] extends PathRoot[T] {
  def hostname: String
  def port: Int

  override def equals(that: Any) = that match {
    case that: NetPathRoot[_] => hostname == that.hostname && port == that.port
    case _ => false
  }

  override def hashCode = hostname.hashCode ^ port
}

class HttpPathRoot(val hostname: String, val port: Int, val ssl: Boolean) extends
    NetPathRoot[HttpUrl] with Uri { thisPathRoot =>
  
  def makePath(ascent: Int, elements: Seq[String], afterPath: AfterPath): HttpUrl =
    new HttpUrl(thisPathRoot, elements, Map(), ssl)

  def scheme = if(ssl) Https else Http
  def canonicalPort = if(ssl) 443 else 80
  def schemeSpecificPart = "//"+hostname+(if(port == canonicalPort) "" else ":"+port)+pathString

  override def /(element: String) = makePath(0, Array(element), Map())
  
  def /[P <: Path[P]](path: P) = makePath(0, path.elements, Map())
  
  override def equals(that: Any): Boolean =
    that.isInstanceOf[HttpPathRoot] && hostname == that.asInstanceOf[HttpPathRoot].hostname
}

/** Factory for creating new HTTP URLs */
object Http extends Scheme[HttpUrl] {
  def schemeName = "http"

  /** Creates a new URL with the http scheme with the specified domain name and port
    *
    * @param hostname A `String` of the domain name for the URL
    * @param port The port to connect to this URL on, defaulting to port 80 */
  def /(hostname: String, port: Int = Services.Tcp.http.portNo) =
    new HttpPathRoot(hostname, port, false)

  private val UrlRegex =
    """(https?):\/\/([\.\-a-z0-9]+)(:[1-9][0-9]*)?(\/?([^\?]*)(\?([^\?]*))?)""".r

  /** Parses a URL string into an HttpUrl */
  def parse(s: String)(implicit rts: Rts): rts.Wrap[HttpUrl, ParseException] =
      rts.wrap { s match {
    case UrlRegex(scheme, server, port, _, path, _, after) =>
      val rp = new SimplePath(path.split("/"), Map())
      val afterPath = after match {
        case null | "" => Map[Symbol, String]()
        case after => after.split("&").map { p => p.split("=", 2) match {
          case Array(k, v) => Symbol(k) -> v
        } }.toMap
      }
      val most = scheme match {
        case "http" =>
          Http./(server, if(port == null) 80 else port.substring(1).toInt) / rp
        case "https" =>
          Https./(server, if(port == null) 443 else port.substring(1).toInt) / rp
        case _ => throw ParseException(s)
      }
      if(afterPath.isEmpty) most else most.query(afterPath)
    case _ => throw ParseException(s)
  } }
}

/** Factory for creating new HTTPS URLs */
object Https extends Scheme[HttpUrl] {
  def schemeName = "https"

  /** Creates a new URL with the https scheme with the specified domain name and port
    *
    * @param hostname A `String` of the domain name for the URL
    * @param port The port to connect to this URL on, defaulting to port 443 */
  def /(hostname: String, port: Int = Services.Tcp.https.portNo) =
    new HttpPathRoot(hostname, port, true)
  
  def parse(s: String)(implicit rts: Rts): rts.Wrap[HttpUrl, ParseException] =
    Http.parse(s)(rts)
}
