object project extends ProjectSettings {
  def scalaVersion = "2.11.0"
  def version = "0.9.0"
  def name = "net"
  def description = "Rapture Net is a library for Rapture IO for working with networked resources"
  
  def dependencies = Seq(
    "io" -> "0.9.1",
    "crypto" -> "0.9.0"
  )
  
  def thirdPartyDependencies = Nil

  def imports = Seq(
    "rapture.core._",
    "rapture.mime._",
    "rapture.uri._",
    "rapture.io._",
    "rapture.net._"
  )
}
