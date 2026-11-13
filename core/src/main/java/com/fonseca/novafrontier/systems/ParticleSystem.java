package com.fonseca.novafrontier.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.fonseca.novafrontier.Assets;

/** Particulas quadradinhas (estilo pixel) para feedback de coleta, dano e viradas. */
public class ParticleSystem {
    private static class P {
        float x, y, vx, vy, life, maxLife, size;
        final Color color = new Color();
    }

    private final Array<P> active = new Array<>();
    private final Pool<P> pool = new Pool<P>() {
        @Override
        protected P newObject() {
            return new P();
        }
    };

    public void burst(float x, float y, Color c, int count, float speed) {
        for (int i = 0; i < count; i++) {
            P p = pool.obtain();
            float ang = MathUtils.random(MathUtils.PI2);
            float s = speed * MathUtils.random(0.3f, 1f);
            p.x = x;
            p.y = y;
            p.vx = MathUtils.cos(ang) * s;
            p.vy = MathUtils.sin(ang) * s;
            p.maxLife = p.life = MathUtils.random(0.35f, 0.8f);
            p.size = MathUtils.random(3f, 7f);
            p.color.set(c);
            active.add(p);
        }
    }

    public void update(float dt) {
        for (int i = active.size - 1; i >= 0; i--) {
            P p = active.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                active.removeIndex(i);
                pool.free(p);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= 1f - Math.min(1f, 2.5f * dt);
            p.vy *= 1f - Math.min(1f, 2.5f * dt);
        }
    }

    public void render(SpriteBatch b, Assets a) {
        for (int i = 0; i < active.size; i++) {
            P p = active.get(i);
            float alpha = p.life / p.maxLife;
            a.rect(b, p.x - p.size / 2f, p.y - p.size / 2f, p.size, p.size, p.color, alpha);
        }
    }

    public void clear() {
        pool.freeAll(active);
        active.clear();
    }
}
