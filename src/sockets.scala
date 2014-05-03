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

import java.io._
import java.net._

object Tcp {
  /** Listens for incoming connections on the specified port
    *
    * @usecase def listen(port: Int): Input[Byte]
    * @param port the port to listen to */
  def listen[K](port: Int)(implicit ib: InputBuilder[InputStream, K],
      ob: OutputBuilder[OutputStream, K], rts: Rts[IoMethods]):
      rts.Wrap[(Input[K], Output[K]), Exception] = rts.wrap {
    val sock = new java.net.ServerSocket(port)
    val sock2 = sock.accept()
    (ib.input(sock2.getInputStream)(raw), ob.output(sock2.getOutputStream)(raw))
  }

  def handle[K](port: Int)(action: (Input[K], Output[K]) => Unit)
      (implicit ib: InputBuilder[InputStream, K], ob: OutputBuilder[OutputStream, K]): Unit = {
    val sock = new java.net.ServerSocket(port)
    while(true) {
      val sock2 = sock.accept()
      fork { action(ib.input(sock2.getInputStream)(raw),
          ob.output(sock2.getOutputStream)(raw)) }
    }
  }
  
  /*def listen(port: Int, local: Boolean = true, timeout: Int = 2000) = {
    val socket = new ServerSocket()
    socket.setSoTimeout(timeout)
    if(local) socket.bind(new InetSocketAddress("127.0.0.1", port))
    else socket.bind(new InetSocketAddress(port))
  }*/
}

class SocketUri(val hostname: String, val port: Int) extends Uri {
  
  def scheme = Socket
  lazy val javaSocket: java.net.Socket = new java.net.Socket(hostname, port)
  
  def schemeSpecificPart = "//"+hostname+":"+port
  
  def absolute = true

}

object Socket extends Scheme[SocketUri] {
  def schemeName = "socket"
  def apply(hostname: String, port: Int): SocketUri = new SocketUri(hostname, port)
  def apply(hostname: String, svc: TcpService): SocketUri = new SocketUri(hostname, svc.portNo)
}


