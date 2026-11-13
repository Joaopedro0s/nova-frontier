package com.fonseca.novafrontier.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.NovaFrontierGame;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.entities.Ally;
import com.fonseca.novafrontier.entities.Boss;
import com.fonseca.novafrontier.entities.Enemy;
import com.fonseca.novafrontier.entities.Player;
import com.fonseca.novafrontier.items.PassiveModule;
import com.fonseca.novafrontier.items.Upgrade;
import com.fonseca.novafrontier.items.Weapon;
import com.fonseca.novafrontier.systems.Draft;
import com.fonseca.novafrontier.systems.GameModeConfig;
import com.fonseca.novafrontier.systems.ProfileManager;
import com.fonseca.novafrontier.systems.SoundManager;
import com.fonseca.novafrontier.systems.World;
import com.fonseca.novafrontier.ui.OptionsPanel;

import java.util.Locale;

/** Tela de jogo: entrada, camera, estados (intro/luta/loja/pausa) e HUD estilo MMO. */
public class GameScreen extends BaseScreen {

    private enum State { INTRO, FIGHT, SHOP, PAUSED }

    private static final float SHAKE_TIME = 0.35f;

    private final GameModeConfig cfg;
    private final World world;
    private final OrthographicCamera hudCam = new OrthographicCamera(Constants.VIEW_W, Constants.VIEW_H);
    private final Vector2 mouseView = new Vector2();
    private final OptionsPanel options = new OptionsPanel();

    private State state = State.INTRO;
    private State beforePause = State.FIGHT;
    private float stateTimer;
    private int wave = 1;
    private int lastBonus;
    private float camX, camY;
    private Array<Draft.Option> draft = new Array<>();
    private boolean debug;

    public GameScreen(NovaFrontierGame game, GameModeConfig cfg) {
        super(game);
        this.cfg = cfg;
        this.world = new World(cfg);
        hudCam.position.set(Constants.VIEW_W / 2f, Constants.VIEW_H / 2f, 0f);
        hudCam.update();
        camX = world.player.pos.x;
        camY = world.player.pos.y;
        beginIntro();
    }

    // ================================================================== fluxo
    private void beginIntro() {
        state = State.INTRO;
        stateTimer = 2.8f;
        world.prepareWave(wave);
    }

    /** Fecha a run: calcula Dados Estelares, grava o perfil e abre a tela final. */
    private void finish(boolean won) {
        ProfileManager profile = world.profile;
        boolean record = world.score.commitBest();
        int dados = world.debugUsed ? 0
                : ProfileManager.starData(world.wavesCleared, world.bossesKilled, cfg.diff.rewardMult);
        profile.data.dados += dados;
        profile.data.runs++;
        if (!world.debugUsed) {
            if (cfg.isEndless()) profile.data.bestWaveEndless = Math.max(profile.data.bestWaveEndless, wave);
            if (cfg.isTower()) profile.data.bestFloor = Math.max(profile.data.bestFloor, won ? wave : wave - 1);
        }
        profile.save();
        game.setScreen(new GameOverScreen(game, cfg, world.score.getScore(), world.score.getBest(), record,
                wave, won, dados, world.bossesKilled, world.debugUsed, world.unlockedThisRun));
    }

    private void onWaveCleared() {
        lastBonus = wave * 100;
        world.score.addBonus(lastBonus);
        world.endWave();
        if (wave >= cfg.totalWaves()) {
            finish(true);
            return;
        }
        if (cfg.isTower()) draft = Draft.roll(world.player, world);
        state = State.SHOP;
    }

    // ================================================================== update
    @Override
    protected void update(float dt) {
        Player p = world.player;
        updateCamera(dt);
        Vector3 m = mouse();
        p.aim.set(m.x, m.y);
        if (p.autoAim) {
            Enemy near = world.nearestEnemy(p.pos, 800f);
            if (near != null) p.aim.set(near.pos);
        }
        mouseView.set(p.aim.x - (camX - Constants.VIEW_W / 2f), p.aim.y - (camY - Constants.VIEW_H / 2f));
        if (world.shake > 0f) world.shake -= dt;

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) debug = !debug;
        if (debug) debugKeys();
        if ((state == State.INTRO || state == State.FIGHT) && Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            world.cyclePosture();
        }

        switch (state) {
            case INTRO:
                if (pausePressed()) { beforePause = state; state = State.PAUSED; break; }
                world.update(dt, false);
                stateTimer -= dt;
                if (stateTimer <= 0f) {
                    world.beginWave(wave);
                    state = State.FIGHT;
                }
                break;
            case FIGHT:
                if (pausePressed()) { beforePause = state; state = State.PAUSED; break; }
                world.update(dt, true);
                if (!p.alive()) finish(false);
                else if (world.waves.isCleared(world)) onWaveCleared();
                break;
            case SHOP:
                world.feed.update(dt);
                if (cfg.isTower()) updateDraft(); else updateShop();
                break;
            case PAUSED:
                options.update();
                if (pausePressed()) state = beforePause;
                else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) game.setScreen(new MenuScreen(game));
                break;
        }
    }

    /** Atalhos do overlay de debug (teclado numerico). */
    private void debugKeys() {
        if (state == State.FIGHT) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) world.debugSkipWave();
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) world.debugSpawnBoss();
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_4)) world.debugLegendary();
        }
        if ((state == State.FIGHT || state == State.SHOP) && Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) {
            world.debugCredits();
        }
    }

    private boolean pausePressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.P) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
    }

    private void updateCamera(float dt) {
        Player p = world.player;
        float tx = p.pos.x + (p.aim.x - p.pos.x) * 0.15f;
        float ty = p.pos.y + (p.aim.y - p.pos.y) * 0.15f;
        float k = Math.min(1f, 6f * dt);
        camX += (tx - camX) * k;
        camY += (ty - camY) * k;
        camX = MathUtils.clamp(camX, Constants.VIEW_W / 2f, Constants.WORLD_W - Constants.VIEW_W / 2f);
        camY = MathUtils.clamp(camY, Constants.VIEW_H / 2f, Constants.WORLD_H - Constants.VIEW_H / 2f);
        float mag = world.shake > 0f ? 8f * Math.min(1f, world.shake / SHAKE_TIME) * world.profile.shakeScale() : 0f;
        camera.position.set(camX + MathUtils.random(-mag, mag), camY + MathUtils.random(-mag, mag), 0f);
        camera.update();
    }

    private void updateShop() {
        Player p = world.player;
        Upgrade[] ups = Upgrade.values();
        for (int i = 0; i < ups.length; i++) {
            if (!Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) continue;
            Upgrade u = ups[i];
            if (u.isMaxed(p.upgradeLevels[i])) {
                world.feed.post(u.label + " ja esta no nivel maximo", Palette.RED);
                continue;
            }
            int cost = u.cost(p.upgradeLevels[i]);
            if (p.credits >= cost) {
                p.credits -= cost;
                p.applyUpgrade(u);
                world.feed.system("Comprou: " + u.label);
                SoundManager.play(SoundManager.Sfx.SHOP);
            } else {
                world.feed.post("Creditos insuficientes para " + u.label, Palette.RED);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            wave++;
            beginIntro();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    /** Draft da Torre: teclas 1-3 escolhem uma opcao gratuita e sobem de andar. */
    private void updateDraft() {
        for (int i = 0; i < draft.size; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                draft.get(i).apply();
                SoundManager.play(SoundManager.Sfx.SHOP);
                wave++;
                beginIntro();
                return;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MenuScreen(game));
    }

    // ================================================================== draw
    @Override
    protected void draw(float dt) {
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(assets.floor, 0, 0, Constants.WORLD_W, Constants.WORLD_H,
                   0, 0, (int) (Constants.WORLD_W / 3), (int) (Constants.WORLD_H / 3), false, false);
        float t = 10f;
        assets.rect(batch, 0, 0, Constants.WORLD_W, t, Palette.CYAN, 0.35f);
        assets.rect(batch, 0, Constants.WORLD_H - t, Constants.WORLD_W, t, Palette.CYAN, 0.35f);
        assets.rect(batch, 0, 0, t, Constants.WORLD_H, Palette.CYAN, 0.35f);
        assets.rect(batch, Constants.WORLD_W - t, 0, t, Constants.WORLD_H, Palette.CYAN, 0.35f);

        world.render(batch, assets, time);

        batch.setProjectionMatrix(hudCam.combined);
        drawHud();
        switch (state) {
            case INTRO: drawIntro(); break;
            case SHOP: if (cfg.isTower()) drawDraft(); else drawShop(); break;
            case PAUSED: drawPause(); break;
            default: break;
        }
        if (debug) drawDebug();
        drawCrosshair();
    }

    private void drawCrosshair() {
        float x = mouseView.x, y = mouseView.y;
        assets.ring(batch, x, y, 30f, Palette.WHITE, 0.8f);
        assets.rect(batch, x - 1f, y - 1f, 3f, 3f, Palette.WHITE, 1f);
    }

    private String progressLabel() {
        String unit = cfg.unit();
        if (cfg.isEndless()) return unit + " " + wave;
        return unit + " " + wave + "/" + cfg.totalWaves();
    }

    private void drawHud() {
        Player p = world.player;
        float W = Constants.VIEW_W;

        // --- vida e escudo
        assets.bar(batch, 14f, 612f, 230f, 16f, p.hp / p.maxHp, Palette.RED);
        assets.text(batch, "VIDA " + (int) Math.max(0f, p.hp) + "/" + (int) p.maxHp, 20f, 626f, 0.9f, Palette.WHITE);
        assets.bar(batch, 14f, 590f, 230f, 14f, p.shield / p.maxShield, Palette.CYAN);
        assets.text(batch, "ESCUDO " + (int) p.shield + "/" + (int) p.maxShield, 20f, 603f, 0.85f, Palette.WHITE);

        // --- armas
        for (int i = 0; i < Player.MAX_WEAPONS; i++) {
            float x = 14f + i * 56f, y = 534f;
            boolean has = i < p.weapons.size;
            boolean sel = has && i == p.current;
            Color border = has ? p.weapons.get(i).rarity.color : Palette.GRAY;
            assets.rect(batch, x - (sel ? 4f : 2f), y - (sel ? 4f : 2f), 48f + (sel ? 8f : 4f), 48f + (sel ? 8f : 4f),
                        sel ? Palette.YELLOW : border, has ? 1f : 0.3f);
            assets.rect(batch, x, y, 48f, 48f, Palette.DARK, 1f);
            if (has) {
                Weapon wp = p.weapons.get(i);
                assets.draw(batch, assets.crate, x + 24f, y + 24f, 30f, 30f, false, wp.color(), 1f);
            }
            assets.text(batch, String.valueOf(i + 1), x + 3f, y + 46f, 0.8f, Palette.GRAY);
        }
        Weapon cw = p.weapon();
        assets.text(batch, cw.name() + "  DMG " + (int) (cw.damage() * p.damageMult), 14f, 522f, 0.9f, cw.rarity.color);
        if (cw.affixes.size() > 0) assets.text(batch, cw.affixText(), 14f, 508f, 0.8f, Palette.WHITE);
        if (p.arsenalTimer > 0f) {
            assets.text(batch, String.format(Locale.ROOT, "ARSENAL DINAMICO +15%% %.1fs", p.arsenalTimer), 14f, 494f,
                        0.8f, Palette.GREEN);
        }
        float dashFrac = 1f - Math.max(0f, p.dashCooldown) / p.dashCdMax;
        assets.bar(batch, 14f, 464f, 110f, 6f, dashFrac, dashFrac >= 1f ? Palette.GREEN : Palette.GRAY);
        assets.text(batch, "DASH [ESPACO]", 14f, 478f, 0.75f, Palette.GRAY);
        assets.text(batch, "AUTO-TIRO [F]: " + (p.autoFire ? "LIGADO" : "desligado"), 14f, 452f, 0.8f,
                    p.autoFire ? Palette.GREEN : Palette.GRAY);
        assets.text(batch, "AUTO-MIRA [T]: " + (p.autoAim ? "LIGADO" : "desligado"), 14f, 436f, 0.8f,
                    p.autoAim ? Palette.GREEN : Palette.GRAY);
        assets.text(batch, "GRUPO [C]: " + world.posture.label, 14f, 420f, 0.8f, Palette.CYAN);
        for (int i = 0; i < p.modules.size; i++) {
            PassiveModule mod = p.modules.get(i);
            assets.text(batch, "MOD " + (i + 1) + ": " + mod.label, 14f, 400f - i * 14f, 0.8f, mod.color);
        }

        // --- onda / pontuacao
        assets.centered(batch, progressLabel(), W / 2f, 634f, 1.6f, Palette.WHITE);
        String sc = String.format(Locale.ROOT, "PONTOS %06d", world.score.getScore());
        assets.centered(batch, sc + (world.score.multiplier() > 1 ? "   x" + world.score.multiplier() : ""),
                        W / 2f, 610f, 1.1f, Palette.YELLOW);
        assets.centered(batch, "CREDITOS " + p.credits, W / 2f, 590f, 1.0f, Palette.ORANGE);
        int left = world.enemies.size + world.waves.remainingToSpawn();
        assets.centered(batch, state == State.FIGHT ? "INIMIGOS: " + left : "", W / 2f, 572f, 0.9f, Palette.GRAY);
        if (world.score.streakFraction() > 0f && world.score.multiplier() > 1) {
            assets.rect(batch, W / 2f - 60f, 560f, 120f * world.score.streakFraction(), 3f, Palette.YELLOW, 0.9f);
        }
        if (world.mutator != GameModeConfig.Mutator.NONE) {
            assets.centered(batch, "MUTATOR: " + world.mutator.label.toUpperCase(Locale.ROOT), W / 2f, 552f, 1.0f,
                            Palette.PURPLE);
        }
        if (world.waves.enraged()) {
            assets.centered(batch, "INIMIGOS ENFURECIDOS (+20% velocidade)", W / 2f, 538f, 0.9f, Palette.RED);
        }

        // --- grupo (party frames)
        float px = W - 226f;
        assets.text(batch, "GRUPO", px, 634f, 0.8f, Palette.GRAY);
        drawPartyRow(px, 618f, "VOCE", 1, p.hp, p.maxHp, p.alive(), 0f, Palette.WHITE);
        for (int i = 0; i < world.allies.size; i++) {
            Ally a = world.allies.get(i);
            drawPartyRow(px, 578f - i * 40f, a.name, a.level, a.hp, a.maxHp, a.alive(),
                         a.downed ? a.downTimer : 0f, Palette.GREEN);
        }

        // --- bosses (ate 2 barras)
        int bi = 0;
        for (int i = 0; i < world.enemies.size && bi < 2; i++) {
            Enemy e = world.enemies.get(i);
            if (!(e instanceof Boss) || !e.alive()) continue;
            Boss boss = (Boss) e;
            float bw = 440f, bx = W / 2f - bw / 2f, off = bi * 52f;
            assets.centered(batch, boss.title() + (boss.enraged() ? "  (FURIA)" : ""), W / 2f, 530f - off, 1.2f,
                            boss.enraged() ? Palette.RED : Palette.WHITE);
            assets.bar(batch, bx, 500f - off, bw, 14f, boss.hp / boss.maxHp, boss.enraged() ? Palette.RED : Palette.ORANGE);
            bi++;
        }

        // --- alerta de conquista
        if (world.achBannerTimer > 0f) {
            float a = Math.min(1f, world.achBannerTimer);
            assets.rect(batch, W / 2f - 260f, 330f, 520f, 40f, Palette.BLACK, 0.7f * a);
            assets.centered(batch, world.achBanner, W / 2f, 360f, 1.5f, Palette.YELLOW, a);
        }

        // --- chat / loot
        world.feed.render(batch, assets, 14f, 14f);

        drawMinimap();
    }

    private void drawPartyRow(float x, float topY, String name, int level, float hp, float maxHp,
                              boolean alive, float respawn, Color nameColor) {
        assets.text(batch, name + "  Lv" + level, x, topY, 0.95f, alive ? nameColor : Palette.RED);
        if (alive) {
            assets.bar(batch, x, topY - 24f, 200f, 8f, hp / maxHp, Palette.GREEN);
        } else {
            assets.text(batch, respawn > 0f ? String.format(Locale.ROOT, "ABATIDO - volta em %.0fs", respawn) : "ABATIDO",
                        x, topY - 14f, 0.85f, Palette.RED);
        }
    }

    private void drawMinimap() {
        float w = 160f, h = w * Constants.WORLD_H / Constants.WORLD_W;
        float x = Constants.VIEW_W - w - 16f, y = 14f;
        float s = w / Constants.WORLD_W;
        assets.panel(batch, x, y, w, h, Palette.DARK, Palette.GRAY, 0.75f);
        for (int i = 0; i < world.enemies.size; i++) {
            Enemy e = world.enemies.get(i);
            float d = e.isBoss() ? 7f : 3f;
            assets.rect(batch, x + e.pos.x * s - d / 2f, y + e.pos.y * s - d / 2f, d, d, Palette.RED, 1f);
        }
        for (int i = 0; i < world.allies.size; i++) {
            Ally a = world.allies.get(i);
            if (a.alive()) assets.rect(batch, x + a.pos.x * s - 2f, y + a.pos.y * s - 2f, 4f, 4f, Palette.GREEN, 1f);
        }
        Player p = world.player;
        assets.rect(batch, x + p.pos.x * s - 3f, y + p.pos.y * s - 3f, 6f, 6f, Palette.WHITE, 1f);
        // retangulo da camera
        float cw = Constants.VIEW_W * s, ch = Constants.VIEW_H * s;
        assets.rect(batch, x + (camX - Constants.VIEW_W / 2f) * s, y + (camY - Constants.VIEW_H / 2f) * s, cw, 1f, Palette.CYAN, 0.6f);
        assets.rect(batch, x + (camX - Constants.VIEW_W / 2f) * s, y + (camY + Constants.VIEW_H / 2f) * s, cw, 1f, Palette.CYAN, 0.6f);
        assets.rect(batch, x + (camX - Constants.VIEW_W / 2f) * s, y + (camY - Constants.VIEW_H / 2f) * s, 1f, ch, Palette.CYAN, 0.6f);
        assets.rect(batch, x + (camX + Constants.VIEW_W / 2f) * s, y + (camY - Constants.VIEW_H / 2f) * s, 1f, ch, Palette.CYAN, 0.6f);
    }

    private void dim(float alpha) {
        assets.rect(batch, 0f, 0f, Constants.VIEW_W, Constants.VIEW_H, Palette.BLACK, alpha);
    }

    private void drawIntro() {
        float cx = Constants.VIEW_W / 2f;
        assets.rect(batch, 0f, 250f, Constants.VIEW_W, 190f, Palette.BLACK, 0.55f);
        assets.centered(batch, cfg.unit() + " " + wave, cx, 430f, 5f, Palette.YELLOW);
        Enemy.Type[] bosses = cfg.bosses(wave);
        if (bosses.length > 0) {
            StringBuilder sb = new StringBuilder(bosses.length > 1 ? "BOSS DUPLO: " : "BOSS: ");
            for (int i = 0; i < bosses.length; i++) {
                if (i > 0) sb.append(" + ");
                sb.append(bosses[i] == Enemy.Type.QUEEN ? "RAINHA ENXAME" : "COLOSSUS");
            }
            assets.centered(batch, sb.toString(), cx, 352f, 2.4f, Palette.RED, 0.6f + 0.4f * MathUtils.sin(time * 8f));
        } else if (world.mutator != GameModeConfig.Mutator.NONE) {
            assets.centered(batch, "MUTATOR: " + world.mutator.label.toUpperCase(Locale.ROOT), cx, 360f, 2.2f,
                            Palette.PURPLE, 0.7f + 0.3f * MathUtils.sin(time * 8f));
            assets.centered(batch, world.mutator.description, cx, 328f, 1.2f, Palette.WHITE);
        } else {
            assets.centered(batch, "Elimine todos os inimigos", cx, 352f, 1.6f, Palette.CYAN);
        }
        assets.centered(batch, "WASD mover | auto-tiro e auto-mira (F / T) | ESPACO dash | 1-4 / Q / E armas | C postura",
                        cx, 300f, 1.0f, Palette.WHITE);
        assets.centered(batch, cfg.isTower() ? "Entre os andares, escolha 1 de 3 melhorias gratis"
                        : "Pegue o loot dos inimigos e compre melhorias entre as ondas",
                        cx, 276f, 1.0f, Palette.GRAY);
    }

    private void drawShop() {
        Player p = world.player;
        float cx = Constants.VIEW_W / 2f;
        dim(0.65f);
        float pw = 600f, ph = 400f, px = cx - pw / 2f, py = 120f;
        assets.panel(batch, px, py, pw, ph, Palette.DARK, Palette.CYAN, 0.97f);
        assets.centered(batch, "LOJA - " + cfg.unit() + " " + wave + " CONCLUIDA", cx, py + ph - 14f, 2.0f, Palette.YELLOW);
        assets.centered(batch, "Bonus de onda: +" + lastBonus + " pontos   |   Grupo curado e escudo recarregado",
                        cx, py + ph - 56f, 0.95f, Palette.GREEN);
        assets.centered(batch, "CREDITOS: " + p.credits, cx, py + ph - 82f, 1.6f, Palette.ORANGE);

        Upgrade[] ups = Upgrade.values();
        for (int i = 0; i < ups.length; i++) {
            Upgrade u = ups[i];
            int level = p.upgradeLevels[i];
            boolean maxed = u.isMaxed(level);
            int cost = u.cost(level);
            boolean ok = !maxed && p.credits >= cost;
            float y = py + ph - 128f - i * 40f;
            assets.rect(batch, px + 16f, y - 26f, pw - 32f, 32f, Palette.PANEL, 0.9f);
            assets.text(batch, "[" + (i + 1) + "]", px + 26f, y - 2f, 1.3f, ok ? Palette.YELLOW : Palette.GRAY);
            assets.text(batch, u.label + (u != Upgrade.REPAIR ? "  (nv " + level + ")" : ""), px + 74f, y - 2f, 1.2f,
                        ok ? Palette.WHITE : Palette.GRAY);
            assets.text(batch, u.description, px + 300f, y - 5f, 0.95f, Palette.GRAY);
            assets.text(batch, maxed ? "MAX" : cost + " cr", px + pw - 96f, y - 2f, 1.2f,
                        maxed ? Palette.GRAY : (ok ? Palette.GREEN : Palette.RED));
        }
        assets.centered(batch, "Pressione 1-5 para comprar", cx, py + 78f, 1.0f, Palette.GRAY);
        assets.centered(batch, "ENTER / ESPACO: proxima onda", cx, py + 50f, 1.6f, Palette.CYAN,
                        0.6f + 0.4f * MathUtils.sin(time * 6f));
        assets.centered(batch, "ESC: voltar ao menu", cx, py + 22f, 0.9f, Palette.GRAY);
    }

    /** Loja draft do Modo Torre: 3 cartas gratuitas, escolhe-se so uma. */
    private void drawDraft() {
        float cx = Constants.VIEW_W / 2f;
        dim(0.7f);
        assets.centered(batch, "ANDAR " + wave + " CONCLUIDO", cx, 590f, 3f, Palette.YELLOW);
        assets.centered(batch, "Bonus: +" + lastBonus + " pontos   |   Escolha UMA melhoria (teclas 1-3)", cx, 536f, 1.2f,
                        Palette.GREEN);
        float cw = 270f, gap = 24f, total = draft.size * cw + (draft.size - 1) * gap;
        float x0 = cx - total / 2f;
        for (int i = 0; i < draft.size; i++) {
            Draft.Option o = draft.get(i);
            float x = x0 + i * (cw + gap), y = 170f;
            assets.panel(batch, x, y, cw, 300f, Palette.DARK, o.color, 0.97f);
            assets.centered(batch, "[" + (i + 1) + "]", x + cw / 2f, y + 280f, 2.4f, Palette.YELLOW);
            assets.centered(batch, o.title, x + cw / 2f, y + 220f, 1.2f, o.color);
            wrapped(o.description, x + 16f, y + 170f, cw - 32f, 1.0f, Palette.WHITE);
        }
        assets.centered(batch, "Depois da escolha voce sobe para o andar " + (wave + 1), cx, 120f, 1.0f, Palette.GRAY);
        assets.centered(batch, "ESC: voltar ao menu", cx, 94f, 0.9f, Palette.GRAY);
    }

    /** Quebra o texto em linhas de no maximo ~28 caracteres. */
    private void wrapped(String text, float x, float topY, float width, float scale, Color c) {
        int max = Math.max(10, (int) (width / (9f * scale)));
        StringBuilder line = new StringBuilder();
        float y = topY;
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > max) {
                assets.text(batch, line.toString(), x, y, scale, c);
                y -= 18f * scale;
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) assets.text(batch, line.toString(), x, y, scale, c);
    }

    private void drawPause() {
        float cx = Constants.VIEW_W / 2f;
        dim(0.75f);
        assets.centered(batch, "PAUSADO", cx, 590f, 5f, Palette.YELLOW);
        assets.centered(batch, "OPCOES", cx, 508f, 1.6f, Palette.CYAN);
        options.draw(batch, assets, cx, 470f);
        assets.centered(batch, "P ou ESC: continuar", cx, 230f, 1.5f, Palette.WHITE);
        assets.centered(batch, "M: voltar ao menu", cx, 196f, 1.5f, Palette.GRAY);
    }

    /** Overlay de debug (F3): FPS, DPS, inimigos e atalhos do teclado numerico. */
    private void drawDebug() {
        float x = 260f;
        assets.rect(batch, x - 6f, 560f, 330f, 76f, Palette.BLACK, 0.6f);
        assets.text(batch, String.format(Locale.ROOT, "DEBUG  FPS %d  DPS %.0f", Gdx.graphics.getFramesPerSecond(),
                world.currentDps()), x, 632f, 0.9f, Palette.GREEN);
        assets.text(batch, String.format(Locale.ROOT, "Inimigos %d  fila %d  tempo %.0fs", world.enemies.size,
                world.waves.remainingToSpawn(), world.waves.waveTime()), x, 616f, 0.85f, Palette.WHITE);
        assets.text(batch, "NUM1 pular onda  NUM2 boss", x, 600f, 0.85f, Palette.YELLOW);
        assets.text(batch, "NUM3 +500 creditos  NUM4 arma lendaria", x, 584f, 0.85f, Palette.YELLOW);
        assets.text(batch, "(usar o debug desliga conquistas e Dados Estelares)", x, 568f, 0.75f, Palette.GRAY);
    }
}
