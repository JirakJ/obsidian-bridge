plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "dev.htmeta"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

intellij {
    version.set("2023.3")
    type.set("IC")
    plugins.set(listOf())
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
