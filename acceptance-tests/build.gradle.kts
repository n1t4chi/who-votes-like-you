plugins {
    id("kotlin-common-conventions")
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.jsoup:jsoup:1.14.2")
    testImplementation("com.github.tomakehurst:wiremock:2.27.2")

    implementation(project( ":vote-fetcher" ))
    implementation(project( ":model" ))
    implementation(project(":utils"))
    testFixturesImplementation(project(":utils"))
}