-- Conta: de onde o dinheiro sai e para onde entra (conta corrente, cartão, carteira).
-- O id é um UUID gerado pela api.
CREATE TABLE contas (
    id   VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(60) NOT NULL CHECK (btrim(nome) <> '')
);

-- Duas contas não têm o mesmo nome, sem diferenciar maiúsculas de minúsculas.
CREATE UNIQUE INDEX contas_nome_unico ON contas (lower(nome));
