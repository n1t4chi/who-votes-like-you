plugins {
    id("kotlin-module")
    id("test-library")
}

dependencies {
    api(project(":storage:api"))
}
