dependencies {
    implementation(project(":mirems-core:core-domain"))
    implementation(platform("org.kie.kogito:kogito-spring-boot-bom:10.2.0"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.kie.kogito:spring-boot-starters:10.2.0")

    // P2-019 originally named these legacy starters:
    // - org.kie.kogito:kogito-spring-boot-starter
    // - org.kie.kogito:kogito-processes-spring-boot-starter
    // - org.kie.kogito:kogito-decisions-spring-boot-starter
    // They are not published at Kogito 10.2.0; Kogito 10.2.0 Spring Boot uses the
    // org.kie.kogito:spring-boot-starters aggregator under kogito-spring-boot-bom.

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.kie.kogito:kogito-spring-boot-test-utils")
}
