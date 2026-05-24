plugins {
    id("kotlin-module")
}

dependencies {
    implementation(project(":jobs:service"))
    implementation(project(":storage:in-memory"))
    implementation(project(":voting-tenant:plugin-api"))

    testImplementation(project(":voting-tenant:service"))

    testImplementation(testFixtures(project(":models")))
    testImplementation(testFixtures(project(":jobs:api")))
    testImplementation(testFixtures(project(":voting-tenant:plugin-api")))
}
