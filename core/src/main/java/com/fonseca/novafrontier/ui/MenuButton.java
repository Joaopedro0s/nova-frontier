package com.fonseca.novafrontier.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Palette;

/** Botao simples de menu (selecionavel por teclado ou mouse). */
public class MenuButton {
    public final Rectangle bounds = new Rectangle();
    private final String label;

    public MenuButton(String label, float cx, float cy, float w, float h) {
        this.label = label;
        bounds.set(cx - w / 2f, cy - h / 2f, w, h);
    }

    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    public void render(SpriteBatch b, Assets a, boolean selected, float time) {
        Color border = selected ? Palette.YELLOW : Palette.GRAY;
        Color fill = selected ? Palette.PANEL : Palette.DARK;
        float pulse = selected ? 1f + 0.03f * MathUtils.sin(time * 8f) : 1f;
        float w = bounds.width * pulse;
        float h = bounds.height * pulse;
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;
        a.panel(b, cx - w / 2f, cy - h / 2f, w, h, fill, border, 1f);
        a.centeredAt(b, selected ? "> " + label + " <" : label, cx, cy, 2.2f,
                     selected ? Palette.YELLOW : Palette.WHITE);
    }
}
