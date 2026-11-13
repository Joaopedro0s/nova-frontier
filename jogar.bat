@echo off
rem Nova Frontier - Windows. Requer Java 17+.
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java nao encontrado. Instale o Java 17 ou mais novo: https://adoptium.net
  pause
  exit /b 1
)
set JAR=
for %%f in (lwjgl3\build\libs\NovaFrontier-*.jar) do set JAR=%%f
if not defined JAR (
  echo Primeira execucao: gerando o jogo ^(precisa de internet^)...
  call gradlew.bat lwjgl3:dist
  if errorlevel 1 (
    echo Falha ao gerar o jogo.
    pause
    exit /b 1
  )
  for %%f in (lwjgl3\build\libs\NovaFrontier-*.jar) do set JAR=%%f
)
java -jar "%JAR%"
if errorlevel 1 pause
