package com.fonseca.novafrontier.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.NovaFrontierGame;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.systems.ProfileManager;
import com.fonseca.novafrontier.ui.MenuButton;

/** Menu inicial: JOGAR / HANGAR / CONQUISTAS / OPCOES / SAIR. */
public class MenuScreen extends BaseScreen {
    private final MenuButton[] buttons = {
        new MenuButton("JOGAR", Constants.VIEW_W / 2f, 452f, 320f, 50f),
        new MenuButton("HANGAR", Constants.VIEW_W / 2f, 392f, 320f, 50f),
        new MenuButton("CONQUISTAS", Constants.VIEW_W / 2f, 332f, 320f, 50f),
        new MenuButton("OPCOES", Constants.VIEW_W / 2f, 272f, 320f, 50f),
        new MenuButton("SAIR", Constants.VIEW_W / 2f, 212f, 320f, 50f)
    };
    private int selected;

    public MenuScreen(NovaFrontierGame game) {
        super(game);
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
            for (int i = 0; i < buttons.length; i++) if (buttons[i].contains(m.x, m.y)) selected = i;
        }
        if (Gdx.input.justTouched()) {
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i].contains(m.x, m.y)) {
                    activate(i);
                    return;
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            activate(selected);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    private void activate(int index) {
        switch (index) {
            case 0: game.setScreen(new SetupScreen(game)); break;
            case 1: game.setScreen(new HangarScreen(game)); break;
            case 2: game.setScreen(new AchievementsScreen(game)); break;
            case 3: game.setScreen(new OptionsScreen(game)); break;
            default: Gdx.app.exit();
        }
    }

    @Override
    protected void draw(float dt) {
        float cx = Constants.VIEW_W / 2f;
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(assets.floor, 0, 0, Constants.VIEW_W, Constants.VIEW_H,
                   0, 0, (int) (Constants.VIEW_W / 3), (int) (Constants.VIEW_H / 3), false, false);
        assets.rect(batch, 0, 0, Constants.VIEW_W, Constants.VIEW_H, Palette.BLACK, 0.4f);

        // frota decorativa patrulhando
        for (int i = 0; i < 7; i++) {
            float x = ((time * (40f + i * 13f)) + i * 170f) % (Constants.VIEW_W + 120f) - 60f;
            float y = 90f + (i * 83f) % 480f + MathUtils.sin(time * 2f + i) * 12f;
            switch (i % 4) {
                case 0: assets.draw(batch, assets.drone, x, y, 48f); break;
                case 1: assets.draw(batch, assets.swarmer, x, y, 36f); break;
                case 2: assets.draw(batch, assets.gunner, x, y, 52f); break;
                default: assets.draw(batch, assets.brute, x, y, 72f); break;
            }
        }

        float bob = MathUtils.sin(time * 2f) * 4f;
        assets.centered(batch, "NOVA FRONTIER", cx + 4f, 612f + bob - 4f, 6f, Palette.BLACK, 0.8f);
        assets.centered(batch, "NOVA FRONTIER", cx, 612f + bob, 6f, Palette.CYAN);
        assets.centered(batch, "Co-op sci-fi: armas, itens, ondas e bosses ao lado do seu grupo",
                        cx, 540f, 1.3f, Palette.YELLOW);
        assets.text(batch, "DADOS ESTELARES: " + ProfileManager.get().data.dados, 16f, 630f, 1.0f, Palette.CYAN);

        assets.draw(batch, assets.playerSkin(ProfileManager.get().skin()), 240f, 350f, 96f);
        assets.draw(batch, assets.colossus, 730f, 340f, 130f);
        for (int i = 0; i < buttons.length; i++) buttons[i].render(batch, assets, i == selected, time);

        assets.panel(batch, 100f, 16f, 760f, 132f, Palette.DARK, Palette.GRAY, 0.85f);
        assets.centered(batch, "COMO JOGAR", cx, 140f, 1.3f, Palette.YELLOW);
        assets.centered(batch, "WASD mover | MOUSE mirar | ESPACO dash | 1-4 / Q / E armas | F auto-tiro | T auto-mira | C postura do grupo",
                        cx, 114f, 0.9f, Palette.WHITE);
        assets.centered(batch, "Modos: Operacao (10 ondas), Infinito (sem limite) e Torre (20 andares com mutators)",
                        cx, 90f, 0.95f, Palette.RED);
        assets.centered(batch, "Colete creditos, medkits, escudos, armas com afixos e modulos dos bosses",
                        cx, 66f, 0.95f, Palette.ORANGE);
        assets.centered(batch, "F3: debug   |   F11: tela cheia   |   Dados Estelares ficam no Hangar",
                        cx, 42f, 0.95f, Palette.GREEN);
    }
}
