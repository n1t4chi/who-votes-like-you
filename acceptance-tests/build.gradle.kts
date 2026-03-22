plugins {
    id("kotlin-common-conventions")
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.jsoup:jsoup:1.22.1")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")

    implementation(project( ":vote-fetcher" ))
    implementation(project( ":model" ))
    implementation(project(":utils"))
    testFixturesImplementation(project(":utils"))
}