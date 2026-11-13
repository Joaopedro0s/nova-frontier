package com.fonseca.novafrontier.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.systems.World;

/**
 * Inimigos comuns:
 * DRONE (persegue), SWARMER (rapido e fragil, em bando), GUNNER (mantem distancia e atira),
 * BRUTE (lento e muito resistente). COLOSSUS e QUEEN sao os bosses (ver {@link Boss}).
 */
public class Enemy extends Entity {
    public enum Type {
        //      vida   vel  contato raio  creditos pontos
        DRONE(34f,   125f, 14f,  16f,  6,  10),
        SWARMER(14f, 210f,  8f,  11f,  3,   5),
        GUNNER(55f,   90f, 11f,  16f, 10,  20),
        BRUTE(320f,   75f, 28f,  30f, 30,  60),
        COLOSSUS(3400f, 75f, 30f, 70f, 200, 1000),
        QUEEN(5600f,  60f, 25f,  76f, 400, 2000);

        public final float hp, speed, contact, radius;
        public final int credits, score;

        Type(float hp, float speed, float contact, float radius, int credits, int score) {
            this.hp = hp;
            this.speed = speed;
            this.contact = contact;
            this.radius = radius;
            this.credits = credits;
            this.score = score;
        }
    }

    public final Type type;
    public float contactTimer;
    public boolean rewarded;
    /** Afixo Criogenica: tempo restante de lentidao (-30% de velocidade). */
    public float slowTimer;
    /** Enrage do Modo Infinito: +20% de velocidade. */
    public boolean rage;

    protected float speed;
    protected float anim = MathUtils.random(MathUtils.PI2);
    protected final Vector2 toTarget = new Vector2();
    protected final Vector2 desired = new Vector2();
    private float shootTimer = MathUtils.random(0.6f, 1.8f);

    public Enemy(Type type, float x, float y, float hpScale) {
        this.type = type;
        pos.set(x, y);
        maxHp = hp = type.hp * hpScale;
        radius = type.radius;
        speed = type.speed;
    }

    public boolean isBoss() { return type == Type.COLOSSUS || type == Type.QUEEN; }

    protected void tick(float dt) {
        speed = type.speed * (slowTimer > 0f ? 0.7f : 1f) * (rage ? 1.2f : 1f);
        if (slowTimer > 0f) slowTimer -= dt;
        anim += dt;
        contactTimer -= dt;
        if (hurtFlash > 0f) hurtFlash -= dt;
    }

    public void update(float dt, World w) {
        tick(dt);
        Entity tg = w.nearestTarget(pos);
        if (tg == null) return;
        toTarget.set(tg.pos).sub(pos);
        float dist = toTarget.len();
        if (dist > 0.001f) toTarget.scl(1f / dist);

        if (type == Type.GUNNER) {
            if (dist > 300f) desired.set(toTarget);
            else if (dist < 200f) desired.set(toTarget).scl(-1f);
            else desired.set(-toTarget.y, toTarget.x).scl(MathUtils.sin(anim * 0.8f) > 0f ? 1f : -1f);
            desired.scl(speed);
            shootTimer -= dt;
            if (shootTimer <= 0f && dist < 540f) {
                w.spawnEnemyBullet(pos.x, pos.y, toTarget.angleDeg(), 260f, type.contact, 5f);
                shootTimer = MathUtils.random(1.5f, 2.3f);
            }
        } else {
            desired.set(toTarget).scl(speed);
        }
        vel.lerp(desired, Math.min(1f, 5f * dt));
        pos.mulAdd(vel, dt);
        clampToWorld();
    }

    protected void clampToWorld() {
        pos.x = MathUtils.clamp(pos.x, radius, Constants.WORLD_W - radius);
        pos.y = MathUtils.clamp(pos.y, radius, Constants.WORLD_H - radius);
    }

    protected Texture texture(Assets a) {
        switch (type) {
            case DRONE: return a.drone;
            case SWARMER: return a.swarmer;
            case GUNNER: return a.gunner;
            case BRUTE: return a.brute;
            case COLOSSUS: return a.colossus;
            default: return a.queen;
        }
    }

    public void render(SpriteBatch b, Assets a, float time) {
        float size = radius * 2.4f;
        float bob = MathUtils.sin(anim * 6f) * (isBoss() ? 2f : 3f);
        a.glow(b, pos.x, pos.y - radius * 0.5f, size * 1.1f, Palette.BLACK, 0.4f);
        Color tint = hurtFlash > 0f ? Palette.RED : Palette.WHITE;
        a.draw(b, texture(a), pos.x, pos.y + bob, size, size, vel.x < -5f, tint, 1f);
        if (!isBoss() && hp < maxHp) {
            a.bar(b, pos.x - radius, pos.y + radius + 8f, radius * 2f, 4f, hp / maxHp, Palette.RED);
        }
    }
}
