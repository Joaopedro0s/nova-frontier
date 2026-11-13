package com.fonseca.novafrontier.items;

import com.badlogic.gdx.graphics.Color;
import com.fonseca.novafrontier.Palette;

/** Modulos de nave: 3 slots passivos por run, dropados por bosses ou escolhidos na Torre. */
public enum PassiveModule {
    DASH_REACTOR("Reator de Dash", "Dash deixa explosao de plasma (35 dano, 80px)", Palette.PURPLE),
    SHIELD_CAPACITOR("Capacitor de Escudo", "Escudo quebrado emite pulso e reseta o dash", Palette.CYAN),
    NEURAL_LINK("Link Neural", "Aliados herdam 50% do upgrade de dano", Palette.GREEN),
    MANUAL_LENS("Lente Manual", "Sem auto-mira, critico causa x2.6", Palette.YELLOW);

    public static final int MAX_SLOTS = 3;

    public final String label;
    public final String description;
    public final Color color;

    PassiveModule(String label, String description, Color color) {
        this.label = label;
        this.description = description;
        this.color = color;
    }
}
