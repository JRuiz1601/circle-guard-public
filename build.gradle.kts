import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("org.springframework.boot") version "3.2.4" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
}

allprojects {
    group = "com.circleguard"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

val sprint2CoverageProjects =
    setOf(
        "circleguard-auth-service",
        "circleguard-identity-service",
        "circleguard-gateway-service",
    )

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "implementation"(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
        "testImplementation"(enforcedPlatform("org.testcontainers:testcontainers-bom:2.0.4"))
        // Spring Boot 3.2.x importa gestión de versiones para org.testcontainers:testcontainers en 1.19.x;
        // debe alinearse con el BOM 2.0.4 o el runtime mezcla 1.19 + 2.0 y falla Docker (/info 400).
        "testImplementation"("org.testcontainers:testcontainers") {
            version { strictly("2.0.4") }
        }
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("com.h2database:h2")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    val generateTestcontainersProperties =
        tasks.register("generateTestcontainersProperties") {
            val outDir = layout.buildDirectory.dir("generated-test-resources")
            outputs.dir(outDir)
            doLast {
                val dir = outDir.get().asFile
                dir.mkdirs()
                val text =
                    if (System.getProperty("os.name").lowercase().contains("windows")) {
                        """
                        docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy
                        docker.host=npipe:////./pipe/dockerDesktopLinuxEngine
                        """.trimIndent()
                    } else {
                        "# Detección Docker por defecto (no Windows).\n"
                    }
                java.io.File(dir, "testcontainers.properties").writeText(text)
            }
        }

    tasks.named<ProcessResources>("processTestResources") {
        dependsOn(generateTestcontainersProperties)
        from(layout.buildDirectory.dir("generated-test-resources"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        if (project.path != ":tests:e2e") {
            exclude("**/e2e/**")
        }
        if (!project.hasProperty("runPerformance")) {
            exclude("**/performance/**")
        }
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    val jacocoExcludes =
        listOf(
            "**/*Application.class",
            "**/*Config.class",
            "**/*Configuration.class",
            "**/*Properties.class",
            "**/model/**",
            "**/dto/**",
            "**/event/**",
        )

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))

        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }

        classDirectories.setFrom(
            files(
                classDirectories.files.map {
                    fileTree(it) {
                        exclude(jacocoExcludes)
                    }
                },
            ),
        )
    }

    if (name in sprint2CoverageProjects) {
        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("jacocoTestReport"))

            violationRules {
                rule {
                    limit {
                        counter = "LINE"
                        value = "COVEREDRATIO"
                        minimum = "0.70".toBigDecimal()
                    }
                }
            }

            classDirectories.setFrom(
                files(
                    classDirectories.files.map {
                        fileTree(it) {
                            exclude(jacocoExcludes)
                        }
                    },
                ),
            )
        }
    }
}

tasks.register("jacocoSprint2Coverage") {
    group = "verification"
    description = "Runs JaCoCo reports and enforces 70% line coverage for auth, identity and gateway."

    dependsOn(
        ":services:circleguard-auth-service:jacocoTestCoverageVerification",
        ":services:circleguard-identity-service:jacocoTestCoverageVerification",
        ":services:circleguard-gateway-service:jacocoTestCoverageVerification",
    )
}
