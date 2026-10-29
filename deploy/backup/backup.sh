#!/bin/bash
# Backup dos bancos de produção e homologação.
# Roda dentro do container "backup": faz um backup ao iniciar e depois todo dia no horário de HORA_BACKUP.
# Cada banco vira um arquivo .sql.gz em /backups; arquivos mais antigos que DIAS_RETENCAO são apagados.
set -uo pipefail

HORA_BACKUP="${HORA_BACKUP:-03:00}"
DIAS_RETENCAO="${DIAS_RETENCAO:-7}"
BANCOS="projeto_producao projeto_homologacao"

fazer_backup() {
  local data
  data="$(date +%Y-%m-%d_%H%M)"
  for banco in $BANCOS; do
    local arquivo="/backups/${banco}_${data}.sql.gz"
    if mariadb-dump -h banco -u root -p"${MARIADB_ROOT_PASSWORD}" \
        --single-transaction --routines --triggers --databases "$banco" | gzip > "${arquivo}.tmp"; then
      mv "${arquivo}.tmp" "$arquivo"
      echo "$(date '+%Y-%m-%d %H:%M:%S') backup concluído: $(basename "$arquivo") ($(du -h "$arquivo" | cut -f1))"
    else
      rm -f "${arquivo}.tmp"
      echo "$(date '+%Y-%m-%d %H:%M:%S') ERRO no backup de $banco" >&2
    fi
  done
  find /backups -name "*.sql.gz" -mtime +"$DIAS_RETENCAO" -delete
}

segundos_ate_o_proximo() {
  local agora alvo
  agora="$(date +%s)"
  alvo="$(date -d "today $HORA_BACKUP" +%s)"
  if [ "$alvo" -le "$agora" ]; then
    alvo="$(date -d "tomorrow $HORA_BACKUP" +%s)"
  fi
  echo $((alvo - agora))
}

mkdir -p /backups
fazer_backup
while true; do
  espera="$(segundos_ate_o_proximo)"
  echo "Próximo backup em $((espera / 3600))h$(( (espera % 3600) / 60 ))min"
  sleep "$espera"
  fazer_backup
done
