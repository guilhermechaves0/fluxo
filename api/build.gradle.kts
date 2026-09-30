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
    implementation(libs.ktor.client.core) // chama o importador em Go
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)

    // Persistência: SQL escrito em Kotlin (Exposed), driver JDBC, pool de conexões e migrações (Flyway).
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.koin.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.archunit) // regra de dependência entre as camadas, conferida como teste
    testImplementation(libs.testcontainers.postgresql) // PostgreSQL de verdade, num container descartável
}

// Os testes de integração sobem um PostgreSQL com o Testcontainers e precisam do Docker. Numa máquina
// sem Docker, `./gradlew :api:test -PsemDocker` deixa esses testes de fora. O CI roda todos.
tasks.test {
    if (project.hasProperty("semDocker")) {
        filter { excludeTestsMatching("*IntegracaoPostgresTest") }
    }
}
