plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:3.5.1"))
    api(platform("org.kie.kogito:kogito-bom:10.2.0"))
    api(platform("org.testcontainers:testcontainers-bom:1.19.8"))

    constraints {
        api("org.postgresql:postgresql:42.7.3")
        api("org.mapstruct:mapstruct:1.5.5.Final")
        api("org.mapstruct:mapstruct-processor:1.5.5.Final")
    }
}
