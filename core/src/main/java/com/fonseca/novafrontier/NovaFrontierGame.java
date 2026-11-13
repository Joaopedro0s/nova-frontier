package com.fonseca.novafrontier;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.fonseca.novafrontier.screens.MenuScreen;
import com.fonseca.novafrontier.systems.ProfileManager;
import com.fonseca.novafrontier.systems.SoundManager;
import com.fonseca.novafrontier.ui.OptionsPanel;

/** Classe principal: cria os recursos compartilhados e abre o menu. */
public class NovaFrontierGame extends Game {
    public SpriteBatch batch;
    public Assets assets;

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets = new Assets();
        SoundManager.init();
        if (ProfileManager.get().data.fullscreen) OptionsPanel.toggleFullscreen();
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        SoundManager.shutdown();
        batch.dispose();
        assets.dispose();
    }
}
