// Interface do app (DIM0524). O código fica em commonMain e roda no desktop e no Android;
// o APK é gerado por :app-android.
//   Testes:          ./gradlew :app:jvmTest (inclui os testes de interface, que rodam no alvo desktop)
//   App no desktop:  ./gradlew :app:run
//   Hot reload:      ./gradlew :app:hotRunJvm

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization) // rotas da navegação são classes @Serializable
}

kotlin {
    jvm() // desktop, para testar e iterar sem emulador

    androidLibrary {
        namespace = "br.ufrn.fluxo.app"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared")) // mesmo domínio da api

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview) // anotação @Preview no commonMain
            implementation(libs.navigation.compose)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.uiTest)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs) // o Skia desenha a tela nos testes de interface
        }
    }
}

dependencies {
    // Usado pelo IDE para renderizar os @Preview a partir do alvo Android.
    androidRuntimeClasspath(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "br.ufrn.fluxo.MainKt"
    }
}
