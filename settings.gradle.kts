pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "mirems-platform"

include("mirems-bom")
project(":mirems-bom").projectDir = file("backend/mirems-bom")

include("mirems-auth")
project(":mirems-auth").projectDir = file("backend/mirems-auth")

include("mirems-core")
project(":mirems-core").projectDir = file("backend/mirems-core")

include("mirems-core:core-domain")
project(":mirems-core:core-domain").projectDir = file("backend/mirems-core/core-domain")

include("mirems-core:core-bpmn")
project(":mirems-core:core-bpmn").projectDir = file("backend/mirems-core/core-bpmn")

include("mirems-core:core-api")
project(":mirems-core:core-api").projectDir = file("backend/mirems-core/core-api")

include("mirems-core:core-infra")
project(":mirems-core:core-infra").projectDir = file("backend/mirems-core/core-infra")

include("extensions")
project(":extensions").projectDir = file("backend/extensions")

include("extensions:ext-common")
project(":extensions:ext-common").projectDir = file("backend/extensions/ext-common")

include("extensions:ext-us")
project(":extensions:ext-us").projectDir = file("backend/extensions/ext-us")

include("extensions:ext-kr")
project(":extensions:ext-kr").projectDir = file("backend/extensions/ext-kr")

include("extensions:ext-template")
project(":extensions:ext-template").projectDir = file("backend/extensions/ext-template")
