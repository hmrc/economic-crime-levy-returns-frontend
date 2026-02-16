import sbt.*

object AppDependencies {

  private val hmrcBootstrapVersion = "10.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % hmrcBootstrapVersion exclude("org.apache.commons", "commons-lang3"),
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30" % "12.31.0",
    "org.typelevel" %% "cats-core"                  % "2.13.0",
    "org.apache.commons" % "commons-lang3"    % "3.18.0",
    "ch.qos.logback"     % "logback-core"     % "1.5.27",
    "ch.qos.logback"     % "logback-classic"  % "1.5.27",
    "at.yawk.lz4"        % "lz4-java"         % "1.10.3"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-30" % hmrcBootstrapVersion,
    "org.jsoup"             % "jsoup"                  % "1.21.1",
    "org.mockito"          %% "mockito-scala"          % "2.0.0",
    "org.scalatestplus"    %% "scalacheck-1-17"        % "3.2.18.0",
    "com.danielasfregola"  %% "random-data-generator"  % "2.9",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"  % "1.1.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}