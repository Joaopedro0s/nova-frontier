# Nova Frontier

Shooter cooperativo topdown sci-fi com cara de MMO, em **libGDX (Java 17+, desktop LWJGL3)** com build Gradle.
Você luta ao lado de dois aliados controlados por IA (NovaKai e Vexa_77, que simulam outros jogadores do grupo) contra **10 ondas** de inimigos e **2 bosses**.

## Recursos

* **Armas e raridades:** Blaster, Scatter (escopeta), Laser Rifle (perfura) e Plasma Launcher (explosão em área), cada uma em 4 raridades (Comum, Raro, Épico, Lendário) que multiplicam o dano. Até 4 armas na mochila; armas repetidas e piores são vendidas automaticamente.
* **Itens:** créditos, medkits, células de escudo e caixas de arma. Chefes sempre largam arma Rara ou melhor.
* **Loja entre as ondas:** casco, escudo, motores, calibre (dano) e reparo total, com preço crescente.
* **Ondas:** 10 ondas com composição e vida crescentes; a onda 5 tem o **Colossus** e a onda 10 a **Rainha Enxame**.
* **Bosses com 2 fases** (fúria abaixo de 50% de vida): o Colossus atira em leque, solta anéis de balas e faz investidas telegrafadas; a Rainha faz espirais de balas, anéis e convoca enxames.
* **Inimigos:** Drone (persegue), Swarmer (rápido e frágil), Gunner (mantém distância e atira), Brute (lento e resistente).
* **Feel de MMO:** grupo com barras de vida (aliados caem e voltam após 10 s), chat dos aliados, log de loot, números de dano e críticos, minimapa, combo de abates e câmera que segue num mundo maior que a tela.
* **Dash** com invulnerabilidade (ESPAÇO), escudo que regenera, tremor de câmera e partículas.

## Controles

`WASD` / setas: mover · **auto-tiro (`F`) e auto-mira (`T`) ligados por padrão**, então basta andar; manualmente: `MOUSE` mira, `botão esquerdo` ou `J` atira · `ESPAÇO`: dash · `1-4` / `Q` / `E`: trocar de arma · `P` / `ESC`: pausar · loja: `1-5` comprar, `ENTER`/`ESPAÇO` próxima onda.

## Como compilar e executar

Requisitos: **JDK 17 ou superior** e internet na primeira vez (o Gradle baixa o libGDX do Maven Central).

```bash
cd nova-frontier
chmod +x gradlew
./gradlew lwjgl3:run            # Windows: gradlew.bat lwjgl3:run

# jar executável único
./gradlew lwjgl3:dist
java -jar lwjgl3/build/libs/NovaFrontier-1.0.0.jar     # macOS: java -XstartOnFirstThread -jar ...
```

Se o `gradlew` não funcionar, qualquer Gradle 8.x instalado serve: `gradle lwjgl3:run`.

## Organização do código

```
core/src/main/java/com/fonseca/novafrontier/
  NovaFrontierGame, Assets (pixel art procedural), Constants, Palette
  screens/   BaseScreen, MenuScreen, GameScreen (estados, câmera, HUD, loja), GameOverScreen
  entities/  Entity, Player, Ally (IA de grupo), Enemy, Boss, Bullet, Pickup
  items/     Weapon, WeaponType, Rarity, Upgrade
  systems/   World (combate, projéteis, loot), WaveManager, ScoreManager, FeedLog, ParticleSystem
  ui/        MenuButton
lwjgl3/      Lwjgl3Launcher (ponto de entrada desktop)
```

## Recursos visuais: origem e licença

* **Sprites, chão, brilhos e ícones:** pixel art **gerada proceduralmente** pelo código (`Assets.java`). Nenhum arquivo de imagem externo. Autoria do projeto, liberados sob **CC0 1.0 (domínio público)**.
* **Fonte:** `BitmapFont` padrão que acompanha o libGDX (framework sob Apache License 2.0).
* **Áudio:** não há.

---

## Expansao (v2)

**Modos** (menu JOGAR): Operacao (10 ondas, bosses nas ondas 5 e 10), Infinito (escala 15%/onda e +6%/onda apos a 10, boss a cada 5 ondas desde a 15, dois bosses na 25+) e Torre (20 andares, mutators Swarm/Armored/Ion Storm/Overload, loja de draft com 3 opcoes). Dificuldades: Recruta, Veterano (base) e Pesadelo.

**Itens**: afixos de arma (Calibrada, Perfura, Criogenica, Vampirica, Comando), Arsenal Dinamico (trocar de arma apos abate = +15% cadencia por 3s) e 3 slots de modulos passivos (Reator de Dash, Capacitor de Escudo, Link Neural, Lente Manual).

**Controles extras**: `C` postura dos aliados (Defensivo/Agressivo/Foco Boss), `F11` tela cheia, `F3` overlay de debug (numpad 1-4: pular onda, spawnar boss, creditos, lendaria; desliga conquistas na partida).

**Persistencia**: perfil em JSON nas Preferences `nova-frontier-profile` (recorde antigo e migrado). Dados Estelares, Hangar (ima, revive, armas iniciais, skins) e 8 conquistas.

**Testes**: `./gradlew test` roda `ExpansionRulesTest` (regras) e `BalanceSimulationTest` (simulacao headless com taxa de sobrevivencia e onda media de morte). Use `-Dnf.runs=N` para mais partidas.

## Como rodar

Requisito: Java 17 ou mais novo (https://adoptium.net).

- Mais facil: Windows = duplo clique em `jogar.bat`; Linux/Mac = `./jogar.sh`. Na primeira vez eles geram o jogo (precisa de internet) e depois so abrem.

- Windows: `gradlew.bat lwjgl3:run`
- Linux/Mac: `chmod +x gradlew && ./gradlew lwjgl3:run`
- Jar unico (qualquer sistema): `./gradlew lwjgl3:dist` e depois `java -jar lwjgl3/build/libs/NovaFrontier-*.jar` (no Mac: `java -XstartOnFirstThread -jar ...`)

### Problemas comuns

Erro "Failed to exec spawn helper" no `run`:
1. `./gradlew --stop` e tente de novo.
2. Adicione `org.gradle.jvmargs=-Djdk.lang.Process.launchMechanism=VFORK` ao `gradle.properties`, rode `./gradlew --stop` e tente de novo.
3. Ou use o jar unico (comando acima).
# nova-frontier
