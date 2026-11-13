package com.fonseca.novafrontier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Todos os recursos visuais sao gerados proceduralmente em tempo de execucao
 * (pixel art desenhada em codigo). Nenhum arquivo de imagem externo e usado.
 */
public class Assets implements Disposable {

    public final Texture player, drone, gunner, swarmer, brute, colossus, queen;
    /** Skins da nave: 0 = padrao, 1 = Carmesim, 2 = Cyber Ouro, 3 = Void Roxo (paletas procedurais). */
    public final Texture[] playerSkins = new Texture[4];
    public final Texture coin, medkit, shieldIcon, crate;
    public final Texture floor, pixel, ring, glow;
    public final TextureRegion pixelRegion;
    public final BitmapFont font;

    private final GlyphLayout layout = new GlyphLayout();
    private final Color tmp = new Color();

    // metade esquerda (espelhada) de sprites 16x16
    private static final String[] PLAYER = {
        "........", "....kkkk", "...kbbbb", "..kbbbbb", "..kbwwww", "..kbwkkw", "..kbwwww", "..kbbbbb",
        ".kkbbyyy", ".kbbbyyy", ".kbbbbbb", "..kbbbbb", "..kkbbbb", "...kbkkk", "...kbk..", "...kkk.."
    };
    private static final String[] DRONE = {
        "........", "...kk...", "...kkkkk", "..kdddds", ".kddssss", ".kdsssrr", ".kdssrww", ".kdssrwk",
        ".kdsssrr", ".kddssss", "..kdddds", "...kkkkk", "...kk...", "........", "........", "........"
    };
    private static final String[] GUNNER = {
        "........", "....kkkk", "...koooo", "..kooooo", "..kowwyy", "..kowkyy", "..kooooo", ".kkddddd",
        "kdddddss", "kd.kdddd", "k..kdddd", "...kddss", "..kkddkk", "..kk.kk.", "..kk....", "........"
    };
    private static final String[] SWARMER = {
        "........", "........", "........", "..k....k", "..k..kkk", "...kgggg", "..kggwkg", ".kgggggg",
        ".kgggggg", "..kggggg", "..kk.kkk", ".k.k.k.k", "........", "........", "........", "........"
    };

    public Assets() {
        Map<Character, Integer> pal = palette();
        player = sprite(PLAYER, pal);
        playerSkins[0] = player;
        playerSkins[1] = skin(pal, 0xd62839, 0xffb703); // Carmesim
        playerSkins[2] = skin(pal, 0xd4a017, 0x5ee6ff); // Cyber Ouro
        playerSkins[3] = skin(pal, 0x7b2cbf, 0xe0aaff); // Void Roxo
        drone = sprite(DRONE, pal);
        gunner = sprite(GUNNER, pal);
        swarmer = sprite(SWARMER, pal);

        brute = canvas(24, pm -> {
            pm.setColor(rgb(0x14101f)); pm.fillCircle(12, 12, 11);
            pm.setColor(rgb(0x8e44ad)); pm.fillCircle(12, 12, 10);
            pm.setColor(rgb(0x5b2a7a)); pm.fillCircle(12, 12, 7);
            pm.setColor(rgb(0xc77dff)); pm.fillRectangle(5, 4, 14, 2);
            pm.setColor(rgb(0x14101f)); pm.fillRectangle(7, 9, 10, 5);
            pm.setColor(rgb(0xff4d5a)); pm.fillRectangle(8, 10, 3, 3); pm.fillRectangle(13, 10, 3, 3);
            pm.setColor(rgb(0xffffff)); pm.fillRectangle(9, 11, 1, 1); pm.fillRectangle(14, 11, 1, 1);
        });

        colossus = canvas(32, pm -> {
            pm.setColor(rgb(0x14101f)); pm.fillRectangle(2, 4, 28, 24);
            pm.fillRectangle(0, 9, 3, 14); pm.fillRectangle(29, 9, 3, 14); pm.fillRectangle(11, 0, 10, 5);
            pm.setColor(rgb(0x6b7a99)); pm.fillRectangle(3, 5, 26, 22);
            pm.setColor(rgb(0x3d405b)); pm.fillRectangle(7, 9, 18, 14);
            pm.setColor(rgb(0xff4d5a)); pm.fillRectangle(1, 11, 1, 10); pm.fillRectangle(30, 11, 1, 10);
            pm.fillRectangle(13, 1, 6, 2);
            pm.setColor(rgb(0xb8c0cc)); pm.drawRectangle(4, 6, 24, 20);
            pm.setColor(rgb(0xff9a3c)); pm.fillCircle(16, 16, 6);
            pm.setColor(rgb(0xffd23f)); pm.fillCircle(16, 16, 4);
            pm.setColor(rgb(0xffffff)); pm.fillCircle(16, 16, 2);
        });

        queen = canvas(32, pm -> {
            pm.setColor(rgb(0x14101f));
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5.0;
                pm.drawLine(16, 16, 16 + (int) Math.round(Math.cos(a) * 15), 16 + (int) Math.round(Math.sin(a) * 15));
                pm.drawLine(17, 16, 17 + (int) Math.round(Math.cos(a) * 15), 16 + (int) Math.round(Math.sin(a) * 15));
            }
            pm.fillCircle(16, 16, 13);
            pm.setColor(rgb(0x2ecc71)); pm.fillCircle(16, 16, 12);
            pm.setColor(rgb(0x1b7a45)); pm.fillCircle(16, 16, 8);
            pm.setColor(rgb(0xc77dff)); pm.fillCircle(16, 16, 4);
            pm.setColor(rgb(0xffffff)); pm.fillCircle(16, 16, 2);
            pm.setColor(rgb(0xff4d5a));
            pm.fillRectangle(8, 9, 2, 2); pm.fillRectangle(22, 9, 2, 2);
            pm.fillRectangle(8, 21, 2, 2); pm.fillRectangle(22, 21, 2, 2);
        });

        coin = canvas(12, pm -> {
            pm.setColor(rgb(0xf59e0b)); pm.fillCircle(6, 6, 5);
            pm.setColor(rgb(0xffd23f)); pm.fillCircle(6, 6, 4);
            pm.setColor(rgb(0xf59e0b)); pm.fillRectangle(5, 3, 2, 6);
        });
        medkit = canvas(12, pm -> {
            pm.setColor(rgb(0x14101f)); pm.fillRectangle(0, 1, 12, 10);
            pm.setColor(rgb(0xffffff)); pm.fillRectangle(1, 2, 10, 8);
            pm.setColor(rgb(0xe63946)); pm.fillRectangle(5, 3, 2, 6); pm.fillRectangle(3, 5, 6, 2);
        });
        shieldIcon = canvas(12, pm -> {
            for (int y = 0; y < 12; y++) {
                int w = 6 - (int) Math.abs(y - 5.5f);
                pm.setColor(rgb(0x5ee6ff));
                pm.fillRectangle(6 - w, y, w * 2, 1);
            }
            pm.setColor(rgb(0xffffff)); pm.fillRectangle(5, 4, 2, 4);
        });
        crate = canvas(14, pm -> {
            pm.setColor(rgb(0x14101f)); pm.fillRectangle(0, 0, 14, 14);
            pm.setColor(rgb(0xdde3ee)); pm.fillRectangle(1, 1, 12, 12);
            pm.setColor(rgb(0x555d78)); pm.fillRectangle(3, 6, 8, 3); pm.fillRectangle(9, 4, 2, 3); pm.fillRectangle(3, 9, 2, 2);
        });

        floor = makeFloor();
        pixel = makePixel();
        pixelRegion = new TextureRegion(pixel);
        ring = makeRing();
        glow = makeGlow();

        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    // ------------------------------------------------------------------ geracao
    private static int rgb(int hex) {
        return (hex << 8) | 0xFF;
    }

    private static Map<Character, Integer> palette() {
        Map<Character, Integer> m = new HashMap<>();
        m.put('k', rgb(0x14101f));
        m.put('w', rgb(0xffffff));
        m.put('b', rgb(0x3a7bd5));
        m.put('c', rgb(0x5ee6ff));
        m.put('y', rgb(0xffd23f));
        m.put('o', rgb(0xff8c1a));
        m.put('r', rgb(0xe63946));
        m.put('g', rgb(0x4ade80));
        m.put('d', rgb(0x3d405b));
        m.put('s', rgb(0xb8c0cc));
        return m;
    }

    private Texture sprite(String[] half, Map<Character, Integer> pal) {
        int h = half.length;
        int hw = half[0].length();
        int w = hw * 2;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < hw; x++) {
                char ch = x < half[y].length() ? half[y].charAt(x) : '.';
                Integer col = pal.get(ch);
                if (col != null) {
                    pm.drawPixel(x, y, col);
                    pm.drawPixel(w - 1 - x, y, col);
                }
            }
        }
        return finish(pm, false);
    }

    /** Variante do sprite da nave trocando as cores do casco ('b') e dos detalhes ('y'). */
    private Texture skin(Map<Character, Integer> base, int body, int trim) {
        Map<Character, Integer> pal = new HashMap<>(base);
        pal.put('b', rgb(body));
        pal.put('y', rgb(trim));
        return sprite(PLAYER, pal);
    }

    public Texture playerSkin(int i) {
        return playerSkins[Math.max(0, Math.min(playerSkins.length - 1, i))];
    }

    private Texture canvas(int size, Consumer<Pixmap> painter) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();
        painter.accept(pm);
        return finish(pm, false);
    }

    private Texture finish(Pixmap pm, boolean repeat) {
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        if (repeat) t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pm.dispose();
        return t;
    }

    private Texture makeFloor() {
        Pixmap pm = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int col = rgb(0x12162a);
                if (x == 0 || y == 0) col = rgb(0x1b2142);
                if ((x == 2 || x == 13) && (y == 2 || y == 13)) col = rgb(0x2c3560);
                if (x == 8 && y > 4 && y < 12) col = rgb(0x151a33);
                pm.drawPixel(x, y, col);
            }
        }
        return finish(pm, true);
    }

    private Texture makePixel() {
        Pixmap pm = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    private Texture makeRing() {
        Pixmap pm = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        for (int r = 61; r <= 63; r++) pm.drawCircle(64, 64, r);
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    private Texture makeGlow() {
        int s = 64;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float dx = (x + 0.5f - s / 2f) / (s / 2f);
                float dy = (y + 0.5f - s / 2f) / (s / 2f);
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - d);
                pm.setColor(1f, 1f, 1f, a * a);
                pm.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    // ------------------------------------------------------------------ desenho
    public void draw(SpriteBatch b, Texture t, float cx, float cy, float w, float h,
                     boolean flipX, Color tint, float alpha) {
        b.setColor(tint.r, tint.g, tint.b, alpha);
        b.draw(t, cx - w / 2f, cy - h / 2f, w, h, 0, 0, t.getWidth(), t.getHeight(), flipX, false);
        b.setColor(1f, 1f, 1f, 1f);
    }

    public void draw(SpriteBatch b, Texture t, float cx, float cy, float size) {
        draw(b, t, cx, cy, size, size, false, Palette.WHITE, 1f);
    }

    public void glow(SpriteBatch b, float cx, float cy, float size, Color c, float alpha) {
        draw(b, glow, cx, cy, size, size, false, c, alpha);
    }

    public void ring(SpriteBatch b, float cx, float cy, float size, Color c, float alpha) {
        draw(b, ring, cx, cy, size, size, false, c, alpha);
    }

    public void rect(SpriteBatch b, float x, float y, float w, float h, Color c, float alpha) {
        b.setColor(c.r, c.g, c.b, alpha);
        b.draw(pixel, x, y, w, h);
        b.setColor(1f, 1f, 1f, 1f);
    }

    public void panel(SpriteBatch b, float x, float y, float w, float h, Color fill, Color border, float alpha) {
        rect(b, x - 3, y - 3, w + 6, h + 6, border, alpha);
        rect(b, x, y, w, h, fill, alpha);
    }

    /** Barra de status (fundo + preenchimento). */
    public void bar(SpriteBatch b, float x, float y, float w, float h, float frac, Color c) {
        rect(b, x - 1, y - 1, w + 2, h + 2, Palette.BLACK, 0.8f);
        rect(b, x, y, w, h, Palette.PANEL, 1f);
        rect(b, x, y, w * Math.max(0f, Math.min(1f, frac)), h, c, 1f);
    }

    public void line(SpriteBatch b, float x, float y, float angleDeg, float length, float thickness,
                     Color c, float alpha) {
        b.setColor(c.r, c.g, c.b, alpha);
        b.draw(pixelRegion, x, y - thickness / 2f, 0f, thickness / 2f, length, thickness, 1f, 1f, angleDeg);
        b.setColor(1f, 1f, 1f, 1f);
    }

    // ------------------------------------------------------------------ texto (y = topo do texto)
    public float text(SpriteBatch b, String s, float x, float topY, float scale, Color c) {
        font.getData().setScale(scale);
        font.setColor(c);
        font.draw(b, s, x, topY);
        layout.setText(font, s);
        float w = layout.width;
        font.getData().setScale(1f);
        return w;
    }

    public void text(SpriteBatch b, String s, float x, float topY, float scale, Color c, float alpha) {
        tmp.set(c.r, c.g, c.b, alpha);
        text(b, s, x, topY, scale, tmp);
    }

    public void centered(SpriteBatch b, String s, float cx, float topY, float scale, Color c, float alpha) {
        font.getData().setScale(scale);
        layout.setText(font, s);
        float w = layout.width;
        font.getData().setScale(1f);
        text(b, s, cx - w / 2f, topY, scale, c, alpha);
    }

    public void centered(SpriteBatch b, String s, float cx, float topY, float scale, Color c) {
        centered(b, s, cx, topY, scale, c, 1f);
    }

    public void centeredAt(SpriteBatch b, String s, float cx, float cy, float scale, Color c) {
        float capH = font.getCapHeight() * scale;
        centered(b, s, cx, cy + capH / 2f, scale, c, 1f);
    }

    @Override
    public void dispose() {
        for (Texture t : playerSkins) t.dispose();
        drone.dispose(); gunner.dispose(); swarmer.dispose(); brute.dispose();
        colossus.dispose(); queen.dispose(); coin.dispose(); medkit.dispose(); shieldIcon.dispose();
        crate.dispose(); floor.dispose(); pixel.dispose(); ring.dispose(); glow.dispose();
        font.dispose();
    }
}
