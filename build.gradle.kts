import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    `java-library`
    jacoco
}

group = "no.nav"
version = "0.1.0"

repositories {
    mavenCentral()
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    shadowJar {
        archiveClassifier.set("") // Removes the '.all' postfix from the file.
        dependencies {
            // Already available in validator_cli.jar and can therefore be omitted to reduce size.
            exclude(dependency("com.fasterxml.*::"))
        }
    }

    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version
                )
            )
        }
    }

    jacocoTestReport {
        dependsOn(test)
    }
}

dependencies {
    compileOnly("ca.uhn.hapi.fhir:org.hl7.fhir.validation:5.5.9")
    compileOnly("org.junit.platform:junit-platform-engine:1.8.1")
    implementation("com.github.shyiko.klob:klob:0.2.1")
    implementation("com.sksamuel.hoplite:hoplite-json:1.4.9")
    implementation("com.sksamuel.hoplite:hoplite-yaml:1.4.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.junit.platform:junit-platform-testkit:1.8.1")
    testRuntimeOnly("ca.uhn.hapi.fhir:org.hl7.fhir.validation:5.5.7")
    testRuntimeOnly("com.squareup.okhttp3:okhttp:4.9.2")
    testRuntimeOnly("org.slf4j:slf4j-nop:1.7.32")
}
