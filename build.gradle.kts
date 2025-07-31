import DotenvUtils.envOrProperty
import groovyjarjarpicocli.CommandLine.ExitCode

plugins {
    scala
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.scala.extras)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.scala)
    implementation(libs.bundles.spark)
}

application {
    mainClass = "org.example.App"
}

tasks.withType<Jar> {
    isZip64 = true
}

tasks.withType<ScalaCompile>().configureEach {
    scalaCompileOptions.additionalParameters.addAll(listOf("-explaintypes"))
}

class AwsConfig {
    val tasksGroup = "AWS services"
    val scriptsDir = projectDir.resolve("scripts")
    val profileName = project.name
    val accessKeyId by project.envOrProperty()
    val secretAccessKey by project.envOrProperty()
    val sessionToken by project.envOrProperty()
    val keyPairPath by project.envOrProperty()
    val keyName by lazy { file(keyPairPath).nameWithoutExtension }
}
val aws = AwsConfig()

val createProfile by tasks.registering(Exec::class) {
    group = aws.tasksGroup
    description = "Create new AWS profile or update existing one."
    requiresAwsCli()
    standardOutput = System.out
    commandLine(
        aws.scriptsDir.resolve("create-profile.sh"),
        aws.profileName,
        aws.accessKeyId,
        aws.secretAccessKey,
        aws.sessionToken,
    )
}

val createCluster by tasks.registering(Exec::class) {
    group = aws.tasksGroup
    description = "Create a new AWS EMR cluster."
    requiresAwsCli()
    dependsOn(createProfile)
    standardOutput = System.out
    commandLine(aws.scriptsDir.resolve("create-cluster.sh"), aws.profileName, aws.keyName)
}

fun Task.requiresAwsCli() = doFirst {
    val isAwsCliInstalled = runCatching { providers.exec { commandLine("aws", "--version") }.result.get().exitValue }
        .map { it == ExitCode.OK }
        .getOrElse { false }
    if (!isAwsCliInstalled) {
        throw GradleException("`aws` command is required by $name task. Please install AWS CLI.")
    }
}
