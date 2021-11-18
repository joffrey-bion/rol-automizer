plugins {
    application
    kotlin("jvm") version "1.6.0"
}

description = "An action sequencer to do the boring tasks on the game http://www.riseoflords.com."

application {
    mainClass.set("org.hildan.bots.riseoflords.RolAutomizerKt")
}

repositories {
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "15"
    }
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.3.0")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("ch.qos.logback:logback-classic:1.2.7")
    implementation("javax.mail:mail:1.4.7") // to send emails with logback
}

// to build a fat jar with all dependencies included
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest { attributes["Main-Class"] = application.mainClass.get() }
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
