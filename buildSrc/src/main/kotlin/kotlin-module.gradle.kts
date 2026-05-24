plugins {
    id("org.jetbrains.kotlin.jvm")
}
plugins.apply(Deps.Kotest.plugin)
plugins.apply(Deps.Ktlint.plugin)

repositories {
    mavenCentral()
}
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=first-only")
    }
}
dependencies {
    testImplementation(Deps.Kotest.assertionsCode)
    testImplementation(Deps.Kotest.property)
    testImplementation(Deps.Kotest.runner)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    outputs.cacheIf { false }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
