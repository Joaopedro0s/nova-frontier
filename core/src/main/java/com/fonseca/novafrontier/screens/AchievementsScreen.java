package com.fonseca.novafrontier.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.NovaFrontierGame;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.systems.AchievementManager;
import com.fonseca.novafrontier.systems.AchievementManager.Ach;
import com.fonseca.novafrontier.systems.ProfileManager;

/** Lista das 8 conquistas locais. */
public class AchievementsScreen extends BaseScreen {
    private final AchievementManager ach = new AchievementManager(ProfileManager.get());

    public AchievementsScreen(NovaFrontierGame game) {
        super(game);
    }

    @Override
    protected void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override
    protected void draw(float dt) {
        float cx = Constants.VIEW_W / 2f;
        batch.draw(assets.floor, 0, 0, Constants.VIEW_W, Constants.VIEW_H,
                   0, 0, (int) (Constants.VIEW_W / 3), (int) (Constants.VIEW_H / 3), false, false);
        assets.rect(batch, 0, 0, Constants.VIEW_W, Constants.VIEW_H, Palette.BLACK, 0.6f);
        assets.centered(batch, "CONQUISTAS", cx, 610f, 4f, Palette.YELLOW);
        assets.centered(batch, ach.unlockedCount() + " / " + Ach.values().length + " desbloqueadas",
                        cx, 556f, 1.3f, Palette.GRAY);
        Ach[] all = Ach.values();
        for (int i = 0; i < all.length; i++) {
            Ach a = all[i];
            boolean got = ach.has(a);
            float y = 520f - i * 50f;
            assets.rect(batch, 100f, y - 36f, 760f, 44f, Palette.PANEL, got ? 1f : 0.55f);
            assets.text(batch, got ? "[X]" : "[ ]", 118f, y - 6f, 1.5f, got ? Palette.GREEN : Palette.GRAY);
            assets.text(batch, a.title, 180f, y - 2f, 1.3f, got ? Palette.WHITE : Palette.GRAY);
            assets.text(batch, a.description, 180f, y - 22f, 0.95f, got ? Palette.CYAN : Palette.GRAY);
        }
        assets.centered(batch, "ESC: voltar", cx, 70f, 1.0f, Palette.GRAY);
    }
}
