plugins {
    id("kotlin-common-conventions")
}

dependencies {
    implementation("org.neo4j:neo4j:2026.02.3")
    implementation("org.neo4j:neo4j-jdbc:6.11.0")
    implementation("org.neo4j.driver:neo4j-java-driver:6.0.3")

    testImplementation("org.neo4j.test:neo4j-harness:2026.02.3")

    testFixturesImplementation(project(":model"))
    implementation(project(":model"))
    implementation(project(":utils"))
    testFixturesImplementation(project(":utils"))
}