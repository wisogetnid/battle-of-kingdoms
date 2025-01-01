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
    implementation("ch.qos.logback:logback-classic:$logback_version")

    testImplementation(kotlin("test"))
    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-params
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
}

application {
    mainClass.set("org.battleofkingdoms.game.server.GameServer")
}

tasks.test {
    useJUnitPlatform()
}