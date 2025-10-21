plugins {
    kotlin("jvm") version "1.9.10"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
}

application {
    mainClass.set("MainKt") // Replace with your actual main class if different
}