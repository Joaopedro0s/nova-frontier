package com.fonseca.novafrontier.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.NovaFrontierGame;

/** Ponto de entrada desktop (LWJGL3). */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Nova Frontier");
        config.setWindowedMode((int) Constants.VIEW_W, (int) Constants.VIEW_H);
        config.setWindowSizeLimits(640, 427, -1, -1);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new NovaFrontierGame(), config);
    }
}
