plugins {
    kotlin("jvm") version "2.1.0"
    application
}

group = "com.ermukhamed.deepresearch"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

application {
    mainClass.set("com.ermukhamed.deepresearch.MainKt")
    applicationName = "deep-research"
}

// Stamps the project version into a resource so `--version` reports something real.
// The version is captured here, at configuration time: reading `project` inside the
// task action would break the configuration cache.
tasks.processResources {
    // Held in a local so the filter below captures a plain String rather than a
    // reference to this build script, which the configuration cache cannot serialize.
    val stamp = version.toString()
    inputs.property("version", stamp)
    filesMatching("version.properties") {
        expand("version" to stamp)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// `./gradlew run` needs a real console for the interactive query prompt.
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
