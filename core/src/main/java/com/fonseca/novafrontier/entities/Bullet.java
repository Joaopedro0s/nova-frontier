package com.fonseca.novafrontier.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/** Projetil (do jogador/aliados ou inimigo). */
public class Bullet {
    public final Vector2 pos = new Vector2();
    public final Vector2 vel = new Vector2();
    public float damage;
    public float radius = 4f;
    public float life = 2f;
    public boolean friendly;
    /** Quantos inimigos ainda pode atravessar. */
    public int pierce;
    /** Raio da explosao ao colidir (0 = sem explosao). */
    public float blast;
    /** Chance de critico extra (afixo Calibrada). */
    public float critBonus;
    /** Afixo Criogenica: reduz a velocidade do alvo atingido. */
    public boolean slow;
    /** Afixo Vampirica: abates com esse projetil podem curar o jogador. */
    public boolean vamp;
    public final Color color = new Color(1f, 1f, 1f, 1f);
    /** Entidades ja atingidas (evita acertar o mesmo alvo varias vezes ao perfurar). */
    public final Array<Entity> hit = new Array<>(4);
}
