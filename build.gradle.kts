plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

group = "it.gmmz.anncsu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(libs.klite.server)
    implementation(libs.klite.json)
    implementation(libs.klite.jobs)
    implementation(libs.klite.openapi)

    implementation(libs.univocity.parsers)
    implementation(libs.clikt)
    implementation(libs.sqlite.jdbc)
}

application {
    mainClass.set("it.gmmz.anncsu.MainKt")
}

tasks.shadowJar {
    archiveFileName.set("gmmzanncsu-all.jar")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "it.gmmz.anncsu.MainKt"
    }
}

kotlin {
    jvmToolchain(21)
}