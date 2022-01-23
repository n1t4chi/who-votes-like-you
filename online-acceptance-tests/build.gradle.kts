plugins {
    kotlin("jvm") version "1.6.0"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0-M1")



    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.jsoup:jsoup:1.14.2")

    implementation(project( ":vote-fetcher" ))
    implementation(project( ":model" ))
}

tasks.getByName<Test>("test") {
    onlyIf {
        project.hasProperty("online")
    }
    useJUnitPlatform()
}
