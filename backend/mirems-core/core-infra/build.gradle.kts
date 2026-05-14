dependencies {
    implementation(project(":mirems-core:core-domain"))
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("io.micrometer:micrometer-core")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
