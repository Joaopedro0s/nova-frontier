package com.fonseca.novafrontier.items;

import com.badlogic.gdx.graphics.Color;

/** Tipos de arma e seus atributos base. */
public enum WeaponType {
    //          nome            dano   recarga vel   tiros espalh. perfura explosao raio  alcance cor
    BLASTER("Blaster",         12f,   0.22f,  650f, 1,    2f,    0,      0f,      3.5f, 620f,   "5ee6ff"),
    SCATTER("Scatter",          7f,   0.75f,  560f, 6,   26f,    0,      0f,      3.0f, 380f,   "ff9a3c"),
    LASER("Laser Rifle",        9f,   0.09f,  900f, 1,    1.5f,  2,      0f,      3.0f, 700f,   "ff4d5a"),
    PLASMA("Plasma Launcher",  42f,   0.95f,  430f, 1,    0f,    0,    100f,      7.0f, 520f,   "c77dff");

    public final String label;
    public final float damage;
    public final float cooldown;
    public final float speed;
    public final int pellets;
    public final float spread;
    public final int pierce;
    public final float blast;
    public final float bulletRadius;
    public final float range;
    public final Color color;

    WeaponType(String label, float damage, float cooldown, float speed, int pellets, float spread,
               int pierce, float blast, float bulletRadius, float range, String hex) {
        this.label = label;
        this.damage = damage;
        this.cooldown = cooldown;
        this.speed = speed;
        this.pellets = pellets;
        this.spread = spread;
        this.pierce = pierce;
        this.blast = blast;
        this.bulletRadius = bulletRadius;
        this.range = range;
        this.color = Color.valueOf(hex);
    }
}
