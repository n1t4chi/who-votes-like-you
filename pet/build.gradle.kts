plugins {
    kotlin("jvm") version "1.6.0"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0-M1")

    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("org.jsoup:jsoup:1.14.2")
    implementation("org.neo4j:neo4j:4.3.6")
    implementation("org.neo4j:neo4j-jdbc:4.0.4")
    implementation("org.neo4j.driver:neo4j-java-driver:4.3.4")

    testImplementation("com.github.tomakehurst:wiremock:2.27.2")
    testImplementation("org.neo4j.test:neo4j-harness:4.3.6")
    testImplementation("com.graphaware.neo4j:tests:4.2.0.58") {
        exclude(group = "com.eaio.uuid", module = "uuid")
    }

    implementation(project( ":vote-fetcher" ))
    implementation(project( ":vote-storage" ))
    implementation(project( ":model" ))
    implementation(project( ":db-synchronizer" ))
}

tasks.getByName<Test>("test") {
    onlyIf {
        project.hasProperty("pet")
    }
    useJUnitPlatform()
}