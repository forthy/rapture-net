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
package rapture.net
import rapture.core._
import rapture.io._

object Ftp extends Scheme[FtpUrl] {
  def schemeName = "ftp"
  def /(hostname: String, username: String = null, password: String = null, port: Int = 25) = {
    new FtpPathRoot(hostname, Option(username), Option(password), port)
  }
}

class FtpPathRoot(val hostname: String, val username: Option[String],
    val password: Option[String], val port: Int) extends NetPathRoot[FtpUrl] { thisPathRoot =>
  def scheme = Ftp
 
  def makePath(ascent: Int, elements: Seq[String], afterPath: AfterPath) =
    new FtpUrl(this, elements)

  def /[P <: Path[P]](path: P) = makePath(0, path.elements, Map())

  override def equals(that: Any): Boolean =
    that.isInstanceOf[FtpPathRoot] && hostname == that.asInstanceOf[FtpPathRoot].hostname
}

class FtpUrl(val pathRoot: NetPathRoot[FtpUrl], val elems: Seq[String]) extends
    Url[FtpUrl](elems, Map()) with NetUrl {
  def makePath(ascent: Int, xs: Seq[String], afterPath: AfterPath) =
    new FtpUrl(pathRoot, elements)

  def canonicalPort = 25
  def hostname = pathRoot.hostname
  def port: Int = pathRoot.port
  def ssl: Boolean = false
}

/*class FtpSession(username: String, password: String, passive: Boolean) {
  def connect(server: String): Unit = println("Creating connection to server "+server)
  def close(): Unit = println("Closing connection")
}

case class FtpServer(username: String, password: String, passive: Boolean) {
  def connect[T](blk: FtpSession => T) = {
    val c = new FtpSession(username, password, passive)
    c.connect()
    val s = blk(c)
    c.close()
    s
  }
}

implicit def ftpReader(implicit session: FtpSession) = new StreamReader[FtpUrl, Byte] {
  def input(ftp: FtpUrl): ![Exception, Input[Byte]] = except(new Input[Byte] {
    def read() = None
    def ready() = true
    def close() = ()
  })
}*/
