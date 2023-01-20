plugins {
    kotlin("jvm") version "1.8.0"
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
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    runtimeOnly("org.eclipse.angus:angus-mail:1.1.0") // implem of Jakarta Mail API to send emails with logback

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

// to build a fat jar with all dependencies included
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest { attributes["Main-Class"] = application.mainClass.get() }
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
