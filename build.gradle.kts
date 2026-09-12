// Os plugins são declarados aqui com apply false e aplicados em cada módulo.
// ktlint e detekt são configurados uma vez, para todos os módulos.

import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

val versaoKtlint = libs.versions.ktlint.engine.get()

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "dev.detekt")

    configure<KtlintExtension> {
        version.set(versaoKtlint)
    }

    configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        // Por padrão o detekt só lê src/main, e os módulos KMP usam src/commonMain, src/jvmMain etc.
        source.setFrom(files("src"))
    }
}
