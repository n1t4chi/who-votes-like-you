plugins {
    id("kotlin-module")
    id("test-library")
}

dependencies {
    api(project(":jobs:api"))
}
