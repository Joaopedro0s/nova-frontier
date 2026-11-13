package com.fonseca.novafrontier.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.NovaFrontierGame;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.items.WeaponType;
import com.fonseca.novafrontier.systems.GameModeConfig;
import com.fonseca.novafrontier.systems.GameModeConfig.Difficulty;
import com.fonseca.novafrontier.systems.GameModeConfig.Mode;
import com.fonseca.novafrontier.systems.ProfileManager;

/** Preparacao da run: modo, dificuldade, arma inicial e paleta da nave. */
public class SetupScreen extends BaseScreen {
    private static Mode lastMode = Mode.OPERACAO;
    private static Difficulty lastDiff = Difficulty.DIFICIL;

    private static final int ROW_MODE = 0, ROW_DIFF = 1, ROW_WEAPON = 2, ROW_SKIN = 3, ROW_START = 4;

    private final ProfileManager profile = ProfileManager.get();
    private int selected = ROW_START;

    public SetupScreen(NovaFrontierGame game) {
        super(game);
    }

    private boolean diffEnabled() { return lastMode != Mode.TORRE; }

    @Override
    protected void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) move(-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) move(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) change(-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) change(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selected == ROW_START) start();
            else change(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    private void move(int dir) {
        do {
            selected = (selected + 5 + dir) % 5;
        } while (selected == ROW_DIFF && !diffEnabled());
    }

    private void change(int dir) {
        ProfileManager.Data d = profile.data;
        switch (selected) {
            case ROW_MODE:
                Mode[] modes = Mode.values();
                lastMode = modes[(lastMode.ordinal() + dir + modes.length) % modes.length];
                break;
            case ROW_DIFF:
                Difficulty[] diffs = Difficulty.values();
                lastDiff = diffs[(lastDiff.ordinal() + dir + diffs.length) % diffs.length];
                break;
            case ROW_WEAPON: {
                WeaponType[] all = WeaponType.values();
                int i = profile.startWeapon().ordinal();
                for (int n = 0; n < all.length; n++) {
                    i = (i + dir + all.length) % all.length;
                    if (profile.isWeaponUnlocked(all[i])) break;
                }
                d.startWeapon = i;
                profile.save();
                break;
            }
            case ROW_SKIN: {
                int n = ProfileManager.SKIN_NAMES.length;
                int i = profile.skin();
                for (int k = 0; k < n; k++) {
                    i = (i + dir + n) % n;
                    if (profile.isSkinUnlocked(i)) break;
                }
                d.skin = i;
                profile.save();
                break;
            }
            default: break;
        }
    }

    private void start() {
        GameModeConfig cfg = new GameModeConfig(lastMode, lastDiff);
        cfg.startWeapon = profile.startWeapon();
        game.setScreen(new GameScreen(game, cfg));
    }

    @Override
    protected void draw(float dt) {
        float cx = Constants.VIEW_W / 2f;
        batch.draw(assets.floor, 0, 0, Constants.VIEW_W, Constants.VIEW_H,
                   0, 0, (int) (Constants.VIEW_W / 3), (int) (Constants.VIEW_H / 3), false, false);
        assets.rect(batch, 0, 0, Constants.VIEW_W, Constants.VIEW_H, Palette.BLACK, 0.6f);
        assets.centered(batch, "PREPARAR MISSAO", cx, 612f, 4f, Palette.CYAN);

        String[] labels = { "Modo de jogo", "Dificuldade", "Arma inicial", "Paleta da nave" };
        String[] values = {
            lastMode.label,
            diffEnabled() ? lastDiff.label : "(padrao)",
            profile.startWeapon().label,
            ProfileManager.SKIN_NAMES[profile.skin()]
        };
        for (int i = 0; i < 4; i++) {
            float y = 536f - i * 52f;
            boolean sel = i == selected;
            boolean off = i == ROW_DIFF && !diffEnabled();
            assets.rect(batch, 140f, y - 38f, 680f, 44f, Palette.PANEL, sel ? 1f : 0.55f);
            assets.text(batch, (sel ? "> " : "  ") + labels[i], 160f, y - 6f, 1.4f,
                        off ? Palette.GRAY : (sel ? Palette.YELLOW : Palette.WHITE));
            assets.text(batch, off ? values[i] : "< " + values[i] + " >", 520f, y - 6f, 1.4f,
                        off ? Palette.GRAY : (sel ? Palette.CYAN : Palette.WHITE));
        }
        assets.draw(batch, assets.playerSkin(profile.skin()), 880f, 470f, 60f);

        float sy = 536f - 4 * 52f;
        boolean sel = selected == ROW_START;
        assets.rect(batch, 140f, sy - 38f, 680f, 44f, sel ? Palette.GREEN : Palette.PANEL, sel ? 0.35f : 0.55f);
        assets.centered(batch, sel ? "> INICIAR <" : "INICIAR", cx, sy - 6f, 1.8f, sel ? Palette.GREEN : Palette.WHITE);

        assets.centered(batch, lastMode.label + ": " + lastMode.description, cx, 250f, 1.2f, Palette.ORANGE);
        if (diffEnabled()) {
            String d;
            switch (lastDiff) {
                case FACIL: d = "Vida/dano inimigo x0.8, drops de cura +25%"; break;
                case HARD: d = "Vida x1.35, dano x1.25, tiros +20%, drops somem em 15s, bosses furiosos aos 65%, score x1.5"; break;
                default: d = "Valores originais do jogo"; break;
            }
            assets.centered(batch, lastDiff.label + ": " + d, cx, 224f, 1.0f, Palette.WHITE);
        } else {
            assets.centered(batch, "Cada andar sorteia um mutator; entre andares voce escolhe 1 de 3 melhorias gratis",
                            cx, 224f, 1.0f, Palette.WHITE);
        }
        assets.centered(batch, "Desbloqueie armas e paletas no Hangar com Dados Estelares", cx, 190f, 0.95f, Palette.GRAY);
        assets.centered(batch, "CIMA/BAIXO: escolher   ESQUERDA/DIREITA: mudar   ENTER: iniciar   ESC: voltar",
                        cx, 70f, 1.0f, Palette.GRAY);
    }
}
