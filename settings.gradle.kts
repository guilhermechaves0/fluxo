// Build Gradle dos módulos Kotlin. services/ (Go) e protos/ (buf) usam as próprias ferramentas.

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "fluxo"

// Com -Pfluxo.soApi=true o build só enxerga :api e :shared sem o alvo Android (shared/build-api.gradle.kts).
// É assim que o Dockerfile da api compila dentro do container, que não tem o SDK do Android. Sem a
// propriedade nada muda.
val soApi = providers.gradleProperty("fluxo.soApi").isPresent

include(":shared") // domínio usado pelo app e pela api
include(":api") // serviço principal em Ktor, DIM0547
if (soApi) {
    project(":shared").buildFileName = "build-api.gradle.kts"
} else {
    include(":app") // interface em Compose Multiplatform, DIM0524
    include(":app-android") // gera o APK (ver ADR-0001)
}
