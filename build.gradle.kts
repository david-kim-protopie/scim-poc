plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "io.protopie.cloud.scim"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}