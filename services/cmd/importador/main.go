// Servidor gRPC do importador do Fluxo (DIM0547).
//
// Recebe extratos bancários em OFX, CSV ou PDF (fatura do cartão do Banco do Brasil) pelo
// ImportadorService e devolve os lançamentos, lidos com o parser de importador/dominio.
//
//	go run ./cmd/importador          # escuta na porta 9090
//	PORT=9091 go run ./cmd/importador
//	importador saude                 # pergunta ao serviço local se está de pé (healthcheck do container)
package main

import (
	"context"
	"log/slog"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"

	importadorv1 "github.com/guilhermechaves0/fluxo/services/gen/fluxo/importador/v1"
	"github.com/guilhermechaves0/fluxo/services/grpcapi"
)

const (
	portaPadrao     = "9090"
	folgaDaMensagem = 1 << 20 // o protobuf acrescenta alguns bytes ao conteúdo; 1 MB sobra
	prazoDaSaude    = 2 * time.Second
	prazoDoDesligar = 10 * time.Second
)

func main() {
	porta := os.Getenv("PORT")
	if porta == "" {
		porta = portaPadrao
	}
	if len(os.Args) > 1 && os.Args[1] == "saude" {
		os.Exit(verificarSaude(porta))
	}
	lis, err := net.Listen("tcp", ":"+porta)
	if err != nil {
		slog.Error("não abriu a porta", "porta", porta, "erro", err)
		os.Exit(1)
	}

	// O padrão do gRPC é 4 MB por mensagem, abaixo do limite de 5 MB do extrato.
	srv := grpc.NewServer(grpc.MaxRecvMsgSize(grpcapi.TamanhoMaximo + folgaDaMensagem))
	importadorv1.RegisterImportadorServiceServer(srv, grpcapi.NovoServidor())

	// Health check padrão do gRPC: o docker compose e as plataformas perguntam se o serviço está de pé.
	saude := health.NewServer()
	healthpb.RegisterHealthServer(srv, saude)
	saude.SetServingStatus("", healthpb.HealthCheckResponse_SERVING)
	saude.SetServingStatus(importadorv1.ImportadorService_ServiceDesc.ServiceName, healthpb.HealthCheckResponse_SERVING)

	// Reflection: deixa o grpcurl descobrir o serviço sem ter o .proto em mãos.
	reflection.Register(srv)

	// Ao receber SIGTERM (docker stop), para de aceitar chamadas novas e termina as que estão em andamento.
	// Se alguma não terminar a tempo, Stop corta.
	go func() {
		sinal := make(chan os.Signal, 1)
		signal.Notify(sinal, syscall.SIGINT, syscall.SIGTERM)
		<-sinal
		slog.Info("encerrando")
		saude.Shutdown()
		time.AfterFunc(prazoDoDesligar, srv.Stop)
		srv.GracefulStop()
	}()

	slog.Info("importador no ar", "porta", porta)
	if err := srv.Serve(lis); err != nil {
		slog.Error("servidor parou", "erro", err)
		os.Exit(1)
	}
}

// verificarSaude chama o health check gRPC do próprio serviço. A imagem final não tem shell nem curl,
// então o healthcheck do compose roda este subcomando.
func verificarSaude(porta string) int {
	conn, err := grpc.NewClient("localhost:"+porta, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return 1
	}
	defer func() { _ = conn.Close() }()
	ctx, cancelar := context.WithTimeout(context.Background(), prazoDaSaude)
	defer cancelar()
	resp, err := healthpb.NewHealthClient(conn).Check(ctx, &healthpb.HealthCheckRequest{})
	if err != nil || resp.GetStatus() != healthpb.HealthCheckResponse_SERVING {
		return 1
	}
	return 0
}
