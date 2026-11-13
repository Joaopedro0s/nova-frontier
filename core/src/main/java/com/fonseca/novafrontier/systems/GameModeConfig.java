package com.fonseca.novafrontier.systems;

import com.fonseca.novafrontier.entities.Enemy;
import com.fonseca.novafrontier.items.WeaponType;

/**
 * Configuracao de uma run: modo de jogo + dificuldade. E injetada no World, no WaveManager e
 * na GameScreen, e concentra todas as formulas de escala (vida, dano, chefes).
 */
public final class GameModeConfig {

    public enum Mode {
        OPERACAO("Operacao", "10 ondas, bosses nas ondas 5 e 10"),
        INFINITO("Infinito", "Sem limite de ondas; inimigos escalam sem parar"),
        TORRE("Torre", "20 andares com mutators e loja draft");

        public final String label;
        public final String description;

        Mode(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    public enum Difficulty {
        //        nome        vida   dano  cura   projet. dropVida gunner brute fase2  score  recompensa
        FACIL("Recruta",       0.8f, 0.8f, 1.25f, 1.0f,   25f,     3,     5,    0.50f, 1.0f,  0.75f),
        DIFICIL("Veterano",    1.0f, 1.0f, 1.0f,  1.0f,   25f,     3,     5,    0.50f, 1.0f,  1.0f),
        HARD("Pesadelo",       1.35f, 1.25f, 1.0f, 1.2f,  15f,     2,     4,    0.65f, 1.5f,  1.5f);

        public final String label;
        public final float hp, dmg, healDrop, bulletSpeed, dropLife;
        public final int gunnerWave, bruteWave;
        /** Fracao de vida abaixo da qual o boss entra na fase 2. */
        public final float bossPhase2;
        public final float scoreMult, rewardMult;

        Difficulty(String label, float hp, float dmg, float healDrop, float bulletSpeed, float dropLife,
                   int gunnerWave, int bruteWave, float bossPhase2, float scoreMult, float rewardMult) {
            this.label = label;
            this.hp = hp;
            this.dmg = dmg;
            this.healDrop = healDrop;
            this.bulletSpeed = bulletSpeed;
            this.dropLife = dropLife;
            this.gunnerWave = gunnerWave;
            this.bruteWave = bruteWave;
            this.bossPhase2 = bossPhase2;
            this.scoreMult = scoreMult;
            this.rewardMult = rewardMult;
        }
    }

    /** Modificadores de sala do Modo Torre. */
    public enum Mutator {
        NONE("Nenhum", ""),
        SWARM("Enxame", "So drones e swarmers, spawn dobrado, +25% creditos"),
        ARMORED("Blindado", "So brutes e gunners"),
        ION_STORM("Tempestade Ionica", "Escudo regenera mais devagar, critico x2.5"),
        OVERLOAD("Sobrecarga", "Cooldown do dash pela metade");

        public final String label;
        public final String description;

        Mutator(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    public final Mode mode;
    public final Difficulty diff;
    /** Arma inicial escolhida para a run (Comum). */
    public WeaponType startWeapon = WeaponType.BLASTER;

    public GameModeConfig(Mode mode, Difficulty diff) {
        this.mode = mode;
        // a Torre ignora a dificuldade (sempre o padrao)
        this.diff = mode == Mode.TORRE ? Difficulty.DIFICIL : diff;
    }

    public boolean isEndless() { return mode == Mode.INFINITO; }

    public boolean isTower() { return mode == Mode.TORRE; }

    /** Numero de ondas para vencer; Integer.MAX_VALUE no Infinito. */
    public int totalWaves() {
        switch (mode) {
            case OPERACAO: return 10;
            case TORRE: return 20;
            default: return Integer.MAX_VALUE;
        }
    }

    /** Nome da unidade de progresso ("ONDA" ou "ANDAR"). */
    public String unit() { return mode == Mode.TORRE ? "ANDAR" : "ONDA"; }

    /** Bosses que surgem nessa onda (vazio se nao for onda de boss). */
    public Enemy.Type[] bosses(int wave) {
        final Enemy.Type[] none = {};
        switch (mode) {
            case OPERACAO:
                if (wave == 5) return new Enemy.Type[] { Enemy.Type.COLOSSUS };
                if (wave == 10) return new Enemy.Type[] { Enemy.Type.QUEEN };
                return none;
            case TORRE:
                if (wave == 5 || wave == 15) return new Enemy.Type[] { Enemy.Type.COLOSSUS };
                if (wave == 10) return new Enemy.Type[] { Enemy.Type.QUEEN };
                if (wave == 20) return new Enemy.Type[] { Enemy.Type.COLOSSUS, Enemy.Type.QUEEN };
                return none;
            default: // INFINITO
                if (wave == 5) return new Enemy.Type[] { Enemy.Type.COLOSSUS };
                if (wave == 10) return new Enemy.Type[] { Enemy.Type.QUEEN };
                if (wave > 10 && wave % 5 == 0) {
                    if (wave >= 25) return new Enemy.Type[] { Enemy.Type.COLOSSUS, Enemy.Type.QUEEN };
                    return new Enemy.Type[] { wave == 15 ? Enemy.Type.COLOSSUS : Enemy.Type.QUEEN };
                }
                return none;
        }
    }

    public boolean isBossWave(int wave) { return bosses(wave).length > 0; }

    /** Crescimento composto do Modo Infinito a partir da onda 11. */
    private double compound(int wave) {
        return isEndless() ? Math.pow(1.06, Math.max(0, wave - 10)) : 1.0;
    }

    /** Multiplicador de vida dos inimigos comuns: base * (1 + 0.15 * (onda - 1)) * composto * dificuldade. */
    public float hpScale(int wave) {
        return (float) ((1.0 + 0.15 * (wave - 1)) * compound(wave)) * diff.hp;
    }

    /** Multiplicador de vida dos bosses (crescimento base menor que o dos comuns). */
    public float bossHpScale(int wave) {
        return (float) ((1.0 + 0.05 * (wave - 1)) * compound(wave)) * diff.hp;
    }

    /** Multiplicador de dano inimigo: dificuldade e, no Infinito, +5% por onda depois da 10. */
    public float dmgScale(int wave) {
        float extra = isEndless() && wave > 10 ? 1f + 0.05f * (wave - 10) : 1f;
        return diff.dmg * extra;
    }

    public String label() {
        return mode == Mode.TORRE ? mode.label : mode.label + " / " + diff.label;
    }
}
