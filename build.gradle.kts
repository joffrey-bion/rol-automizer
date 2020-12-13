import com.jfrog.bintray.gradle.BintrayExtension.*

plugins {
    java
    application
    kotlin("jvm") version "1.4.21"
    id("com.jfrog.bintray") version "1.8.0"
    id("edu.sc.seis.launch4j") version "2.4.3"
}

group = "org.hildan.bots"
version = "1.3.1"
description = "An action sequencer to do the boring tasks on the game http://www.riseoflords.com."

application {
    mainClass.set("org.hildan.bots.riseoflords.RolAutomizerKt")
}

repositories {
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
    implementation("org.apache.httpcomponents:httpclient:4.5.5")
    implementation("org.jsoup:jsoup:1.9.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("javax.mail:mail:1.4.3") // to send emails with logback
}

// to build a fat jar with all dependencies included
tasks.jar {
    manifest { attributes["Main-Class"] = application.mainClass.get() }
    from({
        configurations.compile.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}

// wraps the executable jar into an exe file
launch4j {
    mainClassName = application.mainClass.get()
    headerType = "console"
    icon = "../resources/main/rol-bot.ico"
    windowTitle = project.name
}

// defines what to distribute (jar and exe)
artifacts {
    archives(tasks.jar)
    archives(file(buildDir.name + "/resources/main/template.rol"))
}

tasks.bintrayUpload.get().dependsOn(tasks.launch4j.get())

bintray {
    user = System.getenv("BINTRAY_USER") ?: ""
    key = System.getenv("BINTRAY_KEY") ?: ""
    setConfigurations("archives")

    publish = true // the version should be auto published after an upload

    pkg(closureOf<PackageConfig> {
        repo = project.findProperty("bintrayRepo")?.toString() ?: ""
        name = project.name
        desc = project.description
        setLabels("riseoflords", "rol", "bot", "game")

        val githubRepoName = project.name
        websiteUrl = "https://github.com/joffrey-bion/$githubRepoName"
        issueTrackerUrl = "https://github.com/joffrey-bion/$githubRepoName/issues"
        vcsUrl = "https://github.com/joffrey-bion/$githubRepoName.git"

        setLicenses("MIT")
        version(closureOf<VersionConfig> {
            vcsTag = "v" + project.version
            gpg(closureOf<GpgConfig> {
                sign = true
            })
        })
    })
}
