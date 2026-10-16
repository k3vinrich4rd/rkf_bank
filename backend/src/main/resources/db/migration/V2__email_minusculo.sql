-- Migracao de dados: guarda todo e-mail em minusculas e sem espacos nas pontas.
-- O ClienteService passa a normalizar os cadastros novos; esta migracao corrige os que ja existem.
-- Seguro no MySQL: a collation da coluna (utf8mb4_0900_ai_ci) ja ignora maiusculas no UNIQUE,
-- entao nao existem dois e-mails que colidam ao virar minusculas.
-- Sem WHERE de proposito: filtrar com CAST(... AS BINARY) funciona no MySQL, mas no H2 o BINARY sem tamanho
-- compara so o primeiro byte e pularia 'mARIA@x.com'. Atualizar tudo e portavel e idempotente.
UPDATE tb_cliente
SET email = LOWER(TRIM(email));
