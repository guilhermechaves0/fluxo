# Imagem da api em Kotlin com Ktor.
#
# A imagem copia a distribuição gerada fora do container por ./gradlew :api:installDist
# (a tarefa mise run up já faz isso). Compilar dentro do container exigiria o SDK do Android,
# porque o módulo :shared tem alvo Android. Ver docs/decisoes/0001-monorepo-e-stacks.md.
FROM eclipse-temurin:21-jre
WORKDIR /app

# A api roda com um usuário sem privilégios.
RUN groupadd --system fluxo && useradd --system --gid fluxo fluxo
COPY api/build/install/api/ ./
USER fluxo

# Opções para caber em 512 MB: -Xmx e -Xms limitam o heap, TieredStopAtLevel=1 reduz o
# custo do JIT e UseSerialGC usa o coletor de lixo serial.
ENV JAVA_TOOL_OPTIONS="-Xmx300m -Xms150m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC"

EXPOSE 8080
ENTRYPOINT ["./bin/api"]
