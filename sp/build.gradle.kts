plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor 서버 관련
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.call.logging)

    // 직렬화 관련
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.datatype.jsr310)

    // 데이터베이스 관련
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2)

    // 로깅
    implementation(libs.logback.classic)

    // 테스트
    testImplementation(libs.ktor.server.test.host)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }

    // 테스트 격리 및 순서 보장
    maxParallelForks = 1
    systemProperty("junit.jupiter.execution.order.random.seed", "0")
    systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_method")
}
