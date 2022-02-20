plugins {
    kotlin("jvm") version "1.6.0"
    java
}

group "who-votes-like-you"
version "1.0-SNAPSHOT"

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.jsoup:jsoup:1.14.2")
    testImplementation("com.github.tomakehurst:wiremock:2.27.2")

    implementation(project( ":vote-fetcher" ))
    implementation(project( ":model" ))
}