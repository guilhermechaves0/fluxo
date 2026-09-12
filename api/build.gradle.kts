// Serviço principal (DIM0547), com Ktor e Koin. A escolha está em docs/decisoes/0001-monorepo-e-stacks.md.
//   Testes:  ./gradlew :api:test
//   Rodar:   ./gradlew :api:run

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

application {
    mainClass.set("br.ufrn.fluxo.AplicacaoKt")
}

dependencies {
    implementation(project(":shared")) // mesmo domínio do app

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio) // engine baseada em corrotinas, gasta menos memória que a Netty
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.koin.test)
}
