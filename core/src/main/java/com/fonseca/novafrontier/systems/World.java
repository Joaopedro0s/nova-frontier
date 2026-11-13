package com.fonseca.novafrontier.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.entities.Ally;
import com.fonseca.novafrontier.entities.Boss;
import com.fonseca.novafrontier.entities.Bullet;
import com.fonseca.novafrontier.entities.Enemy;
import com.fonseca.novafrontier.entities.Entity;
import com.fonseca.novafrontier.entities.Pickup;
import com.fonseca.novafrontier.entities.Player;
import com.fonseca.novafrontier.items.PassiveModule;
import com.fonseca.novafrontier.items.Rarity;
import com.fonseca.novafrontier.items.Weapon;
import com.fonseca.novafrontier.items.WeaponType;
import com.fonseca.novafrontier.systems.AchievementManager.Ach;
import com.fonseca.novafrontier.systems.GameModeConfig.Mutator;
import com.fonseca.novafrontier.systems.SoundManager.Sfx;

/**
 * O mundo do jogo: guarda todas as entidades e resolve combate, projeteis, loot e IA coletiva.
 * A GameScreen so cuida de entrada, camera, estados e HUD.
 */
public class World {

    private static final class Floater {
        final String text;
        final float x;
        float y;
        final Color color;
        final float scale;
        final float maxLife;
        float life;

        Floater(String text, float x, float y, Color color, float scale) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.scale = scale;
            this.maxLife = this.life = 0.9f;
        }
    }

    public final Player player = new Player();
    public final Array<Ally> allies = new Array<>();
    public final Array<Enemy> enemies = new Array<>();
    public final Array<Bullet> bullets = new Array<>();
    public final Array<Pickup> pickups = new Array<>();
    public final ParticleSystem particles = new ParticleSystem();
    public final FeedLog feed = new FeedLog();
    public final GameModeConfig cfg;
    public final ProfileManager profile = ProfileManager.get();
    public final WaveManager waves;
    public final ScoreManager score = new ScoreManager();
    public final AchievementManager achievements = new AchievementManager(profile);
    /** Mutator da sala atual (Modo Torre). */
    public Mutator mutator = Mutator.NONE;
    /** Postura dos aliados (tecla C). */
    public Ally.Posture posture = Ally.Posture.DEFENSIVO;
    /** Tremor de camera (segundos restantes); a tela decai e usa esse valor. */
    public float shake;
    /** Estatisticas da run (usadas no fim da partida). */
    public int wavesCleared;
    public int bossesKilled;
    /** Alguma ferramenta de debug foi usada: a run nao rende conquistas nem Dados Estelares. */
    public boolean debugUsed;
    /** Alerta de conquista no HUD. */
    public String achBanner = "";
    public float achBannerTimer;
    /** Conquistas desbloqueadas nesta run (mostradas no fim da partida). */
    public final Array<Ach> unlockedThisRun = new Array<>();

    private boolean autoAimUsedThisWave;
    private final float[] dmgBuckets = new float[6];
    private int dmgIdx;
    private float dmgTimer;
    private float hitSoundGap;

    private final Array<Floater> floaters = new Array<>();
    private final Array<float[]> flashes = new Array<>(); // x, y, raio, vida
    private float lowHpChatTimer;
    private final Vector2 tmp = new Vector2();

    public World() {
        this(new GameModeConfig(GameModeConfig.Mode.OPERACAO, GameModeConfig.Difficulty.DIFICIL));
    }

    public World(GameModeConfig cfg) {
        this.cfg = cfg;
        this.waves = new WaveManager(cfg);
        score.setDifficultyMult(cfg.diff.scoreMult);
        player.pos.set(Constants.WORLD_W / 2f, Constants.WORLD_H / 2f);
        player.weapons.set(0, new Weapon(cfg.startWeapon, Rarity.COMMON));
        player.skin = profile.skin();
        allies.add(new Ally("NovaKai", 14, new Weapon(WeaponType.LASER, Rarity.RARE),
                Color.valueOf("9dffb0"), 0f));
        allies.add(new Ally("Vexa_77", 12, new Weapon(WeaponType.SCATTER, Rarity.COMMON),
                Color.valueOf("e0b3ff"), MathUtils.PI));
        for (Ally a : allies) {
            a.pos.set(player.pos).add(MathUtils.random(-80f, 80f), MathUtils.random(-80f, 80f));
            a.respawnTime = profile.reviveTime();
        }
        feed.system("Voce entrou no grupo com NovaKai e Vexa_77  [" + cfg.label() + "]");
    }

    // ================================================================== regras de modo
    /** Multiplicador de dano inimigo na onda atual (dificuldade + escala do Infinito). */
    public float enemyDmgMult() { return cfg.dmgScale(waves.wave()); }

    /** Dano dos aliados: x0.5 e, com o Link Neural, herdam 50% do upgrade de dano do jogador. */
    public float allyDamageMult() {
        float m = 0.5f;
        if (player.hasModule(PassiveModule.NEURAL_LINK)) m *= 1f + 0.5f * (player.damageMult - 1f);
        return m;
    }

    /** Multiplicador de dano critico: x2 (Tempestade Ionica x2.5; Lente Manual x2.6 sem auto-mira). */
    public float critMult() {
        float m = mutator == Mutator.ION_STORM ? 2.5f : 2f;
        if (player.hasModule(PassiveModule.MANUAL_LENS) && !player.autoAim) m = Math.max(m, 2.6f);
        return m;
    }

    private float creditMult() { return mutator == Mutator.SWARM ? 1.25f : 1f; }

    /** Prepara a introducao de uma onda/andar: no Modo Torre sorteia o mutator (exceto em boss). */
    public void prepareWave(int n) {
        mutator = Mutator.NONE;
        if (cfg.isTower() && !cfg.isBossWave(n)) {
            Mutator[] all = GameModeConfig.Mutator.values();
            mutator = all[MathUtils.random(1, all.length - 1)];
        }
        player.dashCdMax = mutator == Mutator.OVERLOAD ? Player.DASH_COOLDOWN * 0.5f : Player.DASH_COOLDOWN;
        player.shieldDelayMax = mutator == Mutator.ION_STORM ? 6f : 3f;
        if (mutator != Mutator.NONE) {
            feed.system("Mutator do andar: " + mutator.label + " - " + mutator.description);
        }
    }

    /** Alterna a postura dos aliados e avisa no chat. */
    public void cyclePosture() {
        posture = posture.next();
        String[][] lines = {
            { "fico colado em voce", "defendendo a posicao!" },
            { "vou cacar os gunners!", "avancando, cobre minhas costas" },
            { "todo o dano no boss!", "foco total no chefe!" }
        };
        feed.system("Postura do grupo: " + posture.label);
        for (int i = 0; i < allies.size; i++) {
            Ally a = allies.get(i);
            if (a.alive()) feed.chat(a.name, lines[posture.ordinal()][i % 2]);
        }
    }

    public Enemy nearestEnemyOfType(Vector2 from, Enemy.Type type, float maxDist) {
        Enemy best = null;
        float bd = maxDist * maxDist;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.alive() || e.type != type) continue;
            float d = from.dst2(e.pos);
            if (d <= bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    /** Reator de Dash: explosao de plasma no ponto de partida do dash. */
    public void onPlayerDash(float x, float y) {
        if (player.hasModule(PassiveModule.DASH_REACTOR)) {
            explode(x, y, 80f, 35f, null);
        }
    }

    /** Conquista Colecionador Lendario: 2 armas Lendarias equipadas ao mesmo tempo. */
    public void checkLegendaryCollector() {
        int n = 0;
        for (Weapon w : player.weapons) if (w.rarity == Rarity.LEGENDARY) n++;
        if (n >= 2) achievements.unlock(Ach.COLECIONADOR_LENDARIO);
    }

    /** Dano por segundo causado pelo grupo nos ultimos 3 segundos (overlay de debug). */
    public float currentDps() {
        float sum = 0f;
        for (float b : dmgBuckets) sum += b;
        return sum / 3f;
    }

    // ---- ferramentas de debug (F3 + teclado numerico); a run deixa de render conquistas/Dados
    public void debugSkipWave() {
        debugUsed = true;
        achievements.setEnabled(false);
        waves.clearQueue();
        enemies.clear();
    }

    public void debugSpawnBoss() {
        debugUsed = true;
        achievements.setEnabled(false);
        spawnEnemyFar(MathUtils.randomBoolean() ? Enemy.Type.COLOSSUS : Enemy.Type.QUEEN);
    }

    public void debugCredits() {
        debugUsed = true;
        achievements.setEnabled(false);
        player.credits += 500;
    }

    public void debugLegendary() {
        debugUsed = true;
        achievements.setEnabled(false);
        WeaponType[] all = WeaponType.values();
        Weapon w = Weapon.rolled(all[MathUtils.random(all.length - 1)], Rarity.LEGENDARY);
        addPickup(new Pickup(Pickup.Kind.WEAPON, player.pos.x + 40f, player.pos.y, 0, w));
    }

    private void addPickup(Pickup p) {
        p.life = cfg.diff.dropLife;
        pickups.add(p);
    }

    // ================================================================== consultas
    public Entity nearestTarget(Vector2 from) {
        Entity best = null;
        float bd = Float.MAX_VALUE;
        if (player.alive()) {
            best = player;
            bd = from.dst2(player.pos);
        }
        for (int i = 0; i < allies.size; i++) {
            Ally a = allies.get(i);
            if (!a.alive()) continue;
            float d = from.dst2(a.pos);
            if (d < bd) {
                bd = d;
                best = a;
            }
        }
        return best;
    }

    public Enemy nearestEnemy(Vector2 from, float maxDist) {
        Enemy best = null;
        float bd = maxDist * maxDist;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.alive()) continue;
            float d = from.dst2(e.pos);
            if (d <= bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    public Boss activeBoss() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e instanceof Boss && e.alive()) return (Boss) e;
        }
        return null;
    }

    public void allyChat(String msg) {
        Array<Ally> alive = new Array<>();
        for (Ally a : allies) if (a.alive()) alive.add(a);
        if (alive.size == 0) return;
        feed.chat(alive.random().name, msg);
    }

    // ================================================================== criacao
    public Bullet spawnBullet(float x, float y, float angleDeg, float speed, float dmg, float radius,
                              boolean friendly, Color color) {
        Bullet b = new Bullet();
        b.pos.set(x, y);
        b.vel.set(speed, 0f).setAngleDeg(angleDeg);
        b.damage = dmg;
        b.radius = radius;
        b.friendly = friendly;
        b.color.set(color);
        bullets.add(b);
        return b;
    }

    public Bullet spawnEnemyBullet(float x, float y, float angleDeg, float speed, float dmg, float radius) {
        Bullet b = spawnBullet(x, y, angleDeg, speed * cfg.diff.bulletSpeed, dmg * enemyDmgMult(),
                               radius, false, Palette.RED);
        b.life = 6f;
        return b;
    }

    public Enemy spawnEnemy(Enemy.Type type, float x, float y) {
        Enemy e;
        if (type == Enemy.Type.COLOSSUS || type == Enemy.Type.QUEEN) {
            Boss boss = new Boss(type, x, y, cfg.bossHpScale(waves.wave()));
            boss.phase2Frac = cfg.diff.bossPhase2;
            boss.hpLostAtSpawn = player.totalHpLost;
            e = boss;
            feed.system("!! BOSS: " + boss.title() + " !!");
            allyChat("CUIDADO, e o chefe! desvia dos tiros!");
            shake = Math.max(shake, 0.5f);
            SoundManager.play(Sfx.BOSS_ALERT);
        } else {
            e = new Enemy(type, x, y, waves.hpScale());
            e.rage = waves.enraged();
        }
        enemies.add(e);
        return e;
    }

    /** Cria um inimigo longe do jogador (fora da tela). */
    public Enemy spawnEnemyFar(Enemy.Type type) {
        float x = player.pos.x, y = player.pos.y;
        for (int i = 0; i < 25; i++) {
            float ang = MathUtils.random(360f);
            float dist = MathUtils.random(540f, 760f);
            x = MathUtils.clamp(player.pos.x + MathUtils.cosDeg(ang) * dist, 40f, Constants.WORLD_W - 40f);
            y = MathUtils.clamp(player.pos.y + MathUtils.sinDeg(ang) * dist, 40f, Constants.WORLD_H - 40f);
            if (Math.hypot(x - player.pos.x, y - player.pos.y) > 460f) break;
        }
        return spawnEnemy(type, x, y);
    }

    public void beginWave(int n) {
        waves.startWave(n, mutator);
        autoAimUsedThisWave = false;
        feed.system("== " + cfg.unit() + " " + n + " ==");
        if (cfg.isEndless() && n >= 20) achievements.unlock(Ach.SOBREVIVENTE);
        if (!cfg.isBossWave(n)) {
            String[] msgs = { "la vem eles!", "bora, time!", "foco nos gunners!", "segura a posicao!" };
            allyChat(msgs[MathUtils.random(msgs.length - 1)]);
        }
    }

    /** Fim de onda: recolhe o loot, cura o grupo e limpa projeteis. */
    public void endWave() {
        waves.finishWave();
        wavesCleared++;
        int cleared = waves.wave();
        if (cleared >= 10 && !cfg.isTower()) {
            achievements.unlock(Ach.PRIMEIRA_LIMPEZA);
            if (cfg.diff == GameModeConfig.Difficulty.HARD) achievements.unlock(Ach.VETERANO);
        }
        if (cfg.isTower() && cleared >= 20) achievements.unlock(Ach.CONQUISTADOR_TORRE);
        if (cfg.isBossWave(cleared) && !autoAimUsedThisWave) achievements.unlock(Ach.MIRA_LASER);
        bullets.clear();
        for (int i = 0; i < pickups.size; i++) collect(pickups.get(i));
        pickups.clear();
        for (Ally a : allies) {
            if (!a.alive() || a.downed) a.revive(player.pos, 1f);
            else a.hp = a.maxHp;
        }
        player.heal(player.maxHp * 0.35f);
        player.shield = player.maxShield;
        feed.system(cfg.unit().charAt(0) + cfg.unit().substring(1).toLowerCase() + " " + waves.wave() + " concluida!");
        String[] msgs = { "gg!", "boa, pessoal!", "facil demais", "bora na loja!" };
        allyChat(msgs[MathUtils.random(msgs.length - 1)]);
    }

    // ================================================================== update
    /** combat=false congela inimigos e ondas (usado na introducao da onda). */
    public void update(float dt, boolean combat) {
        feed.update(dt);
        particles.update(dt);
        score.update(dt);
        updateFloaters(dt);
        if (lowHpChatTimer > 0f) lowHpChatTimer -= dt;
        if (hitSoundGap > 0f) hitSoundGap -= dt;
        if (achBannerTimer > 0f) achBannerTimer -= dt;
        dmgTimer += dt;
        while (dmgTimer >= 0.5f) {
            dmgTimer -= 0.5f;
            dmgIdx = (dmgIdx + 1) % dmgBuckets.length;
            dmgBuckets[dmgIdx] = 0f;
        }

        player.update(dt, this);
        for (int i = 0; i < allies.size; i++) allies.get(i).update(dt, this);

        if (combat) {
            if (player.autoAim) autoAimUsedThisWave = true;
            achievements.update(dt, score.multiplier());
            waves.update(dt, this);
            for (int i = 0; i < enemies.size; i++) {
                Enemy e = enemies.get(i);
                if (e.alive()) e.update(dt, this);
            }
            separateEnemies();
            resolveContacts();
        }
        updateBullets(dt);
        updatePickups(dt);

        for (int i = enemies.size - 1; i >= 0; i--) {
            if (!enemies.get(i).alive()) enemies.removeIndex(i);
        }
        if (player.alive() && player.hp < player.maxHp * 0.25f && lowHpChatTimer <= 0f) {
            allyChat("pega um medkit! eu te cubro");
            lowHpChatTimer = 25f;
        }
        for (int i = flashes.size - 1; i >= 0; i--) {
            float[] f = flashes.get(i);
            f[3] -= dt;
            if (f[3] <= 0f) flashes.removeIndex(i);
        }
        announceAchievements();
    }

    /** Alerta no HUD + comemoracao dos aliados no chat. */
    private void announceAchievements() {
        Array<Ach> got = achievements.drainPending();
        for (int i = 0; i < got.size; i++) {
            Ach a = got.get(i);
            unlockedThisRun.add(a);
            achBanner = "CONQUISTA: " + a.title;
            achBannerTimer = 4f;
            feed.system("Conquista desbloqueada: " + a.title + " - " + a.description);
            SoundManager.play(Sfx.ACHIEVEMENT);
            Array<Ally> alive = new Array<>();
            for (Ally al : allies) if (al.alive()) alive.add(al);
            if (alive.size > 0) {
                String[] msgs = { "GG! Conquista desbloqueada!", "uau, conquista nova! GG!" };
                feed.chat(alive.get(i % alive.size).name, msgs[i % msgs.length]);
            }
        }
    }

    private void separateEnemies() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy a = enemies.get(i);
            if (!a.alive()) continue;
            for (int j = i + 1; j < enemies.size; j++) {
                Enemy b = enemies.get(j);
                if (!b.alive()) continue;
                float min = (a.radius + b.radius) * 0.8f;
                float d2 = a.pos.dst2(b.pos);
                if (d2 >= min * min || d2 < 0.0001f) continue;
                float d = (float) Math.sqrt(d2);
                float wa = a.isBoss() ? 0f : 1f;
                float wb = b.isBoss() ? 0f : 1f;
                if (wa + wb == 0f) continue;
                float push = (min - d) / (wa + wb);
                float nx = (b.pos.x - a.pos.x) / d, ny = (b.pos.y - a.pos.y) / d;
                a.pos.add(-nx * push * wa, -ny * push * wa);
                b.pos.add(nx * push * wb, ny * push * wb);
                // a separacao nao pode empurrar ninguem para fora da arena (acontece com muitos brutes)
                a.pos.x = MathUtils.clamp(a.pos.x, a.radius, Constants.WORLD_W - a.radius);
                a.pos.y = MathUtils.clamp(a.pos.y, a.radius, Constants.WORLD_H - a.radius);
                b.pos.x = MathUtils.clamp(b.pos.x, b.radius, Constants.WORLD_W - b.radius);
                b.pos.y = MathUtils.clamp(b.pos.y, b.radius, Constants.WORLD_H - b.radius);
            }
        }
    }

    private void resolveContacts() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.alive() || e.contactTimer > 0f) continue;
            float r = (e.radius + player.radius) * 0.8f;
            float contact = e.type.contact * enemyDmgMult();
            if (player.alive() && e.pos.dst2(player.pos) < r * r && player.takeHit(contact)) {
                e.contactTimer = 0.7f;
                onPlayerHit(contact);
                continue;
            }
            for (int k = 0; k < allies.size; k++) {
                Ally a = allies.get(k);
                if (!a.alive()) continue;
                float ra = (e.radius + a.radius) * 0.8f;
                if (e.pos.dst2(a.pos) < ra * ra) {
                    a.damage(contact);
                    e.contactTimer = 0.7f;
                    break;
                }
            }
        }
    }

    private void onPlayerHit(float dmg) {
        shake = Math.max(shake, 0.25f);
        particles.burst(player.pos.x, player.pos.y, Palette.RED, 12, 220f);
        floater("-" + (int) dmg, player.pos.x, player.pos.y + 26f, Palette.RED, 1.2f);
        score.breakStreak();
        if (player.shieldBroke) {
            player.shieldBroke = false;
            SoundManager.play(Sfx.SHIELD_BREAK);
            if (player.hasModule(PassiveModule.SHIELD_CAPACITOR)) shieldPulse();
        } else {
            SoundManager.play(Sfx.HIT);
        }
    }

    /** Capacitor de Escudo: empurra inimigos proximos e zera o cooldown do dash. */
    private void shieldPulse() {
        player.dashCooldown = 0f;
        flashes.add(new float[] { player.pos.x, player.pos.y, 220f, 0.3f });
        particles.burst(player.pos.x, player.pos.y, Palette.CYAN, 30, 320f);
        floater("PULSO!", player.pos.x, player.pos.y + 44f, Palette.CYAN, 1.2f);
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.alive()) continue;
            tmp.set(e.pos).sub(player.pos);
            float d = tmp.len();
            if (d > 240f + e.radius) continue;
            if (d < 1f) tmp.set(1f, 0f); else tmp.scl(1f / d);
            e.pos.mulAdd(tmp, e.isBoss() ? 30f : 150f);
            e.pos.x = MathUtils.clamp(e.pos.x, e.radius, Constants.WORLD_W - e.radius);
            e.pos.y = MathUtils.clamp(e.pos.y, e.radius, Constants.WORLD_H - e.radius);
        }
    }

    private void updateBullets(float dt) {
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.pos.mulAdd(b.vel, dt);
            b.life -= dt;
            boolean remove = b.life <= 0f
                    || b.pos.x < -20f || b.pos.x > Constants.WORLD_W + 20f
                    || b.pos.y < -20f || b.pos.y > Constants.WORLD_H + 20f;

            if (!remove && b.friendly) {
                for (int j = 0; j < enemies.size && !remove; j++) {
                    Enemy e = enemies.get(j);
                    if (!e.alive()) continue;
                    float r = b.radius + e.radius;
                    if (b.pos.dst2(e.pos) > r * r || b.hit.contains(e, true)) continue;
                    b.hit.add(e);
                    if (b.blast > 0f) {
                        remove = true;
                    } else {
                        hitEnemy(e, b.damage, b);
                        if (b.pierce > 0) b.pierce--;
                        else remove = true;
                    }
                }
            } else if (!remove) {
                float r = b.radius + player.radius * 0.8f;
                if (player.alive() && b.pos.dst2(player.pos) < r * r && player.takeHit(b.damage)) {
                    onPlayerHit(b.damage);
                    remove = true;
                }
                for (int k = 0; k < allies.size && !remove; k++) {
                    Ally a = allies.get(k);
                    if (!a.alive()) continue;
                    float ra = b.radius + a.radius * 0.8f;
                    if (b.pos.dst2(a.pos) < ra * ra) {
                        a.damage(b.damage);
                        remove = true;
                    }
                }
            }

            if (remove) {
                if (b.friendly && b.blast > 0f) explode(b.pos.x, b.pos.y, b.blast, b.damage, b);
                bullets.removeIndex(i);
            }
        }
    }

    private void explode(float x, float y, float radius, float dmg, Bullet src) {
        particles.burst(x, y, Palette.PURPLE, 26, 300f);
        particles.burst(x, y, Palette.WHITE, 8, 200f);
        flashes.add(new float[] { x, y, radius, 0.3f });
        shake = Math.max(shake, 0.12f);
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.alive()) continue;
            float reach = radius + e.radius;
            float d = (float) Math.sqrt(e.pos.dst2(tmp.set(x, y)));
            if (d < reach) hitEnemy(e, dmg * (1f - 0.5f * d / reach), src);
        }
    }

    private void hitEnemy(Enemy e, float dmg, Bullet src) {
        float critChance = 0.10f + (src != null ? src.critBonus : 0f);
        boolean crit = MathUtils.randomBoolean(critChance);
        float d = dmg * (crit ? critMult() : 1f) * MathUtils.random(0.92f, 1.08f);
        e.damage(d);
        dmgBuckets[dmgIdx] += d;
        if (src != null && src.slow) e.slowTimer = 2f;
        if (hitSoundGap <= 0f) {
            SoundManager.play(Sfx.HIT);
            hitSoundGap = 0.05f;
        }
        floater((int) d + (crit ? "!" : ""), e.pos.x + MathUtils.random(-10f, 10f), e.pos.y + e.radius,
                crit ? Palette.YELLOW : Palette.WHITE, crit ? 1.3f : 0.9f);
        particles.burst(e.pos.x, e.pos.y, Palette.ORANGE, 2, 120f);
        if (!e.alive()) killEnemy(e, src);
    }

    private void killEnemy(Enemy e, Bullet src) {
        if (e.rewarded) return;
        e.rewarded = true;
        int pts = score.kill(e.type.score);
        player.onKill();
        if (src != null && src.vamp && MathUtils.randomBoolean(0.15f)) {
            if (player.hp < player.maxHp) {
                player.heal(3f);
                floater("+3 vida", player.pos.x, player.pos.y + 30f, Palette.GREEN, 1f);
            } else if (player.shield < player.maxShield) {
                player.shield = Math.min(player.maxShield, player.shield + 5f);
                floater("+5 escudo", player.pos.x, player.pos.y + 30f, Palette.CYAN, 1f);
            }
        }
        boolean boss = e.isBoss();
        particles.burst(e.pos.x, e.pos.y, Palette.ORANGE, boss ? 90 : 14, boss ? 420f : 240f);
        if (boss) {
            shake = 0.8f;
            bossesKilled++;
            feed.system("BOSS derrotado! +" + pts + " pontos");
            allyChat("GG! chefe no chao!");
            if (player.totalHpLost <= ((Boss) e).hpLostAtSpawn) achievements.unlock(Ach.INTOCAVEL);
        }
        dropLoot(e, boss);
    }

    private void dropLoot(Enemy e, boolean boss) {
        int credits = Math.round(e.type.credits * creditMult() * MathUtils.random(0.8f, 1.2f));
        int n = boss ? 10 : (credits > 20 ? 3 : (credits > 8 ? 2 : 1));
        for (int i = 0; i < n; i++) {
            addPickup(new Pickup(Pickup.Kind.CREDIT, e.pos.x, e.pos.y, Math.max(1, credits / n), null));
        }
        if (boss) {
            Weapon w = Weapon.random(0.35f);
            if (w.rarity == Rarity.COMMON) w = Weapon.rolled(w.type, Rarity.RARE);
            addPickup(new Pickup(Pickup.Kind.WEAPON, e.pos.x, e.pos.y, 0, w));
            addPickup(new Pickup(Pickup.Kind.MEDKIT, e.pos.x, e.pos.y, 35, null));
            addPickup(new Pickup(Pickup.Kind.SHIELD, e.pos.x, e.pos.y, 40, null));
            // bosses derrotados garantem um Modulo Passivo (ou creditos se nao houver slot)
            PassiveModule m = player.nextModuleDrop();
            if (m != null) {
                Pickup mp = new Pickup(Pickup.Kind.MODULE, e.pos.x, e.pos.y, 0, null);
                mp.module = m;
                addPickup(mp);
            } else {
                addPickup(new Pickup(Pickup.Kind.CREDIT, e.pos.x, e.pos.y, 150, null));
                feed.system("Slots de modulo cheios: +150 creditos no lugar do modulo");
            }
            return;
        }
        float hd = cfg.diff.healDrop;
        float medkit = 0.10f * hd, shield = medkit + 0.07f * hd, weapon = shield + 0.06f;
        float r = MathUtils.random();
        if (r < medkit) {
            addPickup(new Pickup(Pickup.Kind.MEDKIT, e.pos.x, e.pos.y, 35, null));
        } else if (r < shield) {
            addPickup(new Pickup(Pickup.Kind.SHIELD, e.pos.x, e.pos.y, 40, null));
        } else if (r < weapon) {
            addPickup(new Pickup(Pickup.Kind.WEAPON, e.pos.x, e.pos.y, 0,
                    Weapon.random(Math.min(0.3f, waves.wave() * 0.02f))));
        }
    }

    private void updatePickups(float dt) {
        for (int i = pickups.size - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            p.update(dt, player, profile.magnetRadius());
            if (p.expired()) {
                pickups.removeIndex(i);
            } else if (player.alive() && p.touches(player)) {
                collect(p);
                pickups.removeIndex(i);
            }
        }
    }

    private void collect(Pickup p) {
        switch (p.kind) {
            case CREDIT:
                player.credits += p.value;
                floater("+" + p.value, p.pos.x, p.pos.y + 16f, Palette.YELLOW, 0.9f);
                break;
            case MEDKIT:
                player.heal(p.value);
                floater("+" + p.value + " vida", p.pos.x, p.pos.y + 16f, Palette.GREEN, 1f);
                break;
            case SHIELD:
                player.shield = Math.min(player.maxShield, player.shield + p.value);
                floater("+" + p.value + " escudo", p.pos.x, p.pos.y + 16f, Palette.CYAN, 1f);
                break;
            case WEAPON:
                feed.post(player.pickupWeapon(p.weapon), p.weapon.rarity.color);
                if (p.weapon.affixes.size() > 0) feed.post("  Afixos: " + p.weapon.affixText(), p.weapon.rarity.color);
                checkLegendaryCollector();
                break;
            case MODULE:
                if (player.equipModule(p.module)) {
                    feed.post("Modulo equipado: " + p.module.label + " - " + p.module.description, p.module.color);
                } else {
                    player.credits += 150;
                    feed.system("Modulo repetido ou sem slot: +150 creditos");
                }
                break;
        }
        SoundManager.play(Sfx.PICKUP);
    }

    public void floater(String text, float x, float y, Color color, float scale) {
        if (floaters.size > 80) return;
        floaters.add(new Floater(text, x, y, color, scale));
    }

    private void updateFloaters(float dt) {
        for (int i = floaters.size - 1; i >= 0; i--) {
            Floater f = floaters.get(i);
            f.life -= dt;
            f.y += 42f * dt;
            if (f.life <= 0f) floaters.removeIndex(i);
        }
    }

    // ================================================================== render
    public void render(SpriteBatch b, Assets a, float time) {
        for (int i = 0; i < pickups.size; i++) pickups.get(i).render(b, a);
        for (int i = 0; i < enemies.size; i++) if (!enemies.get(i).isBoss()) enemies.get(i).render(b, a, time);
        for (int i = 0; i < enemies.size; i++) if (enemies.get(i).isBoss()) enemies.get(i).render(b, a, time);
        for (int i = 0; i < allies.size; i++) allies.get(i).render(b, a, time);
        if (player.alive()) player.render(b, a, time);

        for (int i = 0; i < bullets.size; i++) {
            Bullet bl = bullets.get(i);
            float s = bl.radius;
            a.glow(b, bl.pos.x, bl.pos.y, s * 6f, bl.color, 0.55f);
            a.rect(b, bl.pos.x - s * 0.6f, bl.pos.y - s * 0.6f, s * 1.2f, s * 1.2f, Palette.WHITE, 1f);
        }
        for (int i = 0; i < flashes.size; i++) {
            float[] f = flashes.get(i);
            float t = 1f - f[3] / 0.3f;
            a.ring(b, f[0], f[1], f[2] * 2f * (0.4f + 0.6f * t), Palette.PURPLE, 1f - t);
        }
        particles.render(b, a);
        for (int i = 0; i < floaters.size; i++) {
            Floater f = floaters.get(i);
            a.centered(b, f.text, f.x, f.y, f.scale, f.color, Math.min(1f, f.life / f.maxLife * 1.5f));
        }
    }
}
