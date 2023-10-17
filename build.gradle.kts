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
    implementation(libs.logback.classic)
    runtimeOnly(libs.angus.mail) // implem of Jakarta Mail API to send emails with logback

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
