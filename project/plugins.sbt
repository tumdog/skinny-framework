resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.5.0")

addSbtPlugin("org.scalatra.sbt" % "scalatra-sbt" % "0.3.5" excludeAll(
  ExclusionRule(organization = "org.mortbay.jetty"),
  ExclusionRule(organization = "org.eclipse.jetty"),
  ExclusionRule(organization = "org.apache.tomcat.embed"),
  ExclusionRule(organization = "com.earldouglas")
))

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.9.0" excludeAll(
  ExclusionRule(organization = "org.mortbay.jetty"),
  ExclusionRule(organization = "org.eclipse.jetty"),
  ExclusionRule(organization = "org.apache.tomcat.embed")
))

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// here to reduce blanka-app deps
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.6")

// Don't use 0.99.9 because scoverage appends scalac-scoverage-plugin to our artifact automatically
// https://github.com/scoverage/sbt-scoverage/issues/57
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.7.1")

// TODO java.lang.ArrayIndexOutOfBoundsException: -1 with 0.13.2-MX
//scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

