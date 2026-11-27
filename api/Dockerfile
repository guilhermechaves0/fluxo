# Imagem da api em Kotlin com Ktor, em dois estágios. O contexto do build é a raiz do repositório:
#   docker build -f api/Dockerfile -t fluxo-api .
#
# O estágio de build compila dentro do container, então um clone limpo basta para `docker compose up --build`.
# O módulo :shared tem alvo Android, que exigiria o SDK do Android na imagem; por isso o build passa
# -Pfluxo.soApi=true, que deixa de fora o alvo Android e os módulos :app e :app-android (ver
# settings.gradle.kts e shared/build-api.gradle.kts). O build normal do projeto não muda.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# O wrapper e os arquivos de build primeiro. O download do Gradle vai para o mesmo cache mount do build,
# senão a camada dele ficaria escondida pelo mount do RUN seguinte e o download se repetiria.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --version

COPY config config
COPY protos protos
COPY shared shared
COPY api api

# O cache do Gradle fica num volume do BuildKit, fora da imagem, e acelera os builds seguintes. O compilador
# do Kotlin roda no processo do Gradle (in-process), para o -Xmx2g do gradle.properties não criar um segundo
# daemon e estourar a memória de um Docker pequeno.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon -Pfluxo.soApi=true -Pkotlin.compiler.execution.strategy=in-process \
        -Dorg.gradle.jvmargs="-Xmx1500m -Dfile.encoding=UTF-8" \
        :api:installDist -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

# curl só para o health check do compose; a imagem do JRE não o traz.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# A api roda com um usuário sem privilégios.
RUN groupadd --system fluxo && useradd --system --gid fluxo fluxo
COPY --from=build /src/api/build/install/api/ ./
USER fluxo

# Opções para caber em 512 MB: -Xmx e -Xms limitam o heap, TieredStopAtLevel=1 reduz o
# custo do JIT e UseSerialGC usa o coletor de lixo serial.
ENV JAVA_TOOL_OPTIONS="-Xmx300m -Xms150m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC"

# `docker run IMAGEM migrar` aplica as migrações e termina; sem argumento a api sobe o servidor.
EXPOSE 8080
ENTRYPOINT ["./bin/api"]
