package com.fonseca.novafrontier.entities;

import com.badlogic.gdx.math.Vector2;

/** Base de tudo que tem posicao, raio e vida. */
public abstract class Entity {
    public final Vector2 pos = new Vector2();
    public final Vector2 vel = new Vector2();
    public float radius;
    public float hp;
    public float maxHp;
    public float hurtFlash;

    public boolean alive() { return hp > 0f; }

    public void damage(float d) {
        hp -= d;
        hurtFlash = 0.12f;
    }

    public void heal(float h) { hp = Math.min(maxHp, hp + h); }
}
