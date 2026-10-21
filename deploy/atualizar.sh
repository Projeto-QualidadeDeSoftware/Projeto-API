#!/bin/bash
# Usado pelo CD: troca a imagem de um ambiente e reinicia só aquele ambiente.
# Uso: ./atualizar.sh homologacao|producao <imagem>
set -euo pipefail

ambiente="${1:-}"
imagem="${2:-}"

case "$ambiente" in
  homologacao) variavel=IMAGEM_HOMOLOGACAO; servico=api-homologacao ;;
  producao)    variavel=IMAGEM_PRODUCAO;    servico=api-producao ;;
  *) echo "Uso: $0 homologacao|producao <imagem>" >&2; exit 1 ;;
esac

if [ -z "$imagem" ]; then
  echo "Informe a imagem" >&2
  exit 1
fi

cd "$(dirname "$0")"

if grep -q "^${variavel}=" .env; then
  sed -i "s|^${variavel}=.*|${variavel}=${imagem}|" .env
else
  echo "${variavel}=${imagem}" >> .env
fi

docker compose pull "$servico"
docker compose up -d "$servico"
docker image prune -f > /dev/null

echo "$(date '+%Y-%m-%d %H:%M:%S') ${ambiente} ${imagem}" >> implantacoes.log
echo "${servico} atualizado para ${imagem}"
