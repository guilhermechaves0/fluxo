// Variante de shared/build.gradle.kts usada só no build da imagem da api (-Pfluxo.soApi=true, ver o
// settings.gradle.kts). Mesmo domínio e mesmo alvo JVM, sem o alvo Android, que exigiria o SDK do Android
// dentro do container. Se mudar uma dependência de commonMain, mude nos dois arquivos.

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm() // alvo usado pela api

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
    }
}
