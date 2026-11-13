package com.fonseca.novafrontier.systems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.fonseca.novafrontier.entities.Enemy;
import com.fonseca.novafrontier.systems.GameModeConfig.Mutator;

/**
 * Monta a composicao de cada onda (ou andar) e libera os inimigos aos poucos.
 * Respeita o modo e a dificuldade (GameModeConfig) e os mutators da Torre.
 */
public class WaveManager {
    public static final int MAX_ALIVE = 55;
    /** Segundos de onda no Modo Infinito antes da protecao anti-estagnacao agir. */
    public static final float STALL_TIME = 45f;

    private final GameModeConfig cfg;
    private final Array<Enemy.Type> queue = new Array<>();
    private float spawnTimer;
    private float spawnGap = 1f;
    private int wave;
    private boolean active;
    private float waveTime;
    private boolean enraged;
    private float stallCheck;
    private final Vector2 tmp = new Vector2();

    public WaveManager(GameModeConfig cfg) {
        this.cfg = cfg;
    }

    public int wave() { return wave; }
    public boolean isActive() { return active; }
    public int remainingToSpawn() { return queue.size; }
    public boolean enraged() { return enraged; }
    public float waveTime() { return waveTime; }

    /** Fator de vida dos inimigos comuns nessa onda (inclui dificuldade e escala do Infinito). */
    public float hpScale() { return cfg.hpScale(wave); }

    public void startWave(int n) { startWave(n, Mutator.NONE); }

    public void startWave(int n, Mutator mutator) {
        wave = n;
        queue.clear();
        Enemy.Type[] bosses = cfg.bosses(n);
        boolean boss = bosses.length > 0;

        int drones = Math.min(30, 4 + n * 3);
        int swarmers = n >= 2 ? Math.min(24, n * 3) : 0;
        int gunners = n >= cfg.diff.gunnerWave ? Math.min(12, n) : 0;
        int brutes = n >= cfg.diff.bruteWave ? Math.min(20, n - 3) : 0;
        if (boss) {
            drones /= 2;
            swarmers /= 2;
            gunners /= 2;
            brutes /= 2;
        } else if (mutator == Mutator.SWARM) {
            // so drones e swarmers: a "ameaca" de gunners/brutes vira enxame
            drones = Math.min(40, drones + gunners + brutes * 2);
            swarmers = Math.min(30, Math.max(swarmers, 6) + gunners);
            gunners = 0;
            brutes = 0;
        } else if (mutator == Mutator.ARMORED) {
            gunners = Math.min(14, 2 + n * 4 / 5);
            brutes = Math.min(10, 1 + n / 2);
            drones = 0;
            swarmers = 0;
        }
        for (int i = 0; i < drones; i++) queue.add(Enemy.Type.DRONE);
        for (int i = 0; i < swarmers; i++) queue.add(Enemy.Type.SWARMER);
        for (int i = 0; i < gunners; i++) queue.add(Enemy.Type.GUNNER);
        for (int i = 0; i < brutes; i++) queue.add(Enemy.Type.BRUTE);
        queue.shuffle();
        for (int i = bosses.length - 1; i >= 0; i--) queue.insert(0, bosses[i]);

        spawnGap = (!boss && mutator == Mutator.SWARM) ? 0.5f : 1f; // enxame: spawn dobrado
        spawnTimer = 0.2f;
        waveTime = 0f;
        enraged = false;
        stallCheck = 0f;
        active = true;
    }

    public void update(float dt, World w) {
        if (!active) return;
        waveTime += dt;
        if (queue.size > 0) {
            spawnTimer -= dt;
            if (spawnTimer <= 0f && w.enemies.size < MAX_ALIVE) {
                Enemy.Type t = queue.removeIndex(0);
                w.spawnEnemyFar(t);
                spawnTimer = MathUtils.random(0.25f, 0.5f) * spawnGap;
            }
        }
        // Modo Infinito: com o teto de inimigos atingido por muito tempo, evita kiting infinito
        if (cfg.isEndless() && waveTime > STALL_TIME && w.enemies.size >= MAX_ALIVE) {
            stallCheck -= dt;
            if (stallCheck <= 0f) {
                stallCheck = 2f;
                breakStall(w);
            }
        }
    }

    /** Enfurece os comuns (+20% velocidade) e traz para perto os que estao fora da tela. */
    private void breakStall(World w) {
        if (!enraged) {
            enraged = true;
            w.feed.system("Os inimigos ficaram enfurecidos!");
        }
        for (int i = 0; i < w.enemies.size; i++) {
            Enemy e = w.enemies.get(i);
            if (e.isBoss() || !e.alive()) continue;
            e.rage = true;
            float dx = e.pos.x - w.player.pos.x, dy = e.pos.y - w.player.pos.y;
            if (Math.abs(dx) > 520f || Math.abs(dy) > 360f) {
                // reposiciona a 60% da distancia atual (minimo 350px), na mesma direcao
                tmp.set(dx, dy);
                float d = Math.max(1f, tmp.len());
                float nd = Math.max(350f, d * 0.6f);
                e.pos.set(w.player.pos).mulAdd(tmp, nd / d);
                e.pos.x = MathUtils.clamp(e.pos.x, e.radius, com.fonseca.novafrontier.Constants.WORLD_W - e.radius);
                e.pos.y = MathUtils.clamp(e.pos.y, e.radius, com.fonseca.novafrontier.Constants.WORLD_H - e.radius);
            }
        }
    }

    public boolean isCleared(World w) {
        return active && queue.size == 0 && w.enemies.size == 0;
    }

    public void finishWave() { active = false; }

    /** Debug: descarta os inimigos que ainda nao surgiram. */
    public void clearQueue() { queue.clear(); }
}
