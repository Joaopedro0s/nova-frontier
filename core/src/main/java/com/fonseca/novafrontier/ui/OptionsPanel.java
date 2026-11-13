package com.fonseca.novafrontier.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.systems.ProfileManager;
import com.fonseca.novafrontier.systems.SoundManager;

/**
 * Opcoes de desktop usadas no menu principal e no menu de pausa: volume geral e de efeitos
 * (0-100%), tela cheia/janela (tambem F11) e intensidade do screen shake (100/50/0%).
 * Teclado: CIMA/BAIXO escolhem a linha, ESQUERDA/DIREITA mudam o valor.
 */
public class OptionsPanel {
    private static final String[] ROWS = { "Volume geral", "Volume efeitos", "Screen shake", "Tela cheia (F11)" };
    private static final String[] SHAKE = { "100%", "50%", "0%" };

    private final ProfileManager profile = ProfileManager.get();
    private int selected;

    public void update() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected + ROWS.length - 1) % ROWS.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % ROWS.length;
        }
        int dir = 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) dir = -1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) dir = 1;
        if (dir == 0 && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) dir = 1;
        if (dir == 0) return;
        ProfileManager.Data d = profile.data;
        switch (selected) {
            case 0:
                d.volMaster = clampStep(d.volMaster + dir * 10);
                break;
            case 1:
                d.volSfx = clampStep(d.volSfx + dir * 10);
                SoundManager.play(SoundManager.Sfx.PICKUP); // amostra do volume
                break;
            case 2:
                d.shake = (d.shake + dir + SHAKE.length) % SHAKE.length;
                break;
            default:
                toggleFullscreen();
                return;
        }
        profile.save();
    }

    private static int clampStep(int v) { return Math.max(0, Math.min(100, v)); }

    /** Alterna tela cheia/janela e grava no perfil (usado tambem pela tecla F11). */
    public static void toggleFullscreen() {
        try {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode((int) Constants.VIEW_W, (int) Constants.VIEW_H);
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
            ProfileManager p = ProfileManager.get();
            p.data.fullscreen = Gdx.graphics.isFullscreen();
            p.save();
        } catch (Throwable ignored) {
            // sem suporte a troca de modo: ignora
        }
    }

    /** Desenha as 4 linhas a partir de topY, centradas em cx. */
    public void draw(SpriteBatch b, Assets a, float cx, float topY) {
        ProfileManager.Data d = profile.data;
        String[] values = {
            d.volMaster + "%", d.volSfx + "%", SHAKE[d.shake],
            Gdx.graphics != null && Gdx.graphics.isFullscreen() ? "TELA CHEIA" : "JANELA"
        };
        for (int i = 0; i < ROWS.length; i++) {
            float y = topY - i * 40f;
            boolean sel = i == selected;
            a.rect(b, cx - 280f, y - 28f, 560f, 34f, Palette.PANEL, sel ? 1f : 0.55f);
            a.text(b, (sel ? "> " : "  ") + ROWS[i], cx - 264f, y - 2f, 1.3f, sel ? Palette.YELLOW : Palette.WHITE);
            a.text(b, "< " + values[i] + " >", cx + 80f, y - 2f, 1.3f, sel ? Palette.CYAN : Palette.GRAY);
        }
        a.centered(b, "CIMA/BAIXO: escolher   ESQUERDA/DIREITA: mudar", cx, topY - ROWS.length * 40f - 4f, 0.9f, Palette.GRAY);
    }
}
