val akkaVersion = "2.5.22"
val akkaHttpVersion = "10.1.8"

val root = (project in file("."))
  .settings(
    name := "wallet-akka",
    version := "1.0.0",
    scalaVersion := "2.12.8",
    scalafmtOnCompile in ThisBuild := true,
    libraryDependencies ++= Seq(
      "de.huxhorn.sulky" % "de.huxhorn.sulky.ulid" % "8.2.0",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "org.slf4j" % "slf4j-api" % "1.7.26",
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8" % Test,
      "org.iq80.leveldb" % "leveldb" % "0.9" % Test,
      "commons-io" % "commons-io" % "2.4" % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
    ),
    wartremoverErrors in (Compile, compile) ++= Seq(Wart.ArrayEquals, Wart.AnyVal, Wart.Var, Wart.Null, Wart.OptionPartial),
    parallelExecution in Test := false,
    fork := true,
    // --- sbt-multi-jvm用の設定
    compile in MultiJvm := (compile in MultiJvm).triggeredBy(compile in Test).value,
    executeTests in Test := Def.task {
      val testResults = (executeTests in Test).value
      val multiNodeResults = (executeTests in MultiJvm).value
      val overall = (testResults.overall, multiNodeResults.overall) match {
        case (TestResult.Passed, TestResult.Passed) => TestResult.Passed
        case (TestResult.Error, _) => TestResult.Error
        case (_, TestResult.Error) => TestResult.Error
        case (TestResult.Failed, _) => TestResult.Failed
        case (_, TestResult.Failed) => TestResult.Failed
      }
      Tests.Output(overall,
        testResults.events ++ multiNodeResults.events,
        testResults.summaries ++ multiNodeResults.summaries)
    }.value,
    assemblyMergeStrategy in(MultiJvm, assembly) := {
      case "application.conf" => MergeStrategy.concat
      case "META-INF/aop.xml" => MergeStrategy.concat
      case x =>
        val old = (assemblyMergeStrategy in(MultiJvm, assembly)).value
        old(x)
    }
    //, logLevel := Level.Debug
  )
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
