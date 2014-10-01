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
package rapture
import rapture.io._
import rapture.uri._
import rapture.mime._
import rapture.core._
import rapture.codec._

import java.io.{Reader => _, Writer => _, _}

package object net {

  type TcpService = Services.Tcp.Item
  
  implicit val httpQueryParametersMap: HttpQueryParametersBase[(Symbol, String), Map[Symbol,
      String]] = new HttpQueryParametersBase[(Symbol, String), Map[Symbol, String]]

  implicit val HttpQueryParametersIter: HttpQueryParametersBase[(Symbol, String), Seq[(Symbol,
      String)]] = new HttpQueryParametersBase[(Symbol, String), Seq[(Symbol, String)]]

  implicit val pageIdentifier: QueryType[Path[_], Symbol] = new QueryType[Path[_], Symbol] {
    def extras(existing: AfterPath, q: Symbol): AfterPath =
      existing + ('#' -> (q.name -> 2.0))
  }

  implicit val httpUrlSizable: Sizable[HttpUrl, Byte] = new Sizable[HttpUrl, Byte] {
    type ExceptionType = HttpExceptions
    def size(url: HttpUrl): Long = {
      implicit val ts = timeSystems.numeric

      url.head(10000L).headers.get("Content-Length").get.head.toLong
    }
      
  }

  implicit val httpUrlLinkable: Linkable[HttpUrl, HttpUrl] = new Linkable[HttpUrl, HttpUrl] {
    type Result = Link
    def link(src: HttpUrl, dest: HttpUrl) = {
      if(src.ssl == dest.ssl && src.hostname == dest.hostname && src.port == dest.port) {
        val lnk = generalLink(src.elements.to[List], dest.elements.to[List])
        new RelativePath(lnk._1, lnk._2, dest.afterPath)
      } else dest
    }
  }

  implicit val formPostType: PostType[Map[Symbol, String]] = new PostType[Map[Symbol, String]] {
    def contentType = Some(MimeTypes.`application/x-www-form-urlencoded`)
    def sender(content: Map[Symbol, String]) = ByteArrayInput((content map { case (k, v) =>
      java.net.URLEncoder.encode(k.name, "UTF-8")+"="+java.net.URLEncoder.encode(v, "UTF-8")
    } mkString "&").getBytes("UTF-8"))
  }

  implicit val stringPostType: PostType[String] = new PostType[String] {
    def contentType = Some(MimeTypes.`text/plain`)
    def sender(content: String) = ByteArrayInput(content.getBytes("UTF-8"))
  }

  implicit val nonePostType: PostType[None.type] = new PostType[None.type] {
    def contentType = Some(MimeTypes.`application/x-www-form-urlencoded`)
    def sender(content: None.type) = ByteArrayInput(Array[Byte](0))
  }

  /*implicit val JsonPostType = new PostType[Json] {
    def contentType = Some(MimeTypes.`application/x-www-form-urlencoded`)
    def sender(content: Json) =
      ByteArrayInput(content.toString.getBytes("UTF-8"))
  }*/
  
  /** Type class object for reading `Byte`s from `HttpUrl`s */
  implicit val httpStreamByteReader: JavaInputStreamReader[HttpUrl] =
      new JavaInputStreamReader[HttpUrl](_.javaConnection.getInputStream)

  implicit val httpResponseCharReader: Reader[HttpResponse, Char] =
      new Reader[HttpResponse, Char] {
    def input(response: HttpResponse)(implicit mode: Mode[IoMethods]):
        mode.Wrap[Input[Char], Exception] = mode.wrap {
      implicit val enc = encodings.`UTF-8`
      implicit val errorHandler = raw
      response.input[Char]
    }
  }

  implicit val httpResponseByteReader: Reader[HttpResponse, Byte] =
    new Reader[HttpResponse, Byte] {
      def input(response: HttpResponse)(implicit mode: Mode[IoMethods]):
          mode.Wrap[Input[Byte], Exception] =
        mode.wrap(response.input[Byte](?[InputBuilder[InputStream, Byte]], raw))
    }

  implicit val socketStreamByteReader: JavaInputStreamReader[SocketUri] =
    new JavaInputStreamReader[SocketUri](_.javaSocket.getInputStream)

  implicit val socketStreamByteWriter: JavaOutputStreamWriter[SocketUri] =
    new JavaOutputStreamWriter[SocketUri](_.javaSocket.getOutputStream)
}

