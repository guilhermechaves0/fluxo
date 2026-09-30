-- Transação: uma entrada ou uma saída de dinheiro numa conta. Uma conta tem muitas transações.
CREATE TABLE transacoes (
    id             VARCHAR(36)  PRIMARY KEY,
    -- Remover a conta remove as transações dela (ADR-0003).
    conta_id       VARCHAR(36)  NOT NULL REFERENCES contas (id) ON DELETE CASCADE,
    descricao      VARCHAR(200) NOT NULL CHECK (btrim(descricao) <> ''),
    -- Dinheiro em centavos e sempre positivo. O sinal vem do tipo.
    valor_centavos BIGINT       NOT NULL CHECK (valor_centavos > 0),
    data           DATE         NOT NULL,
    tipo           VARCHAR(7)   NOT NULL CHECK (tipo IN ('RECEITA', 'DESPESA')),
    -- A categoria fica na própria transação até virar tabela (ADR-0003). As duas colunas andam juntas.
    categoria_id   VARCHAR(60),
    categoria_nome VARCHAR(60),
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT categoria_inteira CHECK ((categoria_id IS NULL) = (categoria_nome IS NULL))
);

-- A chave estrangeira não cria índice sozinha no PostgreSQL. A listagem é sempre de uma conta, da data
-- mais recente para a mais antiga.
CREATE INDEX transacoes_conta_data ON transacoes (conta_id, data DESC);
