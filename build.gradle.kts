import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

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

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "implementation"(platform(project(":mirems-bom")))
        "testImplementation"(platform(project(":mirems-bom")))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
