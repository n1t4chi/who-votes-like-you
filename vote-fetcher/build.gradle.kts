plugins {
    kotlin("jvm") version "1.6.0"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0")

    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("org.jsoup:jsoup:1.14.2")

    implementation(project(":model"))
    implementation(project(":message-system"))
    implementation(project(":vote-storage"))
    testImplementation(testFixtures(project(":message-system")))
    testImplementation(testFixtures(project(":vote-storage")))
}