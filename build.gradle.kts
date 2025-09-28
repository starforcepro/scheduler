plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
    signing
}

// Coordinates are typically provided via gradle.properties for CI/CD
group = (findProperty("GROUP") as String?) ?: "org.projects"
version = (findProperty("VERSION_NAME") as String?) ?: "1.0.0"

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
    testImplementation("io.mockk:mockk:1.14.5")
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
            artifactId = (findProperty("POM_ARTIFACT_ID") as String?) ?: project.name
            pom {
                name.set((findProperty("POM_NAME") as String?) ?: project.name)
                description.set(project.description)
                url.set((findProperty("POM_URL") as String?) ?: "https://github.com/org-projects/scheduler")
                licenses {
                    license {
                        name.set((findProperty("POM_LICENSE_NAME") as String?) ?: "The Apache License, Version 2.0")
                        url.set((findProperty("POM_LICENSE_URL") as String?) ?: "https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set((findProperty("POM_DEVELOPER_ID") as String?) ?: "org-projects")
                        name.set((findProperty("POM_DEVELOPER_NAME") as String?) ?: "Org Projects")
                        email.set((findProperty("POM_DEVELOPER_EMAIL") as String?) ?: "dev@example.org")
                    }
                }
                scm {
                    connection.set((findProperty("POM_SCM_CONNECTION") as String?) ?: "scm:git:https://github.com/org-projects/scheduler.git")
                    developerConnection.set((findProperty("POM_SCM_DEV_CONNECTION") as String?) ?: "scm:git:ssh://git@github.com:org-projects/scheduler.git")
                    url.set((findProperty("POM_SCM_URL") as String?) ?: "https://github.com/org-projects/scheduler")
                }
            }
        }
    }
    repositories {
        maven {
            name = "OSSRH"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if ((version as String).endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = findProperty("OSSRH_USERNAME") as String?
                password = findProperty("OSSRH_PASSWORD") as String?
            }
        }
    }
}

signing {
    // Only sign if signing key is provided (CI). For local builds, this is skipped.
    val inMemoryKey = (findProperty("SIGNING_KEY") as String?)?.trim()
    val hasKey = !inMemoryKey.isNullOrBlank() || project.hasProperty("signing.keyId")
    if (hasKey) {
        val inMemoryPass = (findProperty("SIGNING_PASSWORD") as String?)?.trim()
        if (!inMemoryKey.isNullOrBlank()) {
            useInMemoryPgpKeys(inMemoryKey, inMemoryPass)
        }
        sign(publishing.publications["mavenJava"])
    }
}

kotlin {
    jvmToolchain(24)
}