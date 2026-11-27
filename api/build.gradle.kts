// Serviço principal (DIM0547), com Ktor e Koin. A escolha está em docs/decisoes/0001-monorepo-e-stacks.md.
//   Testes:  ./gradlew :api:test
//   Rodar:   ./gradlew :api:run

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.protobuf) // gera as classes do contrato .proto: mensagens, stub Java e stub Kotlin
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
    implementation(libs.ktor.server.openapi) // gera o OpenAPI a partir do `describe` de cada rota
    implementation(libs.ktor.server.swagger) // serve a Swagger UI em /swagger
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)

    // Persistência: SQL escrito em Kotlin (Exposed), driver JDBC, pool de conexões e migrações (Flyway).
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime) // colunas de data com os tipos do kotlinx.datetime, os do domínio
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    // Cliente gRPC do importador em Go, com stubs de corrotinas.
    implementation(platform(libs.grpc.bom))
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.protobuf.kotlin)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.grpc.inprocess) // servidor gRPC em memória para testar o adaptador
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.koin.test)
    testImplementation(libs.ktor.client.content.negotiation) // os testes de rota leem JSON
    testImplementation(libs.archunit) // regra de dependência entre as camadas, conferida como teste
    testImplementation(libs.testcontainers.postgresql) // PostgreSQL de verdade, num container descartável
}

// O contrato fica fora do módulo, em ../protos, compartilhado com o importador em Go.
sourceSets { main { proto { srcDir("../protos") } } }

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}" }
    plugins {
        create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.asProvider().get()}" }
        create("grpckt") { artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpc.kotlin.get()}:jdk8@jar" }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins { create("kotlin") } // DSL Kotlin para montar as mensagens
        }
    }
}

// Os testes de integração sobem um PostgreSQL com o Testcontainers e precisam do Docker. Numa máquina
// sem Docker, `./gradlew :api:test -PsemDocker` deixa esses testes de fora. O CI roda todos.
tasks.test {
    if (project.hasProperty("semDocker")) {
        filter { excludeTestsMatching("*IntegracaoPostgresTest") }
    }
}
