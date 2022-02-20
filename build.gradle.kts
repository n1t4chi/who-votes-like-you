plugins {
    java
    `java-test-fixtures`
}

group = "who-votes-like-you"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-test-fixtures")
    
    group = "who-votes-like-you"
    version = "1.0-SNAPSHOT"
    
    repositories {
        mavenCentral()
    }
    
    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
        testImplementation("org.mockito:mockito-core:4.0.0")
    
        testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
        if (this@subprojects.name != "utils") {
            implementation(project(":utils"))
            testFixturesImplementation(project(":utils"))
        }
    
        tasks.findByName("kotlinCompile").let {
            implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
        }
    }
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }
    
    tasks.getByName<Test>("test") {
        jvmArgs("-Dwho.logger.level=DEBUG")
        useJUnitPlatform()
    }
}
