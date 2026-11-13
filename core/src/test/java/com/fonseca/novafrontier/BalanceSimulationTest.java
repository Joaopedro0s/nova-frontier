package com.fonseca.novafrontier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fonseca.novafrontier.BalanceSimulator.Result;
import com.fonseca.novafrontier.systems.GameModeConfig;
import com.fonseca.novafrontier.systems.GameModeConfig.Difficulty;
import com.fonseca.novafrontier.systems.GameModeConfig.Mode;
import com.fonseca.novafrontier.systems.ProfileManager;

import org.junit.Test;

/**
 * Valida as formulas dos modos/dificuldades e roda partidas simuladas (headless) imprimindo a taxa
 * de sobrevivencia e a onda media de morte. Para mais partidas: ./gradlew test -Dnf.runs=20
 */
public class BalanceSimulationTest {
    private static final int RUNS = Integer.getInteger("nf.runs", 4);

    @Test
    public void formulasDoModoInfinito() {
        GameModeConfig c = new GameModeConfig(Mode.INFINITO, Difficulty.DIFICIL);
        // ate a onda 10 segue a progressao normal
        assertEquals(1f + 0.15f * 9, c.hpScale(10), 1e-4);
        // a partir da 11: base * 1.06^(onda - 10)
        assertEquals((1f + 0.15f * 14) * Math.pow(1.06, 5), c.hpScale(15), 1e-3);
        // dano +5% por onda depois da 10
        assertEquals(1f, c.dmgScale(10), 1e-6);
        assertEquals(1.25f, c.dmgScale(15), 1e-6);
        // bosses: 15 Colossus, 20 Rainha, 25+ os dois
        assertEquals(1, c.bosses(15).length);
        assertEquals(1, c.bosses(20).length);
        assertEquals(2, c.bosses(25).length);
        assertEquals(0, c.bosses(17).length);
    }

    @Test
    public void formulasDasDificuldades() {
        GameModeConfig hard = new GameModeConfig(Mode.OPERACAO, Difficulty.HARD);
        GameModeConfig easy = new GameModeConfig(Mode.OPERACAO, Difficulty.FACIL);
        GameModeConfig normal = new GameModeConfig(Mode.OPERACAO, Difficulty.DIFICIL);
        assertEquals(normal.hpScale(4) * 1.35f, hard.hpScale(4), 1e-4);
        assertEquals(normal.hpScale(4) * 0.8f, easy.hpScale(4), 1e-4);
        assertEquals(1.25f, hard.dmgScale(1), 1e-6);
        assertEquals(0.65f, Difficulty.HARD.bossPhase2, 1e-6);
        assertEquals(2, Difficulty.HARD.gunnerWave);
        assertEquals(4, Difficulty.HARD.bruteWave);
        assertEquals(15f, Difficulty.HARD.dropLife, 1e-6);
        // a Torre ignora a dificuldade
        assertEquals(Difficulty.DIFICIL, new GameModeConfig(Mode.TORRE, Difficulty.HARD).diff);
        // Torre: bosses 5, 10, 15 e duplo no 20
        GameModeConfig tower = new GameModeConfig(Mode.TORRE, Difficulty.DIFICIL);
        assertEquals(1, tower.bosses(5).length);
        assertEquals(1, tower.bosses(10).length);
        assertEquals(1, tower.bosses(15).length);
        assertEquals(2, tower.bosses(20).length);
    }

    @Test
    public void dadosEstelares() {
        // (ondas x 10 + bosses x 25) x multiplicador
        assertEquals(150, ProfileManager.starData(10, 2, 1f));
        assertEquals(225, ProfileManager.starData(10, 2, 1.5f));
    }

    @Test
    public void simulaTodosOsModos() {
        BalanceSimulator sim = new BalanceSimulator();
        System.out.println("=== Simulacao de balanceamento (" + RUNS + " partidas por configuracao) ===");
        Result[] op = new Result[3];
        for (Difficulty d : Difficulty.values()) {
            op[d.ordinal()] = sim.simulate(Mode.OPERACAO, d, RUNS, 100);
            System.out.println(op[d.ordinal()]);
        }
        Result[] inf = new Result[3];
        for (Difficulty d : Difficulty.values()) {
            inf[d.ordinal()] = sim.simulate(Mode.INFINITO, d, RUNS, 200);
            System.out.println(inf[d.ordinal()]);
        }
        Result tower = sim.simulate(Mode.TORRE, Difficulty.DIFICIL, RUNS, 300);
        System.out.println(tower);

        for (Result r : new Result[] { op[0], op[1], op[2], inf[0], inf[1], inf[2], tower }) {
            assertEquals("estado invalido (NaN/fora do mapa) em " + r.label, 0, r.anomalies);
            assertTrue(r.survivalRate() >= 0 && r.survivalRate() <= 1);
        }
        // mais facil nunca deve render menos ondas limpas que o Pesadelo
        assertTrue("Recruta deveria limpar pelo menos tantas ondas quanto Pesadelo",
                op[Difficulty.FACIL.ordinal()].avgWavesCleared() >= op[Difficulty.HARD.ordinal()].avgWavesCleared());
        assertTrue("Infinito Recruta deveria ir ao menos tao longe quanto Pesadelo",
                inf[Difficulty.FACIL.ordinal()].avgWavesCleared() >= inf[Difficulty.HARD.ordinal()].avgWavesCleared());
    }
}
