plugins {
    kotlin("jvm") version "1.6.0"
    java
}

group "who-votes-like-you"
version "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0-M1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.jsoup:jsoup:1.14.2")
    testImplementation("com.github.tomakehurst:wiremock:2.27.2")

    implementation(project( ":vote-fetcher" ))
    implementation(project( ":model" ))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
