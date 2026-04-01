plugins {
    kotlin("jvm") version "1.9.23"
    application
}

group = "com.kazumaproject"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-jackson:2.3.12")
    implementation("io.ktor:ktor-server-call-logging:2.3.12")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("runWeb", JavaExec::class) {
    group = "application"
    description = "Run the web autocomplete demo server"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("WebMainKt")
}

tasks.register("generatePagesData", JavaExec::class) {
    group = "documentation"
    description = "Generate static autocomplete data for GitHub Pages"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("PagesDataMainKt")
}

kotlin {
    jvmToolchain(17)
}
