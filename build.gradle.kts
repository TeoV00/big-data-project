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

