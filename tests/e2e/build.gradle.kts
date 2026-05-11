plugins {
    id("java")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    // Root subprojects add Lombok to all modules; the BOM does not resolve a version for testAnnotationProcessor here.
    val lombok = "1.18.30"
    testCompileOnly("org.projectlombok:lombok:$lombok")
    testAnnotationProcessor("org.projectlombok:lombok:$lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
