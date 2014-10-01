[![Build Status](https://travis-ci.org/propensive/rapture-net.png?branch=scala-2.11)](https://travis-ci.org/propensive/rapture-net)

# Rapture Net

Rapture Net is a library for Rapture IO for working with filesystems.

### Status

Rapture Net is *managed*. This means that the API is expected to continue to evolve, but all API changes will be documented with instructions on how to upgrade.

### Availability

Rapture Net 0.10.0 is available under the Apache 2.0 License from Maven Central with group ID `com.propensive` and artifact ID `rapture-net_2.11`.

#### SBT

You can include Rapture Net as a dependency in your own project by adding the following library dependency to your build file:

```scala
libraryDependencies ++= Seq("com.propensive" %% "rapture-net" % "0.10.0")
```

#### Maven

If you use Maven, include the following dependency:

```xml
<dependency>
  <groupId>com.propensive</groupId>
  <artifactId>rapture-net_2.11</artifactId>
  <version>0.10.0<version>
</dependency>
```

#### Download

You can download Rapture Net directly from the [Rapture website](http://rapture.io/)
Rapture Net depends on Scala 2.11 and Rapture Core, URI, MIME, Crypto & IO but has no third-party dependencies.

#### Building from source

To build Rapture URI from source, follow these steps:

```
git clone git@github.com:propensive/rapture-net.git
cd rapture-net
sbt package
```

If the compilation is successful, the compiled JAR file should be found in target/scala-2.11
