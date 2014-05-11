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
import rapture.core._
import rapture.io._
import rapture.uri._

import scala.collection.mutable.HashMap
import java.text.SimpleDateFormat
import java.util.Locale

case class Cookie[I, D](domain: String, name: String, value: String,
    path: SimplePath, expiry: Option[I], secure: Boolean)(implicit ts: TimeSystem[I, D]) {
  lazy val pathString = path.toString
}

class Browser[I: TimeSystem.ByInstant]() {
  val browserString = "Rapture Net Browser 0.9.0"
  private val Rfc1036Pattern = "EEE, dd-MMM-yyyy HH:mm:ss zzz"

  val ts = ?[TimeSystem.ByInstant[I]]

  val cookies: HashMap[(String, String, SimplePath), Cookie[I, _]] =
    new HashMap[(String, String, SimplePath), Cookie[I, _]]

  def parseCookie(s: String, domain: String): Cookie[I, _] = {
    val ps = s.split(";").map(_.trim.split("=")) map { a =>
      a(0) -> (if(a.length > 1) a(1).urlDecode else "")
    }
    val details = ps.tail.toMap

    Cookie(details.get("domain").getOrElse(domain), ps.head._1, ps.head._2,
      SimplePath.parse(details.get("path").getOrElse("")),
      details.get("expires") map { exp =>
        ts.instant(new SimpleDateFormat(Rfc1036Pattern, Locale.US).parse(exp).getTime)
      }, details.contains("secure"))
  }

  def domainCookies(domain: String, secure: Boolean, path: String): String = {
    val now = System.currentTimeMillis
    cookies foreach { c =>
      if(c._2.expiry.map(e => ts.fromInstant(e) < now).getOrElse(false))
        cookies.remove((c._2.domain, c._2.name, c._2.path))
    }

    cookies.toList.filter(secure || !_._2.secure).filter(domain endsWith
        _._2.domain).filter(path startsWith _._2.pathString).map(_._2).groupBy(_.name) map { c =>
        c._1+"="+c._2.maxBy(_.pathString.length).value.urlEncode } mkString "; "
  }

  def accept[I, D](c: Cookie[I, D]): Boolean = c.domain.split("\\.").length > 1

  class BrowserUrl(url: HttpUrl) {

    protected implicit val errorHandler = raw

    def get[I, D](
      timeout: D = null,
      authenticate: Option[(String, String)] = None)
    (implicit ts: TimeSystem[I, D]) = url.get(timeout, authenticate)
    
    def post[C: PostType, D](
      content: C, timeout: D = null,
      authenticate: Option[(String, String)] = None,
      httpHeaders: Map[String, String] = Map())
    (implicit mode: Mode[IoMethods], ts: TimeSystem[I, D]): mode.Wrap[HttpResponse, HttpExceptions] =
      mode.wrap {
        
        var u = url
        var retries = 0
        var response: HttpResponse = null

        do {
          response = u.post[C, D](content, timeout, authenticate, true,
              httpHeaders + ("Cookie" -> domainCookies(u.hostname, u.ssl, u.pathString)),
              followRedirects = false)

          val newCookies = response.headers.get("Set-Cookie").getOrElse(Nil) map { c =>
            parseCookie(c, u.hostname)
          } filter { c: Cookie[I, _] => accept(c) }
        
          for(c <- newCookies) cookies((c.domain, c.name, c.path)) = c

          if(response.status/100 == 3) {
            retries += 1
            if(retries > 5) throw TooManyRedirects()
            val dest = response.headers("Location").headOption.getOrElse(throw BadHttpResponse())

            u = if(dest.startsWith("http")) Http.parse(dest)
                else if(dest.startsWith("/")) Http / u.hostname / SimplePath.parse(dest)
                // FIXME: This doesn't handle ascent in relative paths
                else u / SimplePath.parse(dest)
          }
        } while(response.status/100 == 3)
        
        response
    }
  }

  def apply(url: HttpUrl): BrowserUrl = new BrowserUrl(url)
}

