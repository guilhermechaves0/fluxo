# Proposta do Fluxo: Sprint 0

Esta proposta atende a duas disciplinas, DIM0547 (Desenvolvimento de Sistemas Web II) e DIM0524 (Desenvolvimento de
Sistemas para Dispositivos Móveis), e segue o formato do projeto de referência MUSI. As seções 5.1 e 5.2 tratam das
decisões de Web II, e as seções 5.3 e 5.4, das decisões de Móveis. As demais valem para as duas disciplinas.

## 1. Visão do produto

```
Para          pessoas que querem controlar os gastos sem digitar cada compra
Que           desistem dos aplicativos de finanças porque não conseguem manter o lançamento manual
O Fluxo       é um assistente de finanças pessoais para Android
Que           importa o extrato do banco em OFX ou CSV e agrupa os gastos por categoria
Diferente de  Mobills e Organizze, que dependem de lançamento manual, e do Pierre, que exige conexão por Open Finance
Nosso produto funciona com o arquivo que o próprio banco exporta e pode ser usado sem internet
```

Hipótese de valor: acreditamos que pessoas que já desistiram de um aplicativo de finanças vão manter o controle mensal
porque importar um extrato leva poucos minutos, enquanto lançar cada compra à mão exige disciplina diária.

A hipótese será testada com colegas que instalarem o aplicativo durante a Sprint 3. A medida é quantos deles importam
o extrato de outubro e voltam para importar o de novembro antes da entrega final. Se a maioria importar uma única vez,
a importação sozinha não sustenta o hábito, e o produto precisa mudar.

## 2. MVP

| No MVP | Fora do MVP |
|---|---|
| Lançamento manual de receitas e despesas, com validação | Conexão com bancos por Open Finance |
| Importação de extrato OFX e CSV sem lançamentos duplicados | Atendimento pelo WhatsApp |
| Categorização automática por regras aplicadas à descrição | Investimentos e patrimônio |
| Lista do mês com saldo e filtros | Mais de uma moeda |
| Orçamento por categoria, com alerta perto do limite | Contas compartilhadas |
| Uso sem internet, com sincronização quando a conexão volta | Painel web |
| Login e sincronização com a api do projeto | Leitura de comprovante por foto |

O Open Finance ficou fora porque o acesso aos dados é restrito a instituições participantes autorizadas, o que não cabe
num projeto de semestre. A importação do extrato leva ao mesmo resultado prático para quem usa: as transações aparecem
no aplicativo sem digitação, a partir de um arquivo que o banco já disponibiliza. A leitura de comprovante por foto
pode entrar na entrega final, se sobrar tempo depois do MVP.

O MVP estará completo quando for possível importar o extrato de um mês de um banco real sem duplicar lançamentos e ver
no celular, em modo avião, o saldo e os gastos por categoria.

## 3. Backlog inicial

O backlog está no GitHub Projects, em https://github.com/guilhermechaves0/fluxo/projects, com estimativas em pontos.

| Prio | História | Critérios de aceitação | Sprint | Pontos |
|---|---|---|---|---|
| P1 | Como usuária, quero lançar uma despesa manualmente para registrar gastos que não aparecem no extrato | Valor, data, descrição e categoria validados; lançamento aparece na lista na hora | 1 | 5 |
| P1 | Como usuária, quero ver as transações do mês para saber para onde foi o dinheiro | Lista por data com saldo; filtro por tipo e categoria; mensagem quando vazia | 1 | 3 |
| P1 | Como usuária, quero importar o extrato OFX ou CSV do banco para não digitar cada compra | Arquivo escolhido no aparelho; interpretação no serviço Go; duplicados ignorados; resumo da importação | 2 e 3 | 8 |
| P1 | Como usuária, quero que as transações importadas já venham categorizadas para não classificar uma a uma | Regras por palavra da descrição; categoria editável; regra aprendida com a edição | 3 | 5 |
| P2 | Como usuária, quero usar o aplicativo sem internet para registrar o gasto na hora | Registro salvo em fila local; sincroniza ao reconectar; indicador de pendência | 3 | 8 |
| P2 | Como usuária, quero ver os gastos por categoria contra o orçamento para ajustar o mês | Total por categoria; alerta ao passar de 80 % do limite | 2 | 5 |
| P2 | Como usuária, quero entrar com login para ter meus dados em mais de um aparelho | JWT com refresh; token em armazenamento seguro do sistema | 3 | 5 |
| P3 | Como usuária, quero fotografar um comprovante para anexar à despesa | Câmera por expect/actual; permissão pedida no momento do uso | Final | 3 |

## 4. Entidades principais do domínio

```
Usuario 1──N Conta 1──N Transacao N──1 Categoria
                             N
                             │
                  Importacao 1

Orcamento: Categoria + mês + limite em centavos
```

Cada transação registra a origem, manual ou importada. A importação guarda um hash de cada linha do extrato para não
duplicar lançamentos.

As entidades existem uma única vez, no módulo `shared/` em Kotlin, usado pelo aplicativo e pela api. Valores em
dinheiro são inteiros em centavos, porque ponto flutuante acumula erro de arredondamento. O serviço Go tem o próprio
tipo `Lancamento`, que espelha a mensagem de mesmo nome em `protos/fluxo/importador/v1/importador.proto`.

## 5. Decisões técnicas por disciplina

### 5.1 Kotlin com Ktor ou Java com Quarkus (DIM0547)

O serviço principal usa Kotlin com Ktor.

O motivo principal vem do produto. O aplicativo de Móveis é escrito em Kotlin Multiplatform e, com o Ktor, o módulo
`shared/`, que reúne as entidades e as regras, é o mesmo no aplicativo e na api, como no MUSI. Com uma pessoa na
equipe, manter o domínio numa linguagem só evita escrever e testar as mesmas regras duas vezes.

A carga da api pesou na escolha. Ela passa a maior parte do tempo esperando o banco, o importador e, mais adiante, o
provedor de LLM usado na categorização. As corrotinas tratam essa espera sem bloquear threads, e o servidor CIO do Ktor
cabe nos 512 MB da hospedagem gratuita prevista para a Sprint 3.

Pelo perfil da equipe, o Quarkus teria menos atrito, porque o autor trabalha com Java e Spring. Mesmo assim, o Ktor
permite estudar outra forma de construir serviços, com as rotas escritas como código e a injeção de dependências
montada num único arquivo.

O Quarkus foi descartado apesar do ecossistema maduro e da compilação nativa com GraalVM. Com ele, o domínio que o
aplicativo compartilha em Kotlin teria de ser reescrito em Java, e o projeto passaria a manter dois modelos de domínio
sincronizados.

### 5.2 Divisão entre o serviço principal e o serviço Go (DIM0547)

| Fica na `api/` (Ktor) | Vai para `services/` (Go) |
|---|---|
| Entidades, regras e casos de uso | Interpretação de extratos OFX e CSV em lote |
| Persistência e migrações | Deduplicação das linhas importadas por hash |
| Autenticação e autorização | Cache das agregações do painel, com métricas de acerto (Sprint 3) |
| Rotas HTTP públicas e documentação OpenAPI | Chamadas ao provedor de LLM, com retentativa e limite de concorrência (Sprint 3) |

Um aplicativo pessoal recebe pouco acesso, então desempenho não justificaria um serviço à parte. A separação se
justifica pelo tipo de trabalho da importação, que é processamento em lote e sem estado. Um extrato de um ano tem
milhares de linhas, e cada arquivo pode ser tratado numa goroutine própria. Se a importação falhar, a api continua
funcionando. Go faz esse trabalho só com a biblioteca padrão e gera um binário estático, empacotado numa imagem de
poucos megabytes.

As regras de negócio, como a validade de uma transação e o cálculo do orçamento, precisam de consistência no banco de
dados e por isso ficam no serviço principal, junto da persistência.

O contrato entre os dois serviços já está em `protos/` e passa por `buf lint` no CI. Na Sprint 1, a api chama o
importador por HTTP e JSON. Na Sprint 2, a chamada passa a usar gRPC, com código gerado a partir do contrato.

### 5.3 Plataforma-alvo (DIM0524)

A plataforma-alvo é o Android.

A maior parte dos smartphones em uso no Brasil roda Android, e o público do Fluxo está nesse grupo. Os recursos
exigidos no bloco final, como a câmera para fotografar comprovantes e o Keystore para guardar o token, funcionam no
emulador gratuito e no aparelho de teste do autor, um Samsung Galaxy A54. O APK assinado pode ser publicado
automaticamente como release do GitHub, sem custo, o que atende ao critério de entrega contínua da avaliação final.

O iOS foi descartado. Sem iPhone, os testes ficariam restritos ao simulador, que não tem câmera, e a distribuição pelo
TestFlight exige o Apple Developer Program, que é pago. Desktop e web não servem como alvo principal porque não dão
acesso aos recursos do dispositivo. O desktop fica como segunda plataforma, usado no dia a dia do desenvolvimento e
candidato ao bônus de entrega multiplataforma.

### 5.4 Backend (DIM0524)

O backend é a opção C: a api do próprio projeto, construída em DIM0547, com o importador em Go.

A importação de extratos precisa de processamento no servidor, com regras de deduplicação específicas do produto.
Usar a mesma api nas duas disciplinas também exercita o contrato entre aplicativo e servidor e dá direito ao bônus de
integração.

O Supabase (opção A) resolveria autenticação e banco, mas a importação e a categorização teriam de rodar nas funções do
próprio serviço. O Firebase (opção B) restringe as consultas ao modelo do Firestore, e o painel depende de agregações
por categoria e por mês. A opção local com APIs públicas (D) não oferece autenticação nem sincronização, e o produto
precisa das duas.

## 6. Equipe

| Nome | Matrícula | Conta no GitHub | Papel |
|---|---|---|---|
| Guilherme Chaves | [preencher] | guilhermechaves0 | Product Owner e desenvolvedor |

## 7. Coorte e integração

A apresentação será na coorte B, online.

O mesmo produto atende a DIM0524 e a DIM0547, com entregáveis separados no mesmo repositório: `app/`, `app-android/` e
`shared/` em Móveis, e `api/`, `services/` e `protos/` em Web II. A integração entre as disciplinas está declarada
conforme a seção de bônus das duas sistemáticas de avaliação.

Há intenção de entrega multiplataforma. O Android é o alvo principal, e o desktop é a segunda plataforma. O pipeline
deve publicar artefatos das duas até a entrega final.
