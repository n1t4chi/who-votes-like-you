plugins {
    id("kotlin-module")
    id("test-library")
}

dependencies {
    api(project(":models"))
    api(project(":jobs:api"))
    api(project(":storage:api"))
}
