import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % hmrcBootstrapVersion,
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30" % "8.5.0",
    "org.typelevel" %% "cats-core"                  % "2.10.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-30"   % hmrcBootstrapVersion,
    "org.jsoup"             % "jsoup"                    % "1.17.2",
    "org.mockito"          %% "mockito-scala"            % "1.17.30",
    "org.scalatestplus"    %% "scalacheck-1-17"          % "3.2.18.0",
    "com.danielasfregola"  %% "random-data-generator"    % "2.9",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"    % "1.1.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}
