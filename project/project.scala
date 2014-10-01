object project extends ProjectSettings {
  def scalaVersion = "2.10.4"
  def version = "0.10.0"
  def name = "net"
  def description = "Rapture Net is a library for Rapture IO for working with networked resources"
  
  def dependencies = Seq(
    "io" -> "0.10.0",
    "crypto" -> "0.10.0",
    "codec" -> "1.0.0"
  )
  
  def thirdPartyDependencies = Seq(
    ("commons-net", "commons-net", "2.0")
  )

  def imports = Seq(
    "rapture.core._",
    "rapture.mime._",
    "rapture.uri._",
    "rapture.io._",
    "rapture.net._"
  )
}
