/**********************************************************************************************\
* Rapture Net Library                                                                          *
* Version 0.9.0                                                                                *
*                                                                                              *
* The primary distribution site is                                                             *
*                                                                                              *
*   http://rapture.io/                                                                         *
*                                                                                              *
* Copyright 2010-2013 Propensive Ltd.                                                          *
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
package rapture
import rapture.io._
import rapture.core._
import rapture.json._

import java.io._

package object net {
  
  type TcpService = Services.Tcp.Item
  implicit object HttpQueryParametersMap extends HttpQueryParametersBase[(Symbol, String), Map[Symbol, String]]

  implicit object HttpQueryParametersIter extends HttpQueryParametersBase[(Symbol, String), Seq[(Symbol, String)]]

  implicit object PageIdentifier extends QueryType[Path[_], Symbol] {
    def extras(existing: AfterPath, q: Symbol): AfterPath =
      existing + ('#' -> (q.name -> 2.0))
  }

  implicit object HttpUrlLinkable extends Linkable[HttpUrl, HttpUrl] {
    type Result = Link
    def link(src: HttpUrl, dest: HttpUrl) = {
      if(src.ssl == dest.ssl && src.hostname == dest.hostname && src.port == dest.port) {
        val lnk = generalLink(src.elements.to[List], dest.elements.to[List])
        new RelativePath(lnk._1, lnk._2, dest.afterPath)
      } else dest
    }
  }

  implicit val FormPostType = new PostType[Map[Symbol, String]] {
    def contentType = Some(MimeTypes.`application/x-www-form-urlencoded`)
    def sender(content: Map[Symbol, String]) = ByteArrayInput((content map { case (k, v) =>
      java.net.URLEncoder.encode(k.name, "UTF-8")+"="+java.net.URLEncoder.encode(v, "UTF-8")
    } mkString "&").getBytes("UTF-8"))
  }

  implicit val StringPostType = new PostType[String] {
    def contentType = Some(MimeTypes.`text/plain`)
    def sender(content: String) = ByteArrayInput(content.getBytes("UTF-8"))
  }

  implicit val NonePostType = new PostType[None.type] {
    def contentType = Some(MimeTypes.`application/x-www-form-urlencoded`)
    def sender(content: None.type) = ByteArrayInput(Array[Byte](0))
  }

  implicit val JsonPostType = new PostType[Json] {
    def contentType = Some(MimeTypes.`application/x-www-form-urlencoded`)
    def sender(content: Json) =
      ByteArrayInput(content.toString.getBytes("UTF-8"))
  }
  
  /** Type class object for reading `Byte`s from `HttpUrl`s */
  implicit object HttpStreamByteReader extends JavaInputStreamReader[HttpUrl](
      _.javaConnection.getInputStream)

  implicit object HttpResponseCharReader extends StreamReader[HttpResponse, Char] {
    def input(response: HttpResponse)(implicit eh: ExceptionHandler): eh.![Exception, Input[Char]] =
      eh.except {
        implicit val enc = Encodings.`UTF-8`
        implicit val errorHandler = raw
        response.input[Char]
      }
  }

  implicit object HttpResponseByteReader extends StreamReader[HttpResponse, Byte] {
    def input(response: HttpResponse)(implicit eh: ExceptionHandler): eh.![Exception, Input[Byte]] =
      eh.except(response.input[Byte](implicitly[InputBuilder[InputStream, Byte]], raw))
  }

  implicit object SocketStreamByteReader extends
      JavaInputStreamReader[SocketUri](_.javaSocket.getInputStream)

  implicit object SocketStreamByteWriter extends
      JavaOutputStreamWriter[SocketUri](_.javaSocket.getOutputStream)
}

