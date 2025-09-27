plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
}

group = "org.projects"
version = "1.0.0"

description = "Kotlin library for scheduling and executing AWS Lambda jobs with one-time and recurrent schedules."

repositories {
    mavenCentral()
}

dependencies {
    api("software.amazon.awssdk:lambda:2.27.21")
    api("software.amazon.awssdk:iam:2.27.21")
    api("software.amazon.awssdk:s3:2.27.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://example.org/${project.name}")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("org-projects")
                        name.set("Org Projects")
                        email.set("dev@example.org")
                    }
                }
                scm {
                    connection.set("scm:git:git://example.org/${project.name}.git")
                    developerConnection.set("scm:git:ssh://example.org/${project.name}.git")
                    url.set("https://example.org/${project.name}")
                }
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
}