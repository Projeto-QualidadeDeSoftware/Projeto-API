#!/bin/bash
# Executado só na primeira vez que o banco sobe (volume vazio).
# Cada ambiente tem o próprio banco e o próprio usuário, sem acesso ao do outro.
set -e

mariadb --protocol=socket -uroot -p"${MARIADB_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS projeto_homologacao CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS projeto_producao CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'app_homologacao'@'%' IDENTIFIED BY '${SENHA_HOMOLOGACAO}';
GRANT ALL PRIVILEGES ON projeto_homologacao.* TO 'app_homologacao'@'%';

CREATE USER IF NOT EXISTS 'app_producao'@'%' IDENTIFIED BY '${SENHA_PRODUCAO}';
GRANT ALL PRIVILEGES ON projeto_producao.* TO 'app_producao'@'%';

CREATE USER IF NOT EXISTS 'monitoramento'@'%' IDENTIFIED BY '${SENHA_MONITORAMENTO}' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'monitoramento'@'%';

FLUSH PRIVILEGES;
SQL
