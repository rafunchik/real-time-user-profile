import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.typesafe.sbt.packager.docker.ExecCmd
import sbt.internal.util.ManagedLogger

import scala.io.Source

lazy val artifactoryUrl = "..."

val dockerRegistry = "containers.mpi-internal.com"

val plainVersion = settingKey[String]("The version number containing only [0-9a-zA-Z._]")
plainVersion := version.value.replaceAll("[^0-9a-zA-Z._]", "_").replaceFirst("_", ".")

lazy val dockerSettings = Seq(
  version in Docker := plainVersion.value,
  dockerRepository := Some(dockerRegistry),
  packageName in Docker := s"p10n/${packageName.value}",
  dockerBaseImage := "openjdk:8-jre",
  daemonUser in Docker := "root",
  dockerCommands ++= Seq(
    ExecCmd(
      "RUN", "bash", "-c", "apt-get update && apt-get install -y jq awscli redis-tools kafkacat htop && apt-get clean && rm -rf /var/lib/apt/lists"
    )
  )
)

lazy val artifactorySettings = Seq(
  resolvers ++= Seq(
    "Artifactory Realm Libs" at s"$artifactoryUrl/libs-release/"
  ),
  credentials += Credentials("Artifactory Realm",
    new URL(artifactoryUrl).getHost,
    System.getenv("ARTIFACTORY_USER"),
    System.getenv("ARTIFACTORY_PWD")
  )
)

lazy val ScalaShortVersion = "2.12"
lazy val ScalaLongVersion = s"$ScalaShortVersion.8"
val Http4sVersion = "0.20.8"
val CirceVersion = "0.11.1"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
lazy val CatsVersion  = "1.1.0"
lazy val P10nKafkaVersion = "0.4.3"
lazy val ConfigVersion = "1.3.4"
lazy val enumeratumCirceVersion = "1.5.21"
lazy val pureconfigVersion = "0.11.1"
lazy val kafkaSerdeCirceVersion = "0.4.0"

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging, LinuxPlugin, UpstartPlugin, DockerPlugin)
  .settings(artifactorySettings, dockerSettings)
  .settings(
    organization := "com.ad.mpc.p10n",
    name := "real-time-user-profile",
    scalaVersion := ScalaLongVersion,
    libraryDependencies ++= Seq(
      "org.http4s"                  %% "http4s-blaze-server"   % Http4sVersion,
      "org.http4s"                  %% "http4s-blaze-client"   % Http4sVersion,
      "org.http4s"                  %% "http4s-circe"          % Http4sVersion,
      "org.http4s"                  %% "http4s-dsl"            % Http4sVersion,
      "io.circe"                    %% "circe-generic"         % CirceVersion,
      "io.circe"                    %% "circe-core"            % CirceVersion,
      "io.circe"                    %% "circe-parser"          % CirceVersion,
      "ch.qos.logback"               % "logback-classic"       % LogbackVersion,
      "com.typesafe"                 % "config"                % ConfigVersion,
      "org.typelevel"               %% "cats-core"             % CatsVersion,
      "io.chrisdavenport"           %% "log4cats-slf4j"        % "0.3.0",
      "io.prometheus"                % "simpleclient_hotspot"  % "0.6.0",
      "com.github.pureconfig"       %% "pureconfig"            % pureconfigVersion,
      "com.beachape"                %% "enumeratum-circe"      % enumeratumCirceVersion,
      "io.github.azhur"             %% "kafka-serde-circe"     % kafkaSerdeCirceVersion,
      // TEST dependencies
      "org.scalatest"               %% "scalatest"                % "3.0.8"   % Test,
      "org.mockito"                  % "mockito-core"             % "3.0.0"   % Test,
      "org.apache.kafka"             % "kafka-streams-test-utils" % "2.0.0"   % Test,

      // TODO should be  managed-dependencies
      "..."       %% "p10n-kafka"            % P10nKafkaVersion excludeAll(
        ExclusionRule(organization = "javax.ws.rs")
      ),
      "..."       %% "p10n-aws-sdk"          % "0.4.2"
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
    )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings",
)

// Upload PAAS configuration to artifactory

def uploadYaml(yamls: Iterable[(String, File)])(implicit logger: ManagedLogger): Unit = {
  val travisHelpersPath = "/usr/local/share/bash/travis_helpers.bash"
  val travisHelpersFile = file(travisHelpersPath)
  if (travisHelpersFile.exists()) {
    val publishScript = IO.createTemporaryDirectory / "yaml-upload.sh"
    val header =
      s"""#!/usr/bin/env bash
         |set -euo pipefail
         |IFS=$$'\\n\\t'
         |source /usr/local/share/bash/travis_helpers.bash
         |""".stripMargin
    val body = yamls.map { case (yamlType, yamlFile) =>
      s"upload_yaml $yamlType ${yamlFile.getPath}"
    }.mkString("\n")

    logger.info(s"Uploading yamls ...")

    IO.write(publishScript, header + body)
    publishScript.setExecutable(true)
    import scala.sys.process._
    val exitCode = publishScript.getPath ! logger
    if (exitCode != 0) sys.exit(exitCode)
  }
  else {
    logger.warn("yaml files can only be published from Travis.")
  }
}

val dockerLogin = taskKey[Unit]("Login to Artifactory for docker")
dockerLogin := {
  val logger = streams.value.log
  val user = System.getenv("ARTIFACTORY_USER")

  import scala.sys.process._
  val cmd = Seq("/bin/bash", "-c", s"echo $$ARTIFACTORY_PWD | docker login --username $user --password-stdin $dockerRegistry")
  logger.info(cmd.mkString(" "))
  val exitCode = cmd ! logger
  if (exitCode != 0) sys.exit(exitCode)
}

val dockerPublish = taskKey[Unit]("Build and Publish the docker image and show the details in a way that allows Spinnaker to pick the version to use")
dockerPublish := {
  dockerLogin.value
  (publish in Docker).value
  val repository = dockerRepository.value.fold("")(_ + "/")
  println(s"Pushed image ${repository}${(packageName in Docker).value}:${(version in Docker).value}")
}

val paasPublish = taskKey[Unit]("Publish paas configuration")

paasPublish := {

  val paasDirectory = baseDirectory.value / "deploy" / "paas"

  val accounts = Seq(
    "dev" -> "...",
    "pre" -> "...",
    "pro" -> "..."
  )

  val tempDir = IO.createTemporaryDirectory.toPath
  val passContent = Source.fromFile(paasDirectory / "paas.yml", StandardCharsets.UTF_8.name()).mkString
  val passUploads = accounts.map({ case (env, accountId) =>
    val envPaasContent = passContent
      .replace("${ACCOUNT_ID}", accountId)
      .getBytes(StandardCharsets.UTF_8)
    val outputPath = tempDir.resolve(s"p10n-$env-paas.yml")
    Files.write(outputPath, envPaasContent)
    "paas-app-config" -> outputPath.toFile
  })

  val configUploads = Seq("dev", "pre", "pro").map { env =>
    "k8s-manifest" -> paasDirectory / s"config-$env.yml"
  }

  val spinnakerUploads = Seq(
    "spinnaker-pac" -> paasDirectory / "pipeline-dev.yml",
    "spinnaker-pac" -> paasDirectory / "pipeline-pre-pro.yml"
  )

  uploadYaml(passUploads ++ configUploads ++ spinnakerUploads)(streams.value.log)
}

publish := {
  dockerPublish.value
  paasPublish.value
}
