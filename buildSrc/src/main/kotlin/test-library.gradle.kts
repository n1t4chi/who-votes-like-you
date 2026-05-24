import org.gradle.kotlin.dsl.`java-test-fixtures`

plugins {
    `java-test-fixtures`
}

dependencies {
    testFixturesImplementation(Deps.Kotest.assertionsCode)
    testFixturesImplementation(Deps.Kotest.property)
}