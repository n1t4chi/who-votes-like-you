plugins {
    java
}

group "who-votes-like-you"
version "1.0-SNAPSHOT"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.mockito:mockito-core:4.0.0")

    implementation("org.neo4j:neo4j:4.3.6")
    implementation("org.neo4j.driver:neo4j-java-driver:4.3.4")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")

    implementation(project(":vote-fetcher"))
    implementation(project(":vote-storage"))
    testImplementation(testFixtures(project(":vote-storage")))
    implementation(project(":model"))
}