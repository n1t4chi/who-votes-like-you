plugins {
    id("kotlin-common-conventions")
}

dependencies {
    implementation("org.neo4j:neo4j:4.3.6")
    implementation("org.neo4j.driver:neo4j-java-driver:4.3.4")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")

    implementation(project(":vote-fetcher"))
    implementation(project(":vote-storage"))
    testImplementation(testFixtures(project(":vote-storage")))
    implementation(project(":model"))
    implementation(project(":utils"))
    testFixturesImplementation(project(":utils"))
}