package com.fonseca.novafrontier.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.items.PassiveModule;
import com.fonseca.novafrontier.items.Weapon;

/** Item largado no chao: creditos, medkit, celula de escudo ou arma. */
public class Pickup {
    public enum Kind { CREDIT, MEDKIT, SHIELD, WEAPON, MODULE }

    public static final float RADIUS = 14f;

    public final Kind kind;
    public final Vector2 pos = new Vector2();
    public final Vector2 vel = new Vector2();
    public final int value;
    public final Weapon weapon;
    public PassiveModule module;
    public float life = 25f;
    private float anim = MathUtils.random(MathUtils.PI2);

    public Pickup(Kind kind, float x, float y, int value, Weapon weapon) {
        this.kind = kind;
        this.value = value;
        this.weapon = weapon;
        pos.set(x, y);
        vel.setToRandomDirection().scl(MathUtils.random(60f, 170f));
    }

    public boolean expired() { return life <= 0f; }

    public void update(float dt, Player p, float magnet) {
        anim += dt;
        life -= dt;
        float d2 = pos.dst2(p.pos);
        if (kind != Kind.WEAPON && kind != Kind.MODULE && d2 < magnet * magnet && p.alive()) {
            // atracao magnetica
            float d = (float) Math.sqrt(d2);
            vel.set(p.pos).sub(pos).scl(1f / Math.max(1f, d)).scl(380f);
        } else {
            vel.scl(1f - Math.min(1f, 3.5f * dt));
        }
        pos.mulAdd(vel, dt);
    }

    public boolean touches(Player p) {
        float r = RADIUS + p.radius + 4f;
        return pos.dst2(p.pos) < r * r;
    }

    public void render(SpriteBatch b, Assets a) {
        if (life < 5f && ((int) (life * 6f) % 2 == 0)) return; // pisca antes de sumir
        float bob = MathUtils.sin(anim * 4f) * 3f;
        float x = pos.x, y = pos.y + bob;
        switch (kind) {
            case CREDIT:
                a.glow(b, x, y, 40f, Palette.YELLOW, 0.4f);
                a.draw(b, a.coin, x, y, 28f);
                break;
            case MEDKIT:
                a.glow(b, x, y, 48f, Palette.RED, 0.4f);
                a.draw(b, a.medkit, x, y, 32f);
                break;
            case SHIELD:
                a.glow(b, x, y, 48f, Palette.CYAN, 0.45f);
                a.draw(b, a.shieldIcon, x, y, 32f);
                break;
            case MODULE:
                Color mc = module.color;
                a.glow(b, x, y, 90f, mc, 0.6f + 0.2f * MathUtils.sin(anim * 6f));
                a.draw(b, a.crate, x, y, 38f, 38f, false, mc, 1f);
                a.ring(b, x, y, 52f, mc, 0.8f);
                break;
            case WEAPON:
                Color c = weapon.color();
                a.glow(b, x, y, 80f, c, 0.55f + 0.2f * MathUtils.sin(anim * 6f));
                a.draw(b, a.crate, x, y, 42f, 42f, false, c, 1f);
                break;
        }
    }
}
