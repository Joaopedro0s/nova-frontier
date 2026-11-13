package com.fonseca.novafrontier.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.NovaFrontierGame;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.items.WeaponType;
import com.fonseca.novafrontier.systems.ProfileManager;
import com.fonseca.novafrontier.systems.SoundManager;

/** Hangar: gasta Dados Estelares em melhorias permanentes e desbloqueios. */
public class HangarScreen extends BaseScreen {
    private static final int ROWS = 8;
    private final ProfileManager profile = ProfileManager.get();
    private int selected;
    private String message = "";
    private Color messageColor = Palette.GRAY;

    public HangarScreen(NovaFrontierGame game) {
        super(game);
    }

    @Override
    protected void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected + ROWS - 1) % ROWS;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % ROWS;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) buy();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    private void buy() {
        boolean ok;
        String what;
        if (selected == 0) {
            what = "Ima de Coleta";
            ok = profile.buyMagnet();
        } else if (selected == 1) {
            what = "Protocolo de Reanimacao";
            ok = profile.buyRevive();
        } else if (selected <= 4) {
            WeaponType t = WeaponType.values()[selected - 1];
            what = "Arma inicial " + t.label;
            ok = profile.unlockWeapon(t);
        } else {
            int skin = selected - 4;
            what = "Paleta " + ProfileManager.SKIN_NAMES[skin];
            ok = profile.unlockSkin(skin);
        }
        if (ok) {
            message = what + ": comprado!";
            messageColor = Palette.GREEN;
            SoundManager.play(SoundManager.Sfx.SHOP);
        } else {
            message = what + ": indisponivel (ja comprado ou Dados Estelares insuficientes)";
            messageColor = Palette.RED;
        }
    }

    private String status(int row) {
        ProfileManager.Data d = profile.data;
        switch (row) {
            case 0:
                return d.magnet >= ProfileManager.MAX_LEVEL ? "MAX (" + (int) profile.magnetRadius() + "px)"
                        : "nv " + d.magnet + "/3  ->  " + ProfileManager.MAGNET_COST[d.magnet] + " DE";
            case 1:
                return d.revive >= ProfileManager.MAX_LEVEL ? "MAX (" + (int) profile.reviveTime() + "s)"
                        : "nv " + d.revive + "/3  ->  " + ProfileManager.REVIVE_COST[d.revive] + " DE";
            case 2: case 3: case 4: {
                WeaponType t = WeaponType.values()[row - 1];
                return profile.isWeaponUnlocked(t) ? "DESBLOQUEADA"
                        : ProfileManager.WEAPON_COST[row - 2] + " DE";
            }
            default: {
                int skin = row - 4;
                return profile.isSkinUnlocked(skin) ? "DESBLOQUEADA" : ProfileManager.SKIN_COST[skin] + " DE";
            }
        }
    }

    private String title(int row) {
        switch (row) {
            case 0: return "Ima de Coleta  (atracao 150 -> 225px)";
            case 1: return "Protocolo de Reanimacao  (revive 10 -> 7s)";
            case 2: case 3: case 4: return "Arma inicial: " + WeaponType.values()[row - 1].label;
            default: return "Paleta da nave: " + ProfileManager.SKIN_NAMES[row - 4];
        }
    }

    @Override
    protected void draw(float dt) {
        float cx = Constants.VIEW_W / 2f;
        batch.draw(assets.floor, 0, 0, Constants.VIEW_W, Constants.VIEW_H,
                   0, 0, (int) (Constants.VIEW_W / 3), (int) (Constants.VIEW_H / 3), false, false);
        assets.rect(batch, 0, 0, Constants.VIEW_W, Constants.VIEW_H, Palette.BLACK, 0.6f);
        assets.centered(batch, "HANGAR", cx, 616f, 4f, Palette.CYAN);
        assets.centered(batch, "DADOS ESTELARES: " + profile.data.dados, cx, 566f, 1.6f, Palette.YELLOW);
        for (int i = 0; i < ROWS; i++) {
            float y = 528f - i * 46f;
            boolean sel = i == selected;
            boolean done = status(i).startsWith("DESBLOQ") || status(i).startsWith("MAX");
            assets.rect(batch, 80f, y - 34f, 800f, 40f, Palette.PANEL, sel ? 1f : 0.55f);
            if (sel) assets.rect(batch, 76f, y - 38f, 4f, 48f, Palette.YELLOW, 1f);
            if (i >= 5) assets.draw(batch, assets.playerSkin(i - 4), 112f, y - 14f, 30f);
            assets.text(batch, title(i), i >= 5 ? 140f : 100f, y - 4f, 1.15f, sel ? Palette.YELLOW : Palette.WHITE);
            assets.text(batch, status(i), 640f, y - 4f, 1.1f, done ? Palette.GREEN : Palette.ORANGE);
        }
        assets.centered(batch, message, cx, 128f, 1.0f, messageColor);
        assets.centered(batch, "CIMA/BAIXO: escolher   ENTER: comprar   ESC: voltar   (DE = Dados Estelares)",
                        cx, 90f, 1.0f, Palette.GRAY);
        assets.centered(batch, "Ganhe Dados Estelares ao fim de cada run: (ondas x10 + bosses x25) x dificuldade",
                        cx, 64f, 0.95f, Palette.GRAY);
    }
}
