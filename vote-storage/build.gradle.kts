plugins {
    id("kotlin-common-conventions")
}

dependencies {
    implementation("org.neo4j:neo4j:4.3.6")
    implementation("org.neo4j:neo4j-jdbc:4.0.4")
    implementation("org.neo4j.driver:neo4j-java-driver:4.3.4")

    testImplementation("org.neo4j.test:neo4j-harness:4.3.6")
    testImplementation("com.graphaware.neo4j:tests:4.2.0.58") {
        exclude(group = "com.eaio.uuid", module = "uuid")
    }

    testFixturesImplementation(project(":model"))
    implementation(project(":model"))
    implementation(project(":utils"))
    testFixturesImplementation(project(":utils"))
}