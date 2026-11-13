package com.fonseca.novafrontier.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Palette;

/** Chat do grupo + log de loot/eventos, estilo MMO. */
public class FeedLog {
    private static final int MAX_LINES = 7;
    private static final float TTL = 9f;

    private static final class Line {
        final String text;
        final Color color;
        float ttl = TTL;

        Line(String text, Color color) {
            this.text = text;
            this.color = color;
        }
    }

    private final Array<Line> lines = new Array<>();

    public void post(String text, Color color) {
        lines.add(new Line(text, color));
        if (lines.size > MAX_LINES) lines.removeIndex(0);
    }

    public void chat(String name, String msg) {
        post("[" + name + "] " + msg, Palette.CYAN);
    }

    public void system(String msg) {
        post(msg, Palette.YELLOW);
    }

    public int size() { return lines.size; }

    public void update(float dt) {
        for (int i = lines.size - 1; i >= 0; i--) {
            Line l = lines.get(i);
            l.ttl -= dt;
            if (l.ttl <= 0f) lines.removeIndex(i);
        }
    }

    /** Desenha de baixo para cima a partir de (x, y), a linha mais nova embaixo. */
    public void render(SpriteBatch b, Assets a, float x, float y) {
        for (int i = lines.size - 1, row = 0; i >= 0; i--, row++) {
            Line l = lines.get(i);
            float alpha = Math.min(1f, l.ttl / 1.5f);
            a.text(b, l.text, x, y + 14f + row * 16f, 0.9f, l.color, alpha);
        }
    }
}
