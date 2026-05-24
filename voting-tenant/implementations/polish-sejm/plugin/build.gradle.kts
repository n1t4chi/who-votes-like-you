plugins {
    id("kotlin-module")
}

dependencies {
    implementation(project(":voting-tenant:plugin-api"))
    implementation(project(":voting-tenant:implementations:polish-sejm:client"))

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:6.1.7")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("com.squareup.moshi:moshi-adapters:1.15.2")

    testImplementation(project(":storage:in-memory"))
    testImplementation(project(":jobs:service"))
    testImplementation(project(":utilities:test-utilities"))

    testImplementation(testFixtures(project(":models")))
    testImplementation(testFixtures(project(":jobs:api")))
    testImplementation(testFixtures(project(":voting-tenant:plugin-api")))
}
