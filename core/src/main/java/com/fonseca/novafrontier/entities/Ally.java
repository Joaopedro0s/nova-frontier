package com.fonseca.novafrontier.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.items.Weapon;
import com.fonseca.novafrontier.items.WeaponAffix;
import com.fonseca.novafrontier.systems.World;

/**
 * Aliado controlado por IA: simula outros jogadores de um MMO em grupo. Orbita o jogador,
 * mantem distancia dos inimigos, atira, cai ("abatido") e reaparece depois de alguns segundos.
 */
public class Ally extends Entity {
    public static final float RESPAWN_TIME = 10f;

    /** Postura do grupo (tecla C). */
    public enum Posture {
        DEFENSIVO("Defensivo", 130f),
        AGRESSIVO("Agressivo", 220f),
        FOCO_BOSS("Foco Boss", 130f);

        public final String label;
        public final float orbit;

        Posture(String label, float orbit) {
            this.label = label;
            this.orbit = orbit;
        }

        public Posture next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public final String name;
    public final int level;
    public final Weapon weapon;
    public final Color tint;
    public boolean downed;
    public float downTimer;
    /** Tempo para voltar depois de abatido (o Protocolo de Reanimacao do hangar reduz). */
    public float respawnTime = RESPAWN_TIME;

    private float orbit;
    private float fireTimer = MathUtils.random(0.3f);
    private final Vector2 goal = new Vector2();
    private final Vector2 desired = new Vector2();
    private final Vector2 tmp = new Vector2();

    public Ally(String name, int level, Weapon weapon, Color tint, float orbitStart) {
        this.name = name;
        this.level = level;
        this.weapon = weapon;
        this.tint = tint;
        this.orbit = orbitStart;
        radius = 13f;
        maxHp = hp = 100f;
    }

    public void update(float dt, World w) {
        if (hurtFlash > 0f) hurtFlash -= dt;
        if (!alive()) {
            if (!downed) {
                downed = true;
                downTimer = respawnTime;
                w.feed.chat(name, "fui abatido! volto ja ja");
            }
            downTimer -= dt;
            if (downTimer <= 0f) revive(w.player.pos, 0.5f);
            return;
        }

        fireTimer -= dt;
        orbit += dt * 0.35f;
        Player p = w.player;
        Posture posture = w.posture;
        float orbitR = posture.orbit;
        goal.set(MathUtils.cos(orbit) * orbitR, MathUtils.sin(orbit) * orbitR).add(p.pos);

        Enemy tgt = pickTarget(w, posture);
        if (tgt != null) {
            float d = pos.dst(tgt.pos);
            if (d < 210f) {
                // recua mantendo distancia
                goal.set(pos).add(tmp.set(pos).sub(tgt.pos).nor().scl(110f));
            }
            if (fireTimer <= 0f && d < weapon.type.range * 0.9f) {
                float ang = tmp.set(tgt.pos).sub(pos).angleDeg();
                weapon.fire(w, pos, ang, w.allyDamageMult(), true);
                // afixo Comando na arma do jogador: aliados atiram 25% mais rapido
                float rate = p.weapon().has(WeaponAffix.COMANDO) ? 1.25f : 1f;
                fireTimer = weapon.cooldown() * 1.5f / rate;
            }
        }
        // nunca se afasta demais do jogador
        float leash = posture == Posture.AGRESSIVO ? 520f : 420f;
        if (pos.dst(p.pos) > leash) goal.set(p.pos);

        desired.set(goal).sub(pos);
        float len = desired.len();
        if (len > 4f) desired.scl(Math.min(210f, len * 4f) / len);
        else desired.setZero();
        vel.lerp(desired, Math.min(1f, 6f * dt));
        pos.mulAdd(vel, dt);
        pos.x = MathUtils.clamp(pos.x, radius, Constants.WORLD_W - radius);
        pos.y = MathUtils.clamp(pos.y, radius, Constants.WORLD_H - radius);
    }

    /** Escolhe o alvo conforme a postura: boss, gunners/alvos distantes ou o mais proximo. */
    private Enemy pickTarget(World w, Posture posture) {
        if (posture == Posture.FOCO_BOSS) {
            Boss boss = w.activeBoss();
            if (boss != null && pos.dst(boss.pos) < 1100f) return boss;
        } else if (posture == Posture.AGRESSIVO) {
            Enemy g = w.nearestEnemyOfType(pos, Enemy.Type.GUNNER, 800f);
            if (g != null) return g;
            return w.nearestEnemy(pos, 700f);
        }
        return w.nearestEnemy(pos, 560f);
    }

    public void revive(Vector2 at, float hpFraction) {
        downed = false;
        hp = maxHp * hpFraction;
        pos.set(at).add(MathUtils.random(-40f, 40f), MathUtils.random(-40f, 40f));
        vel.setZero();
    }

    public void render(SpriteBatch b, Assets a, float time) {
        if (!alive()) return;
        a.glow(b, pos.x, pos.y - 10f, 52f, Palette.BLACK, 0.4f);
        Color t = hurtFlash > 0f ? Palette.RED : tint;
        boolean faceLeft = vel.x < -5f;
        a.draw(b, a.player, pos.x, pos.y, 44f, 44f, faceLeft, t, 1f);
        a.text(b, name + " Lv" + level, pos.x - 34f, pos.y + 44f, 0.8f, Palette.GREEN);
        a.bar(b, pos.x - 18f, pos.y + 30f, 36f, 4f, hp / maxHp, Palette.GREEN);
    }
}
