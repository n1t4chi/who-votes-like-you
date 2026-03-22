plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-test-fixtures`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:6.1.7")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
