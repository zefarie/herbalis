import java.security.MessageDigest

plugins {
    java
}

group = "io.github.zefarie"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("Herbalis")
}

// Zippe le resource pack et calcule son SHA-1 pour server.properties
val packResourcePack by tasks.registering(Zip::class) {
    group = "herbalis"
    description = "Zippe le resource pack et calcule son SHA-1"

    archiveFileName.set("Herbalis-ResourcePack.zip")
    destinationDirectory.set(layout.buildDirectory.dir("resourcepack"))

    from(layout.projectDirectory.dir("resourcepack")) {
        exclude("tools/**")
        exclude("**/*.py")
    }

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    doLast {
        val zipFile = archiveFile.get().asFile
        val digest = MessageDigest.getInstance("SHA-1")
        val sha1 = digest.digest(zipFile.readBytes())
            .joinToString("") { "%02x".format(it) }
        val sha1File = zipFile.resolveSibling("Herbalis-ResourcePack.sha1")
        sha1File.writeText(sha1 + "\n")
        println("Resource pack : ${zipFile.absolutePath}")
        println("SHA-1         : $sha1")
    }
}
