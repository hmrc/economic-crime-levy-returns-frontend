import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "7.15.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % hmrcBootstrapVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc"         % "7.3.0-play-28"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-28"   % hmrcBootstrapVersion,
    "org.jsoup"             % "jsoup"                    % "1.15.4",
    "org.mockito"          %% "mockito-scala"            % "1.17.12",
    "org.scalatestplus"    %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "com.danielasfregola"  %% "random-data-generator"    % "2.9",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"    % "1.1.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}
