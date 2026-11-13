package com.fonseca.novafrontier.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.systems.World;

/**
 * Bosses com duas fases (a segunda comeca abaixo de 50% de vida):
 * COLOSSUS - tiros em leque, aneis de balas e investidas telegrafadas.
 * QUEEN    - espirais de balas, aneis e convoca enxames.
 */
public class Boss extends Enemy {
    private float atkTimer = 2f;
    private float ringTimer = 5f;
    private float chargeTimer = 6f;
    private float spawnTimer = 4f;
    private float telegraph;
    private float chargeTime;
    private float spiral;
    private final Vector2 chargeDir = new Vector2();
    /** Fracao de vida abaixo da qual entra na fase 2 (50% normal, 65% no Pesadelo). */
    public float phase2Frac = 0.5f;
    /** Vida perdida pelo jogador quando o boss surgiu (conquista Intocavel). */
    public float hpLostAtSpawn;

    public Boss(Type type, float x, float y, float hpScale) {
        super(type, x, y, hpScale);
    }

    public String title() { return type == Type.COLOSSUS ? "COLOSSUS" : "RAINHA ENXAME"; }

    public boolean enraged() { return hp < maxHp * phase2Frac; }

    @Override
    public void update(float dt, World w) {
        tick(dt);
        Entity tg = w.nearestTarget(pos);
        if (tg == null) return;
        toTarget.set(tg.pos).sub(pos);
        float dist = toTarget.len();
        if (dist > 0.001f) toTarget.scl(1f / dist);

        if (type == Type.COLOSSUS) colossus(dt, w, dist);
        else queen(dt, w, dist);

        pos.mulAdd(vel, dt);
        clampToWorld();
    }

    private void colossus(float dt, World w, float dist) {
        if (telegraph > 0f) {
            telegraph -= dt;
            vel.scl(1f - Math.min(1f, 6f * dt));
            if (telegraph <= 0f) {
                chargeTime = 0.75f;
                vel.set(chargeDir).scl(440f);
                w.shake = Math.max(w.shake, 0.3f);
            }
            return;
        }
        if (chargeTime > 0f) {
            chargeTime -= dt;
            return;
        }
        if (dist > 330f) desired.set(toTarget).scl(speed);
        else if (dist < 230f) desired.set(toTarget).scl(-speed);
        else desired.setZero();
        vel.lerp(desired, Math.min(1f, 3f * dt));

        boolean rage = enraged();
        atkTimer -= dt;
        if (atkTimer <= 0f) {
            float a = toTarget.angleDeg();
            for (int i = -1; i <= 1; i++) w.spawnEnemyBullet(pos.x, pos.y, a + i * 12f, 280f, 12f, 6f);
            atkTimer = rage ? 0.9f : 1.4f;
        }
        ringTimer -= dt;
        if (ringTimer <= 0f) {
            ring(w, rage ? 24 : 16, 200f, 10f, MathUtils.random(360f));
            ringTimer = rage ? 4f : 6f;
        }
        chargeTimer -= dt;
        if (chargeTimer <= 0f) {
            telegraph = 0.8f;
            chargeDir.set(toTarget);
            chargeTimer = rage ? 5f : 7.5f;
        }
    }

    private void queen(float dt, World w, float dist) {
        if (dist > 430f) desired.set(toTarget).scl(speed);
        else if (dist < 300f) desired.set(toTarget).scl(-speed);
        else desired.set(-toTarget.y, toTarget.x).scl(speed * 0.8f * (MathUtils.sin(anim * 0.5f) > 0f ? 1f : -1f));
        vel.lerp(desired, Math.min(1f, 3f * dt));

        boolean rage = enraged();
        atkTimer -= dt;
        if (atkTimer <= 0f) {
            spiral += 17f;
            w.spawnEnemyBullet(pos.x, pos.y, spiral, 230f, 9f, 5f);
            if (rage) w.spawnEnemyBullet(pos.x, pos.y, spiral + 180f, 230f, 9f, 5f);
            atkTimer = 0.11f;
        }
        ringTimer -= dt;
        if (ringTimer <= 0f) {
            ring(w, 20, 180f, 10f, MathUtils.random(360f));
            ringTimer = rage ? 6f : 8f;
        }
        spawnTimer -= dt;
        if (spawnTimer <= 0f) {
            for (int i = 0; i < 4; i++) {
                float ang = i * 90f + MathUtils.random(40f);
                w.spawnEnemy(Type.SWARMER, pos.x + MathUtils.cosDeg(ang) * 90f, pos.y + MathUtils.sinDeg(ang) * 90f);
            }
            spawnTimer = rage ? 5f : 7f;
        }
    }

    private void ring(World w, int n, float speed, float dmg, float offset) {
        for (int i = 0; i < n; i++) w.spawnEnemyBullet(pos.x, pos.y, offset + i * 360f / n, speed, dmg, 6f);
    }

    @Override
    public void render(SpriteBatch b, Assets a, float time) {
        if (telegraph > 0f) {
            a.line(b, pos.x, pos.y, chargeDir.angleDeg(), 520f, 10f, Palette.RED,
                   0.25f + 0.2f * MathUtils.sin(time * 40f));
        }
        if (enraged()) a.glow(b, pos.x, pos.y, radius * 3.4f, Palette.RED, 0.25f + 0.1f * MathUtils.sin(time * 8f));
        super.render(b, a, time);
    }
}
