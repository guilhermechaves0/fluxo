# Imagem do importador em Go. A imagem final parte de `scratch` e contém só o binário estático.

FROM golang:1.27-alpine AS build
WORKDIR /build

# O módulo ainda não tem dependência externa, por isso não existe go.sum.
COPY go.mod ./
RUN go mod download

COPY . .

# CGO_ENABLED=0 gera um binário estático, necessário para rodar em `scratch`.
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /importador ./cmd/importador

FROM scratch
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=build /importador /importador

# O usuário é numérico porque `scratch` não tem /etc/passwd.
USER 65534:65534

EXPOSE 9090
ENTRYPOINT ["/importador"]
