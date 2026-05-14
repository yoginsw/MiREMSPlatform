import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    base
}

group = "io.mirems"
version = "0.1.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}

val backendJavaProjects = listOf(
    ":mirems-auth",
    ":mirems-core:core-domain",
    ":mirems-core:core-bpmn",
    ":mirems-core:core-api",
    ":mirems-core:core-infra",
    ":extensions:ext-common",
    ":extensions:ext-us",
    ":extensions:ext-kr",
    ":extensions:ext-template",
)

configure(backendJavaProjects.map { project(it) }) {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "implementation"(platform(project(":mirems-bom")))
        "testImplementation"(platform(project(":mirems-bom")))
        "implementation"("jakarta.persistence:jakarta.persistence-api")
        "implementation"("org.hibernate.orm:hibernate-core")
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude("**/generated/**")
                }
            })
        )
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
