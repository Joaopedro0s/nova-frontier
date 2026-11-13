package com.fonseca.novafrontier.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.fonseca.novafrontier.Palette;

/** Raridade de armas: multiplica o dano e define a cor do item. */
public enum Rarity {
    COMMON("Comum", 1.0f, Palette.WHITE),
    RARE("Raro", 1.3f, Palette.CYAN),
    EPIC("Epico", 1.7f, Palette.PURPLE),
    LEGENDARY("Lendario", 2.2f, Palette.YELLOW);

    public final String label;
    public final float mult;
    public final Color color;

    Rarity(String label, float mult, Color color) {
        this.label = label;
        this.mult = mult;
        this.color = color;
    }

    /** Sorteia uma raridade; "bonus" (0..0.5) desloca a sorte para raridades melhores. */
    public static Rarity roll(float bonus) {
        float r = MathUtils.clamp(MathUtils.random() + bonus, 0f, 0.9999f);
        if (r < 0.60f) return COMMON;
        if (r < 0.88f) return RARE;
        if (r < 0.98f) return EPIC;
        return LEGENDARY;
    }
}
