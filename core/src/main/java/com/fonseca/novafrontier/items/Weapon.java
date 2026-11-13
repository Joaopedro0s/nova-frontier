package com.fonseca.novafrontier.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.fonseca.novafrontier.entities.Bullet;
import com.fonseca.novafrontier.systems.World;

import java.util.EnumSet;

/** Uma arma concreta: tipo + raridade. */
public class Weapon {
    public final WeaponType type;
    public final Rarity rarity;
    /** Afixos (so Rara ou superior); vazio para armas Comuns. */
    public final EnumSet<WeaponAffix> affixes = EnumSet.noneOf(WeaponAffix.class);

    public Weapon(WeaponType type, Rarity rarity) {
        this.type = type;
        this.rarity = rarity;
    }

    /** Arma com afixos sorteados conforme a raridade (Rara 1, Epica 2, Lendaria 3). */
    public static Weapon rolled(WeaponType type, Rarity rarity) {
        Weapon w = new Weapon(type, rarity);
        w.fillAffixes();
        return w;
    }

    public static int affixCount(Rarity r) {
        switch (r) {
            case RARE: return 1;
            case EPIC: return 2;
            case LEGENDARY: return 3;
            default: return 0;
        }
    }

    private void fillAffixes() {
        WeaponAffix[] all = WeaponAffix.values();
        int want = affixCount(rarity);
        while (affixes.size() < want) affixes.add(all[MathUtils.random(all.length - 1)]);
    }

    public static Weapon random(float rarityBonus) {
        WeaponType[] all = WeaponType.values();
        return rolled(all[MathUtils.random(all.length - 1)], Rarity.roll(rarityBonus));
    }

    public boolean has(WeaponAffix a) { return affixes.contains(a); }

    /** Versao da arma com a raridade acima (mantem os afixos e sorteia os que faltarem). */
    public Weapon upgraded() {
        Rarity[] r = Rarity.values();
        if (rarity.ordinal() >= r.length - 1) return this;
        Weapon w = new Weapon(type, r[rarity.ordinal() + 1]);
        w.affixes.addAll(affixes);
        w.fillAffixes();
        return w;
    }

    /** Lista dos afixos para o HUD, ex.: "Calibrada, Perfura". */
    public String affixText() {
        StringBuilder sb = new StringBuilder();
        for (WeaponAffix a : affixes) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(a.label);
        }
        return sb.toString();
    }

    public float damage() { return type.damage * rarity.mult; }

    public float cooldown() { return type.cooldown; }

    public Color color() { return rarity == Rarity.COMMON ? type.color : rarity.color; }

    public String name() { return rarity.label + " " + type.label; }

    /** Pontuacao de poder (dano por segundo ponderado), usada para comparar armas e vender. */
    public float score() {
        float dps = damage() * type.pellets / type.cooldown;
        if (type.pierce > 0) dps *= 1f + type.pierce * 0.4f;
        if (type.blast > 0f) dps *= 1.6f;
        if (type.pellets > 1) dps *= 0.6f; // nem todos os projeteis acertam
        return dps * (1f + 0.12f * affixes.size());
    }

    public int sellValue() { return Math.max(5, (int) (score() / 3f)); }

    /** Dispara os projeteis do tipo, a partir de "origin" na direcao angleDeg. */
    public void fire(World w, Vector2 origin, float angleDeg, float dmgMult, boolean friendly) {
        for (int i = 0; i < type.pellets; i++) {
            float spread = type.pellets > 1 ? type.spread / 2f : type.spread;
            float a = angleDeg + MathUtils.random(-spread, spread);
            Bullet b = w.spawnBullet(origin.x, origin.y, a, type.speed, damage() * dmgMult,
                                     type.bulletRadius, friendly, color());
            b.pierce = type.pierce + (has(WeaponAffix.PERFURA) ? 1 : 0);
            b.blast = type.blast;
            b.life = type.range / type.speed;
            b.critBonus = has(WeaponAffix.CALIBRADA) ? 0.12f : 0f;
            b.slow = has(WeaponAffix.CRIOGENICA);
            b.vamp = has(WeaponAffix.VAMPIRICA);
        }
    }
}
