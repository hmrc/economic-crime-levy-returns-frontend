import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % "7.3.0",
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "3.27.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "7.3.0",
    "org.jsoup"               %  "jsoup"                      % "1.14.3",
    "org.scalatestplus"       %% "scalacheck-1-15"            % "3.2.10.0",
    "org.scalatestplus"       %% "mockito-3-4"                % "3.2.10.0",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.62.2"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}
