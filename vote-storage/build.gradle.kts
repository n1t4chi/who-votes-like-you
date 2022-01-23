plugins {
    kotlin("jvm") version "1.6.0"
    java
    `java-test-fixtures`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0-M1")

    implementation("org.neo4j:neo4j:4.3.6")
    implementation("org.neo4j:neo4j-jdbc:4.0.4")
    implementation("org.neo4j.driver:neo4j-java-driver:4.3.4")

    testImplementation("org.neo4j.test:neo4j-harness:4.3.6")
    testImplementation("com.graphaware.neo4j:tests:4.2.0.58") {
        exclude(group = "com.eaio.uuid", module = "uuid")
    }

    testFixturesImplementation(project( ":model" ))
    implementation(project( ":model" ))
}