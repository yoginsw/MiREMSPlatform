package io.mirems.core.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.mirems.core")
public class MiremsCoreApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiremsCoreApiApplication.class, args);
    }
}
