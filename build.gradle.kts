
plugins {
    kotlin("jvm") version "2.0.20"
    application
}

group = "wordlike"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.xerial:sqlite-jdbc:3.32.3.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    distZip {
        archiveFileName.set("wordlike.zip")
    }
    distTar {
        archiveFileName.set("wordlike.tar")
    }
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("wordlike.MainKt")
}
