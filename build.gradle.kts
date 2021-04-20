plugins {
    application
    kotlin("jvm") version "1.4.32"
}

description = "An action sequencer to do the boring tasks on the game http://www.riseoflords.com."

val command = "rolAutomizer"
val moduleName by extra("org.hildan.rol.automizer")
val javaHome = System.getProperty("java.home")

application {
    mainClass.set("org.hildan.bots.riseoflords.RolAutomizerKt")
}

repositories {
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "15"
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
    implementation("org.jsoup:jsoup:1.9.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("javax.mail:mail:1.4.3") // to send emails with logback
}

tasks {
    compileJava {
        inputs.property("moduleName", moduleName)
        options.javaModuleMainClass.set("org.hildan.bots.riseoflords.RolAutomizerKt")
        options.compilerArgs = listOf(
            "--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}"
        )
    }

    val jlink by registering(Exec::class) {
        val outputDir by extra("$buildDir/jlink")
        inputs.files(configurations.runtimeClasspath)
        inputs.files(jar)
        outputs.dir(outputDir)
        doFirst {
            val modulePath = files(jar) + configurations.runtimeClasspath.get()
            logger.lifecycle(modulePath.joinToString("\n", "jlink module path:\n"))
            delete(outputDir)
            commandLine("$javaHome/bin/jlink",
                "--module-path",
                listOf("$javaHome/jmods/", modulePath.asPath).joinToString(File.pathSeparator),
                "--add-modules", moduleName,
                "--output", outputDir,
                "--launcher", "$command=$moduleName"
            )
        }
    }
}
