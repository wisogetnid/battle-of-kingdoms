val kotlin_version = "2.1.0"
val ktor_version = "3.0.3"
val logback_version = "1.5.15"

plugins {
    kotlin("jvm") version "2.1.0"
    application
}

group = "org.battleofkingdoms"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
}

application {
    mainClass.set("org.battleofkingdoms.server.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}

tasks.register<JavaExec>("simulate") {
    description = "Plays seeded bot games and prints a balance report, e.g. --args=\"--games 5000 --seed 42\""
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.battleofkingdoms.simulator.MainKt")
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}
