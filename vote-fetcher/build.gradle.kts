plugins {
    id("kotlin-common-conventions")
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.jsoup:jsoup:1.22.1")

    implementation(project(":model"))
    implementation(project(":message-system"))
    implementation(project(":vote-storage"))
    testImplementation(testFixtures(project(":message-system")))
    testImplementation(testFixtures(project(":vote-storage")))
    implementation(project(":utils"))
    testFixturesImplementation(project(":utils"))
}