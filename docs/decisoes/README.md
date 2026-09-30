# Decisões de arquitetura

Registro aqui as decisões de arquitetura do projeto, com as alternativas que descartei em cada uma.

| # | Título | Estado |
|---|---|---|
| [0001](0001-monorepo-e-stacks.md) | Um monorepo para duas disciplinas, com Kotlin (KMP e Ktor) e Go | Aceita |
| [0002](0002-leitura-de-fatura-em-pdf.md) | Leitura da fatura do cartão em PDF no importador | Aceita |
| [0003](0003-persistencia-em-postgresql.md) | Persistência em PostgreSQL, com Flyway e Exposed | Aceita |

Cada nova ADR começa como cópia de [`0000-modelo.md`](0000-modelo.md), com o próximo número da sequência. Depois de
aceita, não edito a ADR: quando uma decisão muda, escrevo uma nova que substitui a anterior.
