package com.fonseca.novafrontier.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.items.PassiveModule;
import com.fonseca.novafrontier.items.Upgrade;
import com.fonseca.novafrontier.items.Weapon;
import com.fonseca.novafrontier.items.WeaponType;
import com.fonseca.novafrontier.items.Rarity;
import com.fonseca.novafrontier.systems.SoundManager;
import com.fonseca.novafrontier.systems.World;

/**
 * Jogador: WASD move, mouse mira e atira, SPACE da um dash (invulneravel), 1-4/Q/E trocam de arma.
 * O escudo absorve dano primeiro e regenera depois de alguns segundos sem tomar hits.
 */
public class Player extends Entity {
    public static final int MAX_WEAPONS = 4;
    private static final float DASH_SPEED = 720f;
    private static final float DASH_TIME = 0.18f;
    public static final float DASH_COOLDOWN = 1.1f;
    /** Buff "Arsenal Dinamico": trocar de arma depois de um abate. */
    public static final float ARSENAL_TIME = 3f;
    public static final float ARSENAL_RATE = 1.15f;

    public float shield, maxShield;
    public float speed = 230f;
    public float damageMult = 1f;
    public int credits;
    /** Atira sozinho quando ha inimigo ao alcance (F alterna) e mira no mais proximo (T alterna). */
    public boolean autoFire = true;
    public boolean autoAim = true;
    public final int[] upgradeLevels = new int[Upgrade.values().length];
    public final Array<PassiveModule> modules = new Array<>();
    /** Cooldown do dash e atraso da regeneracao do escudo (alterados pelos mutators da Torre). */
    public float dashCdMax = DASH_COOLDOWN;
    public float shieldDelayMax = 3f;
    /** Vida perdida acumulada na run (conquista Intocavel). */
    public float totalHpLost;
    /** Ligado quando o escudo vai a zero; o World consome para disparar o Capacitor. */
    public boolean shieldBroke;
    public float arsenalTimer;
    private boolean killSinceSwitch;
    /** Skin (paleta) da nave. */
    public int skin;

    public final Array<Weapon> weapons = new Array<>();
    public int current;
    /** Posicao da mira em coordenadas do mundo (definida pela tela). */
    public final Vector2 aim = new Vector2();

    public float dashCooldown;
    private float dashTime;
    private final Vector2 dashDir = new Vector2();
    private float fireTimer;
    private float shieldDelay;
    private float iFrames;
    private final Vector2 input = new Vector2();
    private final Vector2 target = new Vector2();
    private final Vector2 tmp = new Vector2();

    public Player() {
        radius = 14f;
        maxHp = hp = 100f;
        maxShield = shield = 50f;
        weapons.add(new Weapon(WeaponType.BLASTER, Rarity.COMMON));
    }

    public Weapon weapon() { return weapons.get(current); }

    public boolean isDashing() { return dashTime > 0f; }

    public float aimAngle() { return tmp.set(aim).sub(pos).angleDeg(); }

    public void update(float dt, World w) {
        if (hurtFlash > 0f) hurtFlash -= dt;
        if (iFrames > 0f) iFrames -= dt;
        fireTimer -= dt;
        dashCooldown -= dt;
        shieldDelay -= dt;
        if (arsenalTimer > 0f) arsenalTimer -= dt;

        input.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) input.x -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) input.x += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) input.y -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) input.y += 1f;
        if (input.len2() > 1f) input.nor();

        if (dashTime > 0f) {
            dashTime -= dt;
            vel.set(dashDir).scl(DASH_SPEED);
            if (MathUtils.randomBoolean(0.7f)) w.particles.burst(pos.x, pos.y, Palette.CYAN, 1, 60f);
        } else {
            target.set(input).scl(speed);
            vel.lerp(target, Math.min(1f, 12f * dt));
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && dashCooldown <= 0f) {
                if (input.len2() > 0f) dashDir.set(input);
                else dashDir.set(aim).sub(pos).nor();
                if (dashDir.isZero()) dashDir.set(1f, 0f);
                dashTime = DASH_TIME;
                dashCooldown = dashCdMax;
                iFrames = DASH_TIME + 0.1f;
                w.particles.burst(pos.x, pos.y, Palette.CYAN, 10, 160f);
                SoundManager.play(SoundManager.Sfx.DASH);
                w.onPlayerDash(pos.x, pos.y);
            }
        }
        pos.mulAdd(vel, dt);
        pos.x = MathUtils.clamp(pos.x, radius, Constants.WORLD_W - radius);
        pos.y = MathUtils.clamp(pos.y, radius, Constants.WORLD_H - radius);

        if (shieldDelay <= 0f && shield < maxShield) shield = Math.min(maxShield, shield + 14f * dt);

        // troca de arma
        for (int i = 0; i < weapons.size && i < 4; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) selectWeapon(i);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) selectWeapon((current + weapons.size - 1) % weapons.size);
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) selectWeapon((current + 1) % weapons.size);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) autoFire = !autoFire;
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) autoAim = !autoAim;

        // tiro: mouse, tecla J ou auto-tiro (inimigo ao alcance)
        boolean want = Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyPressed(Input.Keys.J);
        if (!want && autoFire && w.nearestEnemy(pos, weapon().type.range * 0.95f) != null) want = true;
        if (want && fireTimer <= 0f && dashTime <= 0f) {
            Weapon wp = weapon();
            fireTimer = wp.cooldown() / (arsenalTimer > 0f ? ARSENAL_RATE : 1f);
            float ang = aimAngle();
            SoundManager.play(sfxFor(wp.type));
            tmp.set(MathUtils.cosDeg(ang), MathUtils.sinDeg(ang)).scl(26f).add(pos);
            wp.fire(w, tmp, ang, damageMult, true);
        }
    }

    /** Aplica dano considerando escudo, dash e frames de invulnerabilidade. Retorna true se tomou o hit. */
    public boolean takeHit(float dmg) {
        if (iFrames > 0f || dashTime > 0f || !alive()) return false;
        shieldDelay = shieldDelayMax;
        iFrames = 0.25f;
        if (shield > 0f) {
            float absorbed = Math.min(shield, dmg);
            shield -= absorbed;
            dmg -= absorbed;
            if (shield <= 0f) shieldBroke = true;
        }
        if (dmg > 0f) {
            totalHpLost += Math.min(dmg, Math.max(0f, hp));
            damage(dmg);
        } else {
            hurtFlash = 0.08f;
        }
        return true;
    }

    /** Troca de arma; se houve abate desde a ultima troca, concede o buff Arsenal Dinamico. */
    public void selectWeapon(int idx) {
        if (idx < 0 || idx >= weapons.size || idx == current) return;
        current = idx;
        if (killSinceSwitch) {
            arsenalTimer = ARSENAL_TIME;
            killSinceSwitch = false;
        }
    }

    /** Chamado pelo World a cada abate. */
    public void onKill() { killSinceSwitch = true; }

    public boolean hasModule(PassiveModule m) { return modules.contains(m, true); }

    /** Equipa um modulo se houver slot livre e ele ainda nao estiver equipado. */
    public boolean equipModule(PassiveModule m) {
        if (hasModule(m) || modules.size >= PassiveModule.MAX_SLOTS) return false;
        modules.add(m);
        return true;
    }

    /** Sorteia um modulo ainda nao equipado, ou null se nao houver slot/modulo. */
    public PassiveModule nextModuleDrop() {
        if (modules.size >= PassiveModule.MAX_SLOTS) return null;
        Array<PassiveModule> pool = new Array<>();
        for (PassiveModule m : PassiveModule.values()) if (!hasModule(m)) pool.add(m);
        return pool.size == 0 ? null : pool.random();
    }

    private static SoundManager.Sfx sfxFor(WeaponType t) {
        switch (t) {
            case SCATTER: return SoundManager.Sfx.SHOOT_SCATTER;
            case LASER: return SoundManager.Sfx.SHOOT_LASER;
            case PLASMA: return SoundManager.Sfx.SHOOT_PLASMA;
            default: return SoundManager.Sfx.SHOOT_BLASTER;
        }
    }

    /** Equipa/vende uma arma encontrada. Retorna uma mensagem para o log. */
    public String pickupWeapon(Weapon nw) {
        for (int i = 0; i < weapons.size; i++) {
            Weapon old = weapons.get(i);
            if (old.type == nw.type) {
                if (nw.score() > old.score()) {
                    weapons.set(i, nw);
                    return "Equipou " + nw.name();
                }
                return sell(nw);
            }
        }
        if (weapons.size < MAX_WEAPONS) {
            weapons.add(nw);
            return "Nova arma: " + nw.name();
        }
        int weakest = 0;
        for (int i = 1; i < weapons.size; i++) {
            if (weapons.get(i).score() < weapons.get(weakest).score()) weakest = i;
        }
        if (nw.score() > weapons.get(weakest).score()) {
            sell(weapons.get(weakest));
            weapons.set(weakest, nw);
            return "Trocou por " + nw.name();
        }
        return sell(nw);
    }

    private String sell(Weapon wp) {
        int v = wp.sellValue();
        credits += v;
        return "Vendeu " + wp.name() + " (+" + v + " cr)";
    }

    public void applyUpgrade(Upgrade u) {
        upgradeLevels[u.ordinal()]++;
        switch (u) {
            case HULL: maxHp += 30f; hp += 30f; break;
            case SHIELD: maxShield += 25f; shield += 25f; break;
            case ENGINE: speed *= 1.08f; break;
            case DAMAGE: damageMult += 0.10f; break;
            case REPAIR: hp = maxHp; shield = maxShield; break;
        }
    }

    public void render(SpriteBatch b, Assets a, float time) {
        if (iFrames > 0f && !isDashing() && ((int) (iFrames * 20f) % 2 == 0)) return;
        float ang = aimAngle();
        boolean faceLeft = MathUtils.cosDeg(ang) < 0f;
        a.glow(b, pos.x, pos.y - 10f, 56f, Palette.BLACK, 0.45f);
        if (isDashing()) {
            for (int i = 3; i >= 1; i--) {
                a.draw(b, a.playerSkin(skin), pos.x - vel.x * 0.02f * i, pos.y - vel.y * 0.02f * i, 48f, 48f,
                       faceLeft, Palette.CYAN, 0.25f / i);
            }
        }
        Color tint = hurtFlash > 0f ? Palette.RED : Palette.WHITE;
        a.draw(b, a.playerSkin(skin), pos.x, pos.y, 48f, 48f, faceLeft, tint, 1f);
        // arma apontando para a mira
        Weapon wp = weapon();
        a.line(b, pos.x, pos.y - 2f, ang, 30f, 7f, Palette.BLACK, 1f);
        a.line(b, pos.x, pos.y - 2f, ang, 28f, 4f, wp.color(), 1f);
        if (shield > 0f) a.ring(b, pos.x, pos.y, 70f, Palette.CYAN, 0.15f + 0.3f * (shield / maxShield));
    }
}
