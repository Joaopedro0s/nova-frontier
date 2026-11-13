package com.fonseca.novafrontier.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.NovaFrontierGame;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.systems.AchievementManager.Ach;
import com.fonseca.novafrontier.systems.GameModeConfig;
import com.fonseca.novafrontier.ui.MenuButton;

/** Tela final: pontuacao, recorde, Dados Estelares ganhos, conquistas e opcoes de reiniciar ou voltar ao menu. */
public class GameOverScreen extends BaseScreen {
    private final GameModeConfig cfg;
    private final int score;
    private final int best;
    private final boolean newRecord;
    private final int levelReached;
    private final boolean won;
    private final int dados;
    private final int bosses;
    private final boolean debugUsed;
    private final Array<Ach> achievements;

    private final MenuButton restart = new MenuButton("REINICIAR", Constants.VIEW_W / 2f, 150f, 360f, 54f);
    private final MenuButton menu = new MenuButton("MENU", Constants.VIEW_W / 2f, 84f, 360f, 54f);
    private final MenuButton[] buttons = { restart, menu };
    private int selected;

    public GameOverScreen(NovaFrontierGame game, GameModeConfig cfg, int score, int best, boolean newRecord,
                          int levelReached, boolean won, int dados, int bosses, boolean debugUsed,
                          Array<Ach> achievements) {
        super(game);
        this.cfg = cfg;
        this.score = score;
        this.best = best;
        this.newRecord = newRecord;
        this.levelReached = levelReached;
        this.won = won;
        this.dados = dados;
        this.bosses = bosses;
        this.debugUsed = debugUsed;
        this.achievements = achievements;
    }

    @Override
    protected void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected + buttons.length - 1) % buttons.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % buttons.length;
        }

        Vector3 m = mouse();
        if (mouseMoved()) {
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i].contains(m.x, m.y)) selected = i;
            }
        }
        if (Gdx.input.justTouched()) {
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i].contains(m.x, m.y)) {
                    activate(i);
                    return;
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) activate(0);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) activate(1);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) activate(selected);
    }

    private void activate(int index) {
        if (index == 0) game.setScreen(new GameScreen(game, cfg));
        else game.setScreen(new MenuScreen(game));
    }

    @Override
    protected void draw(float dt) {
        float cx = Constants.VIEW_W / 2f;
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(assets.floor, 0, 0, Constants.VIEW_W, Constants.VIEW_H,
                   0, 0, (int) (Constants.VIEW_W / 3), (int) (Constants.VIEW_H / 3), false, false);
        assets.rect(batch, 0, 0, Constants.VIEW_W, Constants.VIEW_H, Palette.BLACK, 0.5f);

        Color titleColor = won ? Palette.GREEN : Palette.RED;
        String title = won ? "VITORIA!" : "GAME OVER";
        float bob = MathUtils.sin(time * 3f) * 3f;
        assets.centered(batch, title, cx + 4f, 606f + bob - 4f, 6f, Palette.BLACK, 0.8f);
        assets.centered(batch, title, cx, 606f + bob, 6f, titleColor);
        assets.centered(batch, cfg.label(), cx, 540f, 1.3f, Palette.CYAN);

        assets.draw(batch, won ? assets.player : assets.drone, 150f, 400f, 96f);
        assets.draw(batch, won ? assets.colossus : assets.brute, 810f, 400f, 110f);

        assets.centered(batch, "PONTUACAO FINAL", cx, 500f, 1.3f, Palette.GRAY);
        assets.centered(batch, String.valueOf(score), cx, 478f, 3.4f, Palette.YELLOW);
        if (newRecord) {
            assets.centered(batch, "NOVO RECORDE!", cx, 424f, 1.5f, Palette.GREEN, 0.6f + 0.4f * MathUtils.sin(time * 10f));
        } else {
            assets.centered(batch, "Melhor: " + best, cx, 422f, 1.3f, Palette.CYAN);
        }
        String reached = won ? "Missao cumprida" : cfg.unit().charAt(0) + cfg.unit().substring(1).toLowerCase()
                + " alcancada: " + levelReached;
        assets.centered(batch, reached + "   |   Bosses derrotados: " + bosses, cx, 398f, 1.1f, Palette.GRAY);

        if (debugUsed) {
            assets.centered(batch, "Debug usado nesta run: sem Dados Estelares nem conquistas", cx, 366f, 1.2f, Palette.RED);
        } else {
            assets.centered(batch, "+" + dados + " DADOS ESTELARES", cx, 366f, 1.6f, Palette.CYAN);
        }
        if (achievements.size > 0) {
            assets.centered(batch, "CONQUISTAS DESBLOQUEADAS", cx, 328f, 1.1f, Palette.YELLOW);
            for (int i = 0; i < achievements.size && i < 4; i++) {
                assets.centered(batch, achievements.get(i).title, cx, 304f - i * 20f, 1.1f, Palette.WHITE);
            }
        }

        for (int i = 0; i < buttons.length; i++) buttons[i].render(batch, assets, i == selected, time);
        assets.centered(batch, "R: reiniciar     M ou ESC: menu     ENTER: confirmar", cx, 40f, 0.95f, Palette.GRAY);
    }
}
