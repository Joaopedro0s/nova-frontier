package com.fonseca.novafrontier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.fonseca.novafrontier.entities.Ally;
import com.fonseca.novafrontier.entities.Boss;
import com.fonseca.novafrontier.entities.Bullet;
import com.fonseca.novafrontier.entities.Enemy;
import com.fonseca.novafrontier.entities.Player;
import com.fonseca.novafrontier.items.PassiveModule;
import com.fonseca.novafrontier.items.Rarity;
import com.fonseca.novafrontier.items.Upgrade;
import com.fonseca.novafrontier.items.Weapon;
import com.fonseca.novafrontier.items.WeaponAffix;
import com.fonseca.novafrontier.items.WeaponType;
import com.fonseca.novafrontier.systems.AchievementManager;
import com.fonseca.novafrontier.systems.AchievementManager.Ach;
import com.fonseca.novafrontier.systems.Draft;
import com.fonseca.novafrontier.systems.GameModeConfig;
import com.fonseca.novafrontier.systems.GameModeConfig.Difficulty;
import com.fonseca.novafrontier.systems.GameModeConfig.Mode;
import com.fonseca.novafrontier.systems.GameModeConfig.Mutator;
import com.fonseca.novafrontier.systems.ProfileManager;
import com.fonseca.novafrontier.systems.World;

import org.junit.Before;
import org.junit.Test;

/** Regras da expansao: afixos, modulos, mutators, dificuldades, postura, conquistas e perfil. */
public class ExpansionRulesTest {
    private static final float DT = 1f / 60f;

    @Before
    public void setUp() {
        new BalanceSimulator(); // instala Gdx.input falso e perfil em memoria
        MathUtils.random = new RandomXS128(42);
    }

    private static World world(Mode m, Difficulty d) {
        ProfileManager.get().data = new ProfileManager.Data(); // isola conquistas/perfil entre mundos
        return new World(new GameModeConfig(m, d));
    }

    private static void invincible(World w) {
        w.player.maxHp = w.player.hp = 1e7f;
    }

    // ------------------------------------------------------------------ afixos
    @Test
    public void afixosPorRaridade() {
        for (int i = 0; i < 50; i++) {
            assertEquals(0, Weapon.rolled(WeaponType.BLASTER, Rarity.COMMON).affixes.size());
            assertEquals(1, Weapon.rolled(WeaponType.BLASTER, Rarity.RARE).affixes.size());
            assertEquals(2, Weapon.rolled(WeaponType.BLASTER, Rarity.EPIC).affixes.size());
            assertEquals(3, Weapon.rolled(WeaponType.BLASTER, Rarity.LEGENDARY).affixes.size());
        }
        assertEquals(0, new Weapon(WeaponType.LASER, Rarity.RARE).affixes.size()); // armas dos aliados
    }

    @Test
    public void afixosAlteramProjeteis() {
        World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
        Weapon wp = new Weapon(WeaponType.BLASTER, Rarity.RARE);
        wp.affixes.add(WeaponAffix.CALIBRADA);
        wp.affixes.add(WeaponAffix.PERFURA);
        wp.affixes.add(WeaponAffix.CRIOGENICA);
        wp.affixes.add(WeaponAffix.VAMPIRICA);
        wp.fire(w, w.player.pos, 0f, 1f, true);
        Bullet b = w.bullets.peek();
        assertEquals(0.12f, b.critBonus, 1e-6);
        assertEquals(WeaponType.BLASTER.pierce + 1, b.pierce);
        assertTrue(b.slow);
        assertTrue(b.vamp);
        // sem afixos nada muda
        w.bullets.clear();
        new Weapon(WeaponType.BLASTER, Rarity.RARE).fire(w, w.player.pos, 0f, 1f, true);
        Bullet plain = w.bullets.peek();
        assertEquals(0f, plain.critBonus, 1e-6);
        assertEquals(0, plain.pierce);
        assertFalse(plain.slow);
    }

    @Test
    public void criogenicaReduzVelocidade() {
        World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
        invincible(w);
        w.waves.startWave(1);
        w.waves.clearQueue();
        Enemy e = w.spawnEnemy(Enemy.Type.BRUTE, w.player.pos.x + 600f, w.player.pos.y);
        Bullet b = w.spawnBullet(e.pos.x, e.pos.y, 0f, 0f, 1f, 6f, true, Palette.WHITE);
        b.slow = true;
        w.update(DT, true);
        assertTrue("alvo deveria estar lento", e.slowTimer > 1.5f);
        w.update(DT, true);
        // tick seguinte aplica -30% de velocidade
        float expected = Enemy.Type.BRUTE.speed * 0.7f;
        java.lang.reflect.Field f;
        try {
            f = Enemy.class.getDeclaredField("speed");
            f.setAccessible(true);
            assertEquals(expected, f.getFloat(e), 1e-3);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void vampiricaCuraAoAbater() {
        World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
        w.waves.startWave(1);
        w.waves.clearQueue();
        w.player.maxHp = 1e6f;
        w.player.hp = 100f;
        w.player.shield = w.player.maxShield; // cheio: a cura vai para a vida
        float start = w.player.hp;
        for (int i = 0; i < 300; i++) {
            Enemy e = w.spawnEnemy(Enemy.Type.SWARMER, w.player.pos.x + 700f, w.player.pos.y);
            Bullet b = w.spawnBullet(e.pos.x, e.pos.y, 0f, 0f, 9999f, 6f, true, Palette.WHITE);
            b.vamp = true;
            w.update(DT, false);
            w.player.hp = Math.min(w.player.hp, 1e6f);
        }
        assertTrue("300 abates com 15% de chance deveriam curar alguma vez", w.player.hp > start);
        assertEquals("cada cura vale 3 de vida", 0f, (w.player.hp - start) % 3f, 1e-3);
    }

    @Test
    public void arsenalDinamicoSoComAbatePrevio() {
        Player p = new Player();
        p.weapons.add(new Weapon(WeaponType.LASER, Rarity.COMMON));
        p.selectWeapon(1);
        assertEquals("sem abate nao ha buff", 0f, p.arsenalTimer, 1e-6);
        p.onKill();
        p.selectWeapon(0);
        assertEquals(Player.ARSENAL_TIME, p.arsenalTimer, 1e-6);
        p.selectWeapon(1); // sem novo abate: nao renova
        p.arsenalTimer = 0f;
        p.selectWeapon(0);
        assertEquals(0f, p.arsenalTimer, 1e-6);
    }

    // ------------------------------------------------------------------ modulos
    @Test
    public void modulosLimitadosATresSlots() {
        Player p = new Player();
        assertTrue(p.equipModule(PassiveModule.DASH_REACTOR));
        assertFalse("repetido", p.equipModule(PassiveModule.DASH_REACTOR));
        assertTrue(p.equipModule(PassiveModule.NEURAL_LINK));
        assertTrue(p.equipModule(PassiveModule.MANUAL_LENS));
        assertFalse("sem slot", p.equipModule(PassiveModule.SHIELD_CAPACITOR));
        assertEquals(null, p.nextModuleDrop());
    }

    @Test
    public void linkNeuralELenteManualEIonica() {
        World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
        assertEquals(0.5f, w.allyDamageMult(), 1e-6);
        w.player.damageMult = 1.4f;
        w.player.equipModule(PassiveModule.NEURAL_LINK);
        assertEquals(0.5f * (1f + 0.5f * 0.4f), w.allyDamageMult(), 1e-6);

        assertEquals(2f, w.critMult(), 1e-6);
        w.player.equipModule(PassiveModule.MANUAL_LENS);
        w.player.autoAim = true;
        assertEquals("com auto-mira nao ha bonus", 2f, w.critMult(), 1e-6);
        w.player.autoAim = false;
        assertEquals(2.6f, w.critMult(), 1e-6);

        World t = world(Mode.TORRE, Difficulty.DIFICIL);
        for (int i = 0; i < 200 && t.mutator != Mutator.ION_STORM; i++) t.prepareWave(1);
        assertEquals(Mutator.ION_STORM, t.mutator);
        assertEquals(2.5f, t.critMult(), 1e-6);
    }

    @Test
    public void capacitorDeEscudoEmpurraEZeraODash() {
        for (boolean withModule : new boolean[] { false, true }) {
            World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
            w.waves.startWave(1);
            w.waves.clearQueue();
            if (withModule) w.player.equipModule(PassiveModule.SHIELD_CAPACITOR);
            w.player.shield = 1f;
            w.player.dashCooldown = 1f;
            Enemy e = w.spawnEnemy(Enemy.Type.DRONE, w.player.pos.x + 20f, w.player.pos.y);
            float before = e.pos.dst(w.player.pos);
            w.update(DT, true);
            if (withModule) {
                assertTrue("dash deveria ser resetado", w.player.dashCooldown <= 0f);
                assertTrue("inimigo deveria ser empurrado", e.pos.dst(w.player.pos) > before + 50f);
            } else {
                assertTrue(w.player.dashCooldown > 0.9f);
            }
        }
    }

    @Test
    public void reatorDeDashExplode() {
        World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
        w.waves.startWave(1);
        w.waves.clearQueue();
        Enemy e = w.spawnEnemy(Enemy.Type.BRUTE, 500f, 500f);
        float hp = e.hp;
        w.onPlayerDash(500f, 500f); // sem modulo: nada
        assertEquals(hp, e.hp, 1e-6);
        w.player.equipModule(PassiveModule.DASH_REACTOR);
        w.onPlayerDash(500f, 500f);
        assertTrue("explosao de ~35 de dano", hp - e.hp > 25f && hp - e.hp < 45f);
    }

    // ------------------------------------------------------------------ postura dos aliados
    @Test
    public void posturaCicla() {
        World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
        assertEquals(Ally.Posture.DEFENSIVO, w.posture);
        int lines = w.feed.size();
        w.cyclePosture();
        assertEquals(Ally.Posture.AGRESSIVO, w.posture);
        assertTrue("o chat deveria avisar a mudanca", w.feed.size() > lines);
        w.cyclePosture();
        assertEquals(Ally.Posture.FOCO_BOSS, w.posture);
        w.cyclePosture();
        assertEquals(Ally.Posture.DEFENSIVO, w.posture);
        assertEquals(130f, Ally.Posture.DEFENSIVO.orbit, 1e-6);
        assertEquals(220f, Ally.Posture.AGRESSIVO.orbit, 1e-6);
    }

    @Test
    public void aliadosAgressivosOrbitamMaisLonge() {
        float[] avg = new float[2];
        Ally.Posture[] modes = { Ally.Posture.DEFENSIVO, Ally.Posture.AGRESSIVO };
        for (int m = 0; m < 2; m++) {
            World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
            invincible(w);
            w.posture = modes[m];
            float sum = 0f;
            int n = 0;
            for (int i = 0; i < 600; i++) {
                w.update(DT, false);
                if (i > 300) {
                    for (Ally a : w.allies) {
                        sum += a.pos.dst(w.player.pos);
                        n++;
                    }
                }
            }
            avg[m] = sum / n;
        }
        assertTrue("agressivo (" + avg[1] + ") deveria orbitar mais longe que defensivo (" + avg[0] + ")",
                avg[1] > avg[0] + 40f);
    }

    // ------------------------------------------------------------------ mutators e modos
    @Test
    public void mutatoresDaTorre() {
        World t = world(Mode.TORRE, Difficulty.DIFICIL);
        boolean sawIon = false, sawOverload = false;
        for (int i = 0; i < 300; i++) {
            t.prepareWave(1 + i % 4); // andares comuns
            assertNotEquals(Mutator.NONE, t.mutator);
            if (t.mutator == Mutator.ION_STORM) {
                sawIon = true;
                assertEquals(6f, t.player.shieldDelayMax, 1e-6);
            } else {
                assertEquals(3f, t.player.shieldDelayMax, 1e-6);
            }
            if (t.mutator == Mutator.OVERLOAD) {
                sawOverload = true;
                assertEquals(0.55f, t.player.dashCdMax, 1e-6);
            } else {
                assertEquals(1.1f, t.player.dashCdMax, 1e-6);
            }
        }
        assertTrue(sawIon && sawOverload);
        for (int floor : new int[] { 5, 10, 15, 20 }) {
            t.prepareWave(floor);
            assertEquals("andar de boss sem mutator", Mutator.NONE, t.mutator);
        }
        World op = world(Mode.OPERACAO, Difficulty.DIFICIL);
        op.prepareWave(2);
        assertEquals(Mutator.NONE, op.mutator);
    }

    private static Array<Enemy.Type> spawnAll(World w, int wave, Mutator m) {
        w.waves.startWave(wave, m);
        for (int i = 0; i < 400 && w.waves.remainingToSpawn() > 0; i++) w.waves.update(1f, w);
        Array<Enemy.Type> types = new Array<>();
        for (Enemy e : w.enemies) types.add(e.type);
        return types;
    }

    @Test
    public void composicaoDosMutators() {
        Array<Enemy.Type> swarm = spawnAll(world(Mode.TORRE, Difficulty.DIFICIL), 8, Mutator.SWARM);
        assertTrue(swarm.size > 0);
        for (Enemy.Type t : swarm) assertTrue(t == Enemy.Type.DRONE || t == Enemy.Type.SWARMER);
        Array<Enemy.Type> armored = spawnAll(world(Mode.TORRE, Difficulty.DIFICIL), 8, Mutator.ARMORED);
        assertTrue(armored.size > 0);
        for (Enemy.Type t : armored) assertTrue(t == Enemy.Type.GUNNER || t == Enemy.Type.BRUTE);
    }

    @Test
    public void pesadeloAntecipaGunnersEBrutes() {
        assertTrue(spawnAll(world(Mode.OPERACAO, Difficulty.HARD), 2, Mutator.NONE).contains(Enemy.Type.GUNNER, true));
        assertFalse(spawnAll(world(Mode.OPERACAO, Difficulty.DIFICIL), 2, Mutator.NONE).contains(Enemy.Type.GUNNER, true));
        assertTrue(spawnAll(world(Mode.OPERACAO, Difficulty.HARD), 4, Mutator.NONE).contains(Enemy.Type.BRUTE, true));
        assertFalse(spawnAll(world(Mode.OPERACAO, Difficulty.DIFICIL), 4, Mutator.NONE).contains(Enemy.Type.BRUTE, true));
    }

    @Test
    public void pesadeloAjustaProjeteisDropsBossEScore() {
        World hard = world(Mode.OPERACAO, Difficulty.HARD);
        hard.waves.startWave(1);
        hard.waves.clearQueue();
        Bullet b = hard.spawnEnemyBullet(0f, 0f, 0f, 100f, 10f, 5f);
        assertEquals(120f, b.vel.len(), 1e-3);
        assertEquals(12.5f, b.damage, 1e-4);
        hard.debugLegendary();
        assertEquals(15f, hard.pickups.peek().life, 1e-6);

        Boss boss = (Boss) hard.spawnEnemy(Enemy.Type.COLOSSUS, 400f, 400f);
        boss.hp = boss.maxHp * 0.6f;
        assertTrue("fase 2 aos 65%", boss.enraged());
        boss.hp = boss.maxHp * 0.7f;
        assertFalse(boss.enraged());

        assertEquals(150, hard.score.kill(100));
        World normal = world(Mode.OPERACAO, Difficulty.DIFICIL);
        assertEquals(100, normal.score.kill(100));
        normal.waves.startWave(1);
        Boss nb = (Boss) normal.spawnEnemy(Enemy.Type.COLOSSUS, 400f, 400f);
        nb.hp = nb.maxHp * 0.6f;
        assertFalse("normal: fase 2 so abaixo de 50%", nb.enraged());
        nb.hp = nb.maxHp * 0.45f;
        assertTrue(nb.enraged());
    }

    @Test
    public void danoInimigoEscalaNoInfinito() {
        World w = world(Mode.INFINITO, Difficulty.DIFICIL);
        w.waves.startWave(10);
        assertEquals(1f, w.enemyDmgMult(), 1e-6);
        w.waves.startWave(20);
        assertEquals(1.5f, w.enemyDmgMult(), 1e-6);
        Bullet b = w.spawnEnemyBullet(0f, 0f, 0f, 100f, 10f, 5f);
        assertEquals(15f, b.damage, 1e-4);
    }

    @Test
    public void anticamperNoInfinito() {
        for (Mode m : new Mode[] { Mode.INFINITO, Mode.OPERACAO }) {
            World w = world(m, Difficulty.DIFICIL);
            invincible(w);
            w.waves.startWave(11);
            for (int i = 0; i < 56; i++) w.spawnEnemy(Enemy.Type.DRONE, 60f + i, 60f); // longe e fora da tela
            for (int i = 0; i < 60 * 50; i++) {
                // mantem o teto de 55 vivos e os inimigos longe, como num kiting
                for (Enemy e : w.enemies) if (e.pos.dst(w.player.pos) < 700f) e.pos.set(60f, 60f);
                w.waves.update(DT, w);
            }
            boolean rage = w.waves.enraged();
            if (m == Mode.INFINITO) {
                assertTrue("deveria enfurecer apos 45s com 55 vivos", rage);
                int raging = 0;
                for (Enemy e : w.enemies) if (e.rage) raging++;
                assertEquals(w.enemies.size, raging);
            } else {
                assertFalse("so existe no Modo Infinito", rage);
            }
        }
    }

    @Test
    public void motorLimitadoAoNivel5() {
        assertFalse(Upgrade.ENGINE.isMaxed(4));
        assertTrue(Upgrade.ENGINE.isMaxed(5));
        assertFalse(Upgrade.DAMAGE.isMaxed(50));
        World w = world(Mode.TORRE, Difficulty.DIFICIL);
        w.player.upgradeLevels[Upgrade.ENGINE.ordinal()] = 5;
        for (int i = 0; i < 100; i++) {
            for (Draft.Option o : Draft.roll(w.player, w)) assertNotEquals("+1 " + Upgrade.ENGINE.label, o.title);
        }
    }

    @Test
    public void draftOferece3OpcoesDistintasEAplicaUma() {
        World w = world(Mode.TORRE, Difficulty.DIFICIL);
        Array<Draft.Option> opts = Draft.roll(w.player, w);
        assertEquals(3, opts.size);
        assertNotEquals(opts.get(0).title, opts.get(1).title);
        assertNotEquals(opts.get(1).title, opts.get(2).title);
        // "Reparo + Arma": cura 50% e sobe a raridade
        Draft.Option repair = null;
        for (int i = 0; i < 200 && repair == null; i++) {
            for (Draft.Option o : Draft.roll(w.player, w)) if (o.title.startsWith("Reparo")) repair = o;
        }
        w.player.hp = 10f;
        Rarity before = w.player.weapon().rarity;
        repair.apply();
        assertEquals(60f, w.player.hp, 1e-3); // 10 + 50% de 100
        assertEquals(Rarity.values()[before.ordinal() + 1], w.player.weapon().rarity);
    }

    @Test
    public void armaInicialEBossGaranteModulo() {
        GameModeConfig cfg = new GameModeConfig(Mode.OPERACAO, Difficulty.DIFICIL);
        cfg.startWeapon = WeaponType.PLASMA;
        ProfileManager.get().data = new ProfileManager.Data();
        World w = new World(cfg);
        assertEquals(WeaponType.PLASMA, w.player.weapon().type);
        assertEquals(Rarity.COMMON, w.player.weapon().rarity);

        w.waves.startWave(5);
        w.waves.clearQueue();
        Boss boss = (Boss) w.spawnEnemy(Enemy.Type.COLOSSUS, w.player.pos.x + 500f, w.player.pos.y);
        w.spawnBullet(boss.pos.x, boss.pos.y, 0f, 0f, 1e9f, 6f, true, Palette.WHITE);
        w.update(DT, false);
        boolean module = false;
        for (com.fonseca.novafrontier.entities.Pickup p : w.pickups) {
            if (p.kind == com.fonseca.novafrontier.entities.Pickup.Kind.MODULE) module = true;
        }
        assertTrue("boss derrotado deve soltar um modulo", module);
        assertEquals(1, w.bossesKilled);
    }

    // ------------------------------------------------------------------ conquistas
    @Test
    public void conquistasDeVitoria() {
        World w = world(Mode.OPERACAO, Difficulty.HARD);
        w.waves.startWave(10);
        w.endWave();
        assertTrue(w.achievements.has(Ach.PRIMEIRA_LIMPEZA));
        assertTrue(w.achievements.has(Ach.VETERANO));
        assertFalse(w.achievements.has(Ach.CONQUISTADOR_TORRE));

        World n = world(Mode.OPERACAO, Difficulty.DIFICIL);
        n.waves.startWave(10);
        n.endWave();
        assertTrue(n.achievements.has(Ach.PRIMEIRA_LIMPEZA));

        World t = world(Mode.TORRE, Difficulty.DIFICIL);
        t.waves.startWave(20);
        t.endWave();
        assertTrue(t.achievements.has(Ach.CONQUISTADOR_TORRE));

        World i = world(Mode.INFINITO, Difficulty.DIFICIL);
        i.beginWave(19);
        assertFalse(i.achievements.has(Ach.SOBREVIVENTE));
        i.beginWave(20);
        assertTrue(i.achievements.has(Ach.SOBREVIVENTE));
    }

    @Test
    public void conquistaMiraLaser() {
        World off = world(Mode.OPERACAO, Difficulty.DIFICIL);
        off.player.autoAim = false;
        off.beginWave(5);
        off.waves.clearQueue();
        off.update(DT, true);
        off.endWave();
        assertTrue(off.achievements.has(Ach.MIRA_LASER));

        World on = world(Mode.OPERACAO, Difficulty.DIFICIL);
        invincible(on);
        on.beginWave(5);
        on.waves.clearQueue();
        on.update(DT, true); // auto-mira ligada durante a onda
        on.endWave();
        assertFalse(on.achievements.has(Ach.MIRA_LASER));
    }

    @Test
    public void conquistaIntocavel() {
        for (boolean hurt : new boolean[] { false, true }) {
            World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
            w.waves.startWave(5);
            w.waves.clearQueue();
            Boss boss = (Boss) w.spawnEnemy(Enemy.Type.COLOSSUS, w.player.pos.x + 600f, w.player.pos.y);
            if (hurt) w.player.takeHit(w.player.shield + 20f); // perde vida
            else w.player.takeHit(10f); // so escudo
            w.spawnBullet(boss.pos.x, boss.pos.y, 0f, 0f, 1e9f, 6f, true, Palette.WHITE);
            w.update(DT, false);
            assertEquals(hurt ? "perdeu vida: sem conquista" : "so escudo: conquista", !hurt,
                    w.achievements.has(Ach.INTOCAVEL));
        }
    }

    @Test
    public void conquistaColecionadorLendario() {
        World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
        w.player.weapons.add(Weapon.rolled(WeaponType.LASER, Rarity.LEGENDARY));
        w.checkLegendaryCollector();
        assertFalse(w.achievements.has(Ach.COLECIONADOR_LENDARIO));
        w.player.weapons.add(Weapon.rolled(WeaponType.PLASMA, Rarity.LEGENDARY));
        w.checkLegendaryCollector();
        assertTrue(w.achievements.has(Ach.COLECIONADOR_LENDARIO));
    }

    @Test
    public void conquistaComboMaximoEAlertas() {
        World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
        AchievementManager a = w.achievements;
        for (int i = 0; i < 14 * 60; i++) a.update(DT, 5);
        assertFalse(a.has(Ach.COMBO_MAXIMO));
        a.update(DT, 4); // quebrou o combo: recomeca
        for (int i = 0; i < 14 * 60; i++) a.update(DT, 5);
        assertFalse(a.has(Ach.COMBO_MAXIMO));
        for (int i = 0; i < 2 * 60; i++) a.update(DT, 5);
        assertTrue(a.has(Ach.COMBO_MAXIMO));

        // alerta no HUD e mensagem comemorativa dos aliados
        int lines = w.feed.size();
        w.update(DT, false);
        assertTrue(w.achBannerTimer > 0f && w.achBanner.contains("Combo Maximo"));
        assertTrue(w.feed.size() >= lines + 2);
        assertEquals(1, w.unlockedThisRun.size);
    }

    @Test
    public void debugDesligaConquistas() {
        World w = world(Mode.OPERACAO, Difficulty.DIFICIL);
        w.waves.startWave(10);
        w.debugCredits();
        assertTrue(w.debugUsed);
        w.endWave();
        assertFalse(w.achievements.has(Ach.PRIMEIRA_LIMPEZA));
    }

    // ------------------------------------------------------------------ perfil
    @Test
    public void perfilSerializaEmJson() {
        ProfileManager p = ProfileManager.get();
        p.data.bestScore = 12345;
        p.data.dados = 77;
        p.data.skinMask = 5;
        p.data.achMask = 9;
        p.data.volSfx = 30;
        Json json = new Json();
        ProfileManager.Data back = json.fromJson(ProfileManager.Data.class, p.toJson());
        assertEquals(12345, back.bestScore);
        assertEquals(77, back.dados);
        assertEquals(5, back.skinMask);
        assertEquals(9, back.achMask);
        assertEquals(30, back.volSfx);
    }

    @Test
    public void hangarCompraELimites() {
        ProfileManager p = ProfileManager.get();
        assertFalse(p.buyMagnet());
        p.data.dados = 1000;
        assertEquals(150f, p.magnetRadius(), 1e-6);
        assertEquals(10f, p.reviveTime(), 1e-6);
        assertTrue(p.buyMagnet());
        assertTrue(p.buyMagnet());
        assertTrue(p.buyMagnet());
        assertFalse("maximo 3 niveis", p.buyMagnet());
        assertEquals(225f, p.magnetRadius(), 1e-6);
        for (int i = 0; i < 3; i++) assertTrue(p.buyRevive());
        assertFalse(p.buyRevive());
        assertEquals(7f, p.reviveTime(), 1e-6);
        assertEquals(1000 - 190 - 190, p.data.dados);

        assertEquals(WeaponType.BLASTER, p.startWeapon());
        p.data.startWeapon = WeaponType.LASER.ordinal();
        assertEquals("sem desbloquear volta ao Blaster", WeaponType.BLASTER, p.startWeapon());
        assertTrue(p.unlockWeapon(WeaponType.LASER));
        assertFalse(p.unlockWeapon(WeaponType.LASER));
        assertEquals(WeaponType.LASER, p.startWeapon());

        assertEquals(0, p.skin());
        assertTrue(p.unlockSkin(2));
        p.data.skin = 2;
        assertEquals(2, p.skin());
        assertFalse("sem desbloquear volta ao padrao", p.isSkinUnlocked(3));

        // o hangar vale na proxima run
        World w = new World(new GameModeConfig(Mode.OPERACAO, Difficulty.DIFICIL));
        assertEquals(7f, w.allies.get(0).respawnTime, 1e-6);
        assertEquals(2, w.player.skin);
    }

    @Test
    public void dadosEstelaresComMultiplicador() {
        assertEquals(150, ProfileManager.starData(10, 2, Difficulty.DIFICIL.rewardMult));
        assertEquals(225, ProfileManager.starData(10, 2, Difficulty.HARD.rewardMult));
        assertEquals(113, ProfileManager.starData(10, 2, Difficulty.FACIL.rewardMult));
        assertEquals(0, ProfileManager.starData(0, 0, 1.5f));
    }
}
