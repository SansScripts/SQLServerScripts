plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.dbtools"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // SQL Server JDBC Driver
    implementation("com.microsoft.sqlserver:mssql-jdbc:12.8.1.jre11")
    
    // Apache Commons CLI for command line parsing
    implementation("commons-cli:commons-cli:1.6.0")
    
    // Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

tasks.shadowJar {
    archiveBaseName.set("sqlserver-scripts")
    archiveClassifier.set("-all")
    archiveVersion.set(version.toString())
    manifest {
        attributes["Main-Class"] = "com.dbtools.sqlserverscripts.SQLServerScriptsCLI"
    }
}

// Custom run task
tasks.register<JavaExec>("run") {
    dependsOn(tasks.shadowJar)
    classpath = files(tasks.shadowJar.get().archiveFile)
    mainClass.set("com.dbtools.sqlserverscripts.SQLServerScriptsCLI")
    standardInput = System.`in`
    args = project.findProperty("appArgs")?.toString()?.split(" ") ?: emptyList()
}

// Make build depend on shadowJar
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Task to create distribution
tasks.register<Copy>("createDist") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.get().archiveFile)
    into("dist")
}

// Clean task
tasks.clean {
    delete("dist")
}
