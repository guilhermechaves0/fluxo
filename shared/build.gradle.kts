// Domínio do Fluxo, usado por :api e por :app. O commonMain não pode depender de Compose, Ktor ou Koin.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvm() // alvo usado pela api e pelos testes

    androidLibrary {
        namespace = "br.ufrn.fluxo.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime) // LocalDate aparece nas assinaturas públicas do domínio
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
