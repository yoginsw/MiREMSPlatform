import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("org.springframework.boot") version "3.5.1"
    id("org.openapi.generator") version "7.8.0"
}

springBoot {
    buildInfo {
        properties {
            name.set("MiREMS Platform Core API")
            version.set(project.version.toString())
        }
    }
}

val openApiSpec = rootProject.layout.projectDirectory.file("docs/api/mirems-api.yaml")
val generatedServerDir = layout.buildDirectory.dir("generated/openapi/server")
val generatedTypeScriptClientDir = rootProject.layout.projectDirectory.dir("frontend/packages/api-client/src/generated")
val generatedGitResourcesDir = layout.buildDirectory.dir("generated/resources/git")

val generateGitProperties by tasks.registering {
    val outputFile = generatedGitResourcesDir.map { it.file("git.properties") }
    outputs.file(outputFile)
    doLast {
        val gitCommit = runCatching {
            providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.get().trim()
        }.getOrDefault("unknown")
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("git.commit.id.abbrev=$gitCommit\ngit.commit.id=$gitCommit\n")
        }
    }
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(openApiSpec.asFile.absolutePath)
    outputDir.set(generatedServerDir.get().asFile.absolutePath)
    apiPackage.set("io.mirems.core.api.generated.api")
    modelPackage.set("io.mirems.core.api.generated.model")
    invokerPackage.set("io.mirems.core.api.generated.invoker")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "delegatePattern" to "false",
            "documentationProvider" to "none",
            "interfaceOnly" to "true",
            "openApiNullable" to "false",
            "skipDefaultInterface" to "true",
            "useBeanValidation" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true"
        )
    )
}

tasks.register<GenerateTask>("generateTypeScriptAxiosClient") {
    generatorName.set("typescript-axios")
    inputSpec.set(openApiSpec.asFile.absolutePath)
    outputDir.set(generatedTypeScriptClientDir.asFile.absolutePath)
    configOptions.set(
        mapOf(
            "apiPackage" to "apis",
            "modelPackage" to "models",
            "npmName" to "@mirems/api-client",
            "supportsES6" to "true",
            "typescriptThreePlus" to "true",
            "withSeparateModelsAndApi" to "true"
        )
    )
    doLast {
        generatedTypeScriptClientDir.asFile.walkTopDown()
            .filter { it.isFile && it.extension in setOf("ts", "md", "json") }
            .forEach { file ->
                val trimmedLines = file.readLines()
                    .map { it.trimEnd() }
                    .dropLastWhile { it.isBlank() }
                file.writeText(trimmedLines.joinToString("\n") + "\n")
            }
        generatedTypeScriptClientDir.file("package.json").asFile.delete()
    }
}

sourceSets {
    main {
        java.srcDir(generatedServerDir.map { it.dir("src/main/java") })
        resources.srcDir(generatedGitResourcesDir)
    }
}

tasks.named("processResources") {
    dependsOn(tasks.named("bootBuildInfo"), generateGitProperties)
}

tasks.named("compileJava") {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.named("check") {
    dependsOn(tasks.named("generateTypeScriptAxiosClient"))
}

dependencies {
    implementation(project(":mirems-core:core-domain"))
    implementation(project(":mirems-core:core-bpmn"))
    implementation(project(":mirems-core:core-infra"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.22")
    implementation("org.mapstruct:mapstruct")
    annotationProcessor(platform(project(":mirems-bom")))
    annotationProcessor("org.mapstruct:mapstruct-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.rest-assured:rest-assured")
}
