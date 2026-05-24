plugins {
    id("kotlin-module")
    id("test-library")
}

dependencies {
    api(project(":voting-tenant:plugin-api"))
}
