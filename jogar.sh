#!/bin/sh
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"
# Nova Frontier - Linux/Mac. Requer Java 17+.
cd "$(dirname "$0")" || exit 1
if ! command -v java >/dev/null 2>&1; then
  echo "Java nao encontrado. Instale o Java 17 ou mais novo (https://adoptium.net) e tente de novo."
  exit 1
fi
JAR=$(ls lwjgl3/build/libs/NovaFrontier-*.jar 2>/dev/null | head -n 1)
if [ -z "$JAR" ]; then
  echo "Primeira execucao: gerando o jogo (precisa de internet)..."
  chmod +x gradlew
  ./gradlew lwjgl3:dist || { echo "Falha ao gerar o jogo."; exit 1; }
  JAR=$(ls lwjgl3/build/libs/NovaFrontier-*.jar | head -n 1)
fi
OPTS=""
[ "$(uname)" = "Darwin" ] && OPTS="-XstartOnFirstThread"
exec java $OPTS -jar "$JAR"
