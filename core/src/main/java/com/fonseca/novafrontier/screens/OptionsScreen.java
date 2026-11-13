package com.fonseca.novafrontier.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.NovaFrontierGame;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.ui.OptionsPanel;

/** Opcoes de desktop acessadas pelo menu principal. */
public class OptionsScreen extends BaseScreen {
    private final OptionsPanel panel = new OptionsPanel();

    public OptionsScreen(NovaFrontierGame game) {
        super(game);
    }

    @Override
    protected void update(float dt) {
        panel.update();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override
    protected void draw(float dt) {
        float cx = Constants.VIEW_W / 2f;
        batch.draw(assets.floor, 0, 0, Constants.VIEW_W, Constants.VIEW_H,
                   0, 0, (int) (Constants.VIEW_W / 3), (int) (Constants.VIEW_H / 3), false, false);
        assets.rect(batch, 0, 0, Constants.VIEW_W, Constants.VIEW_H, Palette.BLACK, 0.6f);
        assets.centered(batch, "OPCOES", cx, 590f, 4f, Palette.CYAN);
        panel.draw(batch, assets, cx, 450f);
        assets.centered(batch, "F11 alterna tela cheia em qualquer tela   |   ESC: voltar", cx, 120f, 1.0f, Palette.GRAY);
    }
}
