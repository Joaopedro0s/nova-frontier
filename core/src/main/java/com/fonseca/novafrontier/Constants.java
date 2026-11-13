package com.fonseca.novafrontier;

/** Constantes globais. */
public final class Constants {
    private Constants() {}

    /** Area visivel (a janela escala via FitViewport). */
    public static final float VIEW_W = 960f;
    public static final float VIEW_H = 640f;
    /** Mundo jogavel, maior que a tela (a camera segue o jogador). */
    public static final float WORLD_W = 1920f;
    public static final float WORLD_H = 1280f;

    public static final int PIXEL_SCALE = 3;
    public static final int TOTAL_WAVES = 10;

    public static boolean isBossWave(int wave) {
        return wave == 5 || wave == 10;
    }
}
