plugins {
    id("org.springframework.boot") version "3.5.1"
}

dependencies {
    implementation(project(":mirems-core:core-domain"))
    implementation(project(":mirems-core:core-infra"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.mapstruct:mapstruct")
    annotationProcessor(platform(project(":mirems-bom")))
    annotationProcessor("org.mapstruct:mapstruct-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}
