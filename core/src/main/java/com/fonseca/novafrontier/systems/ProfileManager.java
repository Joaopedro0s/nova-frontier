package com.fonseca.novafrontier.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.fonseca.novafrontier.items.WeaponType;

/**
 * Perfil do jogador serializado em JSON dentro de Preferences ("nova-frontier-profile").
 * Guarda recorde, Dados Estelares, melhorias do hangar, conquistas e opcoes.
 * Migra o recorde antigo (Preferences "nova-frontier", chave "best") na primeira leitura.
 * Sem Gdx.app (testes headless) funciona so em memoria.
 */
public class ProfileManager {

    /** Dados persistidos (campos publicos para o Json do libGDX). */
    public static class Data {
        public int version = 1;
        public int bestScore;
        public int dados;
        public int runs;
        public int bestWaveEndless;
        public int bestFloor;
        // hangar
        public int magnet;
        public int revive;
        public boolean unlockScatter, unlockLaser, unlockPlasma;
        public int skinMask = 1; // bit 0 = padrao (sempre)
        public int skin;
        public int startWeapon; // ordinal de WeaponType
        // conquistas
        public int achMask;
        // opcoes
        public int volMaster = 80;
        public int volSfx = 80;
        public int shake; // 0 = 100%, 1 = 50%, 2 = 0%
        public boolean fullscreen;
    }

    public static final String PREFS = "nova-frontier-profile";
    public static final String LEGACY_PREFS = "nova-frontier";
    private static final String KEY = "profile";

    public static final int[] MAGNET_COST = { 30, 60, 100 };
    public static final int[] REVIVE_COST = { 30, 60, 100 };
    public static final int MAX_LEVEL = 3;
    public static final String[] SKIN_NAMES = { "Padrao", "Carmesim", "Cyber Ouro", "Void Roxo" };
    public static final int[] SKIN_COST = { 0, 40, 60, 60 };
    /** Custo para desbloquear a arma inicial: SCATTER, LASER, PLASMA. */
    public static final int[] WEAPON_COST = { 50, 80, 120 };

    private static ProfileManager instance;
    private static boolean memoryOnly;

    public Data data = new Data();
    private final Json json = new Json();

    private ProfileManager() {
        json.setOutputType(JsonWriter.OutputType.json);
        json.setIgnoreUnknownFields(true);
        load();
    }

    public static synchronized ProfileManager get() {
        if (instance == null) instance = new ProfileManager();
        return instance;
    }

    /** Usado nos testes: descarta o perfil atual e nao toca em Preferences. */
    public static synchronized void useMemoryOnly() {
        memoryOnly = true;
        instance = new ProfileManager();
    }

    // ------------------------------------------------------------------ persistencia
    private void load() {
        if (memoryOnly) return;
        try {
            Preferences p = Gdx.app.getPreferences(PREFS);
            String raw = p.getString(KEY, "");
            if (!raw.isEmpty()) {
                Data d = json.fromJson(Data.class, raw);
                if (d != null) data = d;
            } else {
                // retrocompatibilidade com o recorde antigo
                Preferences old = Gdx.app.getPreferences(LEGACY_PREFS);
                data.bestScore = old.getInteger("best", 0);
                if (data.bestScore > 0) save();
            }
        } catch (Throwable t) {
            data = new Data(); // sem persistencia: segue em memoria
        }
    }

    public void save() {
        if (memoryOnly) return;
        try {
            Preferences p = Gdx.app.getPreferences(PREFS);
            p.putString(KEY, json.toJson(data));
            p.flush();
        } catch (Throwable ignored) {
            // sem persistencia: segue o jogo
        }
    }

    public String toJson() { return json.toJson(data); }

    // ------------------------------------------------------------------ meta-progressao
    /** Dados Estelares ganhos numa run: (ondas*10 + bosses*25) * multiplicador de dificuldade. */
    public static int starData(int wavesCleared, int bossesKilled, float difficultyMult) {
        return Math.round((wavesCleared * 10 + bossesKilled * 25) * difficultyMult);
    }

    public float magnetRadius() { return 150f + 25f * data.magnet; }

    public float reviveTime() { return 10f - data.revive; }

    public boolean buyMagnet() {
        if (data.magnet >= MAX_LEVEL || data.dados < MAGNET_COST[data.magnet]) return false;
        data.dados -= MAGNET_COST[data.magnet++];
        save();
        return true;
    }

    public boolean buyRevive() {
        if (data.revive >= MAX_LEVEL || data.dados < REVIVE_COST[data.revive]) return false;
        data.dados -= REVIVE_COST[data.revive++];
        save();
        return true;
    }

    public boolean isWeaponUnlocked(WeaponType t) {
        switch (t) {
            case SCATTER: return data.unlockScatter;
            case LASER: return data.unlockLaser;
            case PLASMA: return data.unlockPlasma;
            default: return true;
        }
    }

    public boolean unlockWeapon(WeaponType t) {
        if (t == WeaponType.BLASTER || isWeaponUnlocked(t)) return false;
        int cost = WEAPON_COST[t.ordinal() - 1];
        if (data.dados < cost) return false;
        data.dados -= cost;
        if (t == WeaponType.SCATTER) data.unlockScatter = true;
        else if (t == WeaponType.LASER) data.unlockLaser = true;
        else data.unlockPlasma = true;
        save();
        return true;
    }

    public boolean isSkinUnlocked(int i) { return (data.skinMask & (1 << i)) != 0; }

    public boolean unlockSkin(int i) {
        if (i <= 0 || i >= SKIN_NAMES.length || isSkinUnlocked(i) || data.dados < SKIN_COST[i]) return false;
        data.dados -= SKIN_COST[i];
        data.skinMask |= 1 << i;
        save();
        return true;
    }

    public WeaponType startWeapon() {
        WeaponType[] all = WeaponType.values();
        WeaponType t = all[Math.max(0, Math.min(all.length - 1, data.startWeapon))];
        return isWeaponUnlocked(t) ? t : WeaponType.BLASTER;
    }

    public int skin() { return isSkinUnlocked(data.skin) ? data.skin : 0; }

    // ------------------------------------------------------------------ opcoes
    public float masterVolume() { return data.volMaster / 100f; }

    public float sfxVolume() { return data.volSfx / 100f; }

    /** Escala do screen shake: 100%, 50% ou 0%. */
    public float shakeScale() { return data.shake == 0 ? 1f : (data.shake == 1 ? 0.5f : 0f); }
}
