package com.fonseca.novafrontier;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.fonseca.novafrontier.entities.Enemy;
import com.fonseca.novafrontier.entities.Player;
import com.fonseca.novafrontier.items.Upgrade;
import com.fonseca.novafrontier.systems.Draft;
import com.fonseca.novafrontier.systems.GameModeConfig;
import com.fonseca.novafrontier.systems.GameModeConfig.Difficulty;
import com.fonseca.novafrontier.systems.GameModeConfig.Mode;
import com.fonseca.novafrontier.systems.ProfileManager;
import com.fonseca.novafrontier.systems.World;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Simulador headless: um bot heuristico joga partidas completas (sem janela, sem audio) e mede
 * a taxa de sobrevivencia e a onda media de morte de cada modo/dificuldade.
 * O bot anda para longe dos inimigos, da dash quando cercado, compra upgrades, escolhe cartas do
 * draft na Torre e usa a melhor arma. Nao e um jogador humano: serve para comparar configuracoes.
 */
public class BalanceSimulator {

    /** Resultado agregado de varias partidas da mesma configuracao. */
    public static final class Result {
        public final String label;
        public int runs, survived, stalled, anomalies;
        public double sumDeathWave, sumWavesCleared;
        public int bestWave;

        Result(String label) { this.label = label; }

        public double survivalRate() { return runs == 0 ? 0 : survived / (double) runs; }

        /** Onda media de morte (so das partidas que terminaram em derrota). */
        public double avgDeathWave() {
            int deaths = runs - survived;
            return deaths == 0 ? 0 : sumDeathWave / deaths;
        }

        public double avgWavesCleared() { return runs == 0 ? 0 : sumWavesCleared / runs; }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                    "%-22s partidas=%-3d sobrevivencia=%5.1f%%  onda media de morte=%5.1f  ondas limpas (media)=%5.1f  melhor=%d%s",
                    label, runs, survivalRate() * 100, avgDeathWave(), avgWavesCleared(), bestWave,
                    anomalies > 0 ? "  ANOMALIAS=" + anomalies : "");
        }
    }

    private static final float DT = 1f / 60f;
    private static final float WAVE_TIMEOUT = 240f;
    /** Limite de ondas no Modo Infinito para a simulacao terminar. */
    public static final int ENDLESS_CAP = 30;

    private final Set<Integer> held = new HashSet<>();
    private final Set<Integer> justPressed = new HashSet<>();

    /** Instala um Gdx.input falso controlado pelo bot (so uma vez por JVM). */
    public BalanceSimulator() {
        ProfileManager.useMemoryOnly();
        Gdx.input = (Input) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Input.class },
                (proxy, m, args) -> {
                    switch (m.getName()) {
                        case "isKeyPressed": return held.contains((Integer) args[0]);
                        case "isKeyJustPressed": return justPressed.remove((Integer) args[0]);
                        case "isButtonPressed": return true; // atira sempre
                        default:
                    }
                    Class<?> r = m.getReturnType();
                    if (r == boolean.class) return false;
                    if (r == int.class) return 0;
                    if (r == float.class) return 0f;
                    if (r == long.class) return 0L;
                    return null;
                });
    }

    public Result simulate(Mode mode, Difficulty diff, int runs, long seed) {
        GameModeConfig probe = new GameModeConfig(mode, diff);
        Result res = new Result(probe.label());
        for (int i = 0; i < runs; i++) {
            MathUtils.random = new RandomXS128(seed + i * 7919L);
            playOnce(new GameModeConfig(mode, diff), res);
        }
        return res;
    }

    private void playOnce(GameModeConfig cfg, Result res) {
        ProfileManager.useMemoryOnly();
        World w = new World(cfg);
        Player p = w.player;
        int cap = cfg.isEndless() ? ENDLESS_CAP : cfg.totalWaves();
        boolean survived = true;
        int deathWave = 0;
        for (int wave = 1; wave <= cap; wave++) {
            w.prepareWave(wave);
            w.beginWave(wave);
            float t = 0f;
            while (true) {
                steer(w);
                w.update(DT, true);
                held.clear();
                t += DT;
                if (anomaly(w)) res.anomalies++;
                if (!p.alive()) break;
                if (w.waves.isCleared(w)) break;
                if (t > WAVE_TIMEOUT) break;
            }
            if (!p.alive()) {
                survived = false;
                deathWave = wave;
                break;
            }
            if (!w.waves.isCleared(w)) { // onda travada: conta como derrota por estagnacao
                survived = false;
                deathWave = wave;
                res.stalled++;
                break;
            }
            w.score.addBonus(wave * 100);
            w.endWave();
            res.sumWavesCleared++;
            res.bestWave = Math.max(res.bestWave, wave);
            if (wave < cap) shop(w, cfg);
        }
        res.runs++;
        if (survived) {
            res.survived++;
        } else {
            res.sumDeathWave += deathWave;
        }
    }

    /** Anda para longe do inimigo mais proximo, mira nele e usa o dash quando cercado. */
    private void steer(World w) {
        Player p = w.player;
        Enemy n = w.nearestEnemy(p.pos, 5000f);
        if (n != null) p.aim.set(n.pos);
        float fx = 0f, fy = 0f;
        int close = 0;
        for (int i = 0; i < w.enemies.size; i++) {
            Enemy e = w.enemies.get(i);
            float dx = p.pos.x - e.pos.x, dy = p.pos.y - e.pos.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 300f && d > 1f) {
                float wgt = (300f - d) / 300f;
                fx += dx / d * wgt;
                fy += dy / d * wgt;
                if (d < 110f) close++;
            }
        }
        // empurrao suave para o centro quando perto das paredes
        float mx = Constants.WORLD_W / 2f - p.pos.x, my = Constants.WORLD_H / 2f - p.pos.y;
        if (Math.abs(mx) > 700f) fx += Math.signum(mx) * 0.8f;
        if (Math.abs(my) > 450f) fy += Math.signum(my) * 0.8f;
        if (fx < -0.2f) held.add(Input.Keys.A);
        if (fx > 0.2f) held.add(Input.Keys.D);
        if (fy < -0.2f) held.add(Input.Keys.S);
        if (fy > 0.2f) held.add(Input.Keys.W);
        if (close >= 2 && p.dashCooldown <= 0f) justPressed.add(Input.Keys.SPACE);
        // arma de maior poder
        int best = 0;
        for (int i = 1; i < p.weapons.size; i++) if (p.weapons.get(i).score() > p.weapons.get(best).score()) best = i;
        p.selectWeapon(best);
    }

    /** Loja entre ondas: Torre escolhe uma carta aleatoria; os outros modos compram upgrades. */
    private void shop(World w, GameModeConfig cfg) {
        Player p = w.player;
        if (cfg.isTower()) {
            Array<Draft.Option> opts = Draft.roll(p, w);
            if (opts.size > 0) opts.random().apply();
            return;
        }
        boolean bought = true;
        while (bought) {
            bought = false;
            // prioriza dano e escudo; reparo so quando precisa
            Upgrade[] order = { Upgrade.DAMAGE, Upgrade.SHIELD, Upgrade.HULL, Upgrade.ENGINE };
            for (Upgrade u : order) {
                int lvl = p.upgradeLevels[u.ordinal()];
                int cost = u.cost(lvl);
                if (!u.isMaxed(lvl) && p.credits >= cost) {
                    p.credits -= cost;
                    p.applyUpgrade(u);
                    bought = true;
                    break;
                }
            }
        }
    }

    private boolean anomaly(World w) {
        Player p = w.player;
        if (Float.isNaN(p.pos.x) || Float.isNaN(p.hp) || Float.isNaN(p.shield)) return why("NaN no jogador");
        if (w.enemies.size > com.fonseca.novafrontier.systems.WaveManager.MAX_ALIVE + 8) {
            return why("inimigos vivos: " + w.enemies.size);
        }
        for (int i = 0; i < w.enemies.size; i++) {
            Enemy e = w.enemies.get(i);
            if (Float.isNaN(e.pos.x) || e.pos.x < -1f || e.pos.x > Constants.WORLD_W + 1f
                    || e.pos.y < -1f || e.pos.y > Constants.WORLD_H + 1f) return why("inimigo fora do mapa: " + e.type + " " + e.pos);
        }
        return false;
    }

    private boolean why(String msg) {
        if (System.getProperty("nf.debug") != null) System.out.println("  anomalia: " + msg);
        return true;
    }

    /** Execucao pelo terminal: java ... BalanceSimulator [partidas]. */
    public static void main(String[] args) {
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        BalanceSimulator sim = new BalanceSimulator();
        System.out.println("=== Simulacao de balanceamento (" + runs + " partidas por configuracao) ===");
        for (Difficulty d : Difficulty.values()) System.out.println(sim.simulate(Mode.OPERACAO, d, runs, 100));
        for (Difficulty d : Difficulty.values()) System.out.println(sim.simulate(Mode.INFINITO, d, runs, 200));
        System.out.println(sim.simulate(Mode.TORRE, Difficulty.DIFICIL, runs, 300));
    }
}
