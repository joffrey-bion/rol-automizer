plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

description = "An action sequencer to do the boring tasks on the game http://www.riseoflords.com."

application {
    mainClass.set("org.hildan.bots.riseoflords.RolAutomizerKt")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.jsoup)
    implementation(libs.simple.ocr)
    implementation(libs.logback.classic)
    runtimeOnly(libs.angus.mail) // implem of Jakarta Mail API to send emails with logback

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}
