package com.fonseca.novafrontier.systems;

import com.badlogic.gdx.utils.Array;

/** 8 conquistas locais, verificadas em tempo real pelo World e persistidas no perfil. */
public class AchievementManager {

    public enum Ach {
        PRIMEIRA_LIMPEZA("Primeira Limpeza", "Vencer a onda 10 em qualquer dificuldade"),
        VETERANO("Veterano da Fronteira", "Vencer a onda 10 no modo Pesadelo"),
        SOBREVIVENTE("Sobrevivente", "Alcancar a onda 20 no Modo Infinito"),
        CONQUISTADOR_TORRE("Conquistador da Torre", "Vencer o andar 20 no Modo Torre"),
        INTOCAVEL("Intocavel", "Derrotar um boss sem perder vida (so escudo)"),
        MIRA_LASER("Mira Laser", "Vencer uma onda de boss com a auto-mira desligada"),
        COLECIONADOR_LENDARIO("Colecionador Lendario", "Equipar 2 armas Lendarias na mesma partida"),
        COMBO_MAXIMO("Combo Maximo", "Manter o combo x5 por 15 segundos seguidos");

        public final String title;
        public final String description;

        Ach(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    public static final float COMBO_HOLD_TIME = 15f;

    private final ProfileManager profile;
    private final Array<Ach> pending = new Array<>();
    private float comboTimer;
    private boolean enabled = true;

    public AchievementManager(ProfileManager profile) {
        this.profile = profile;
    }

    /** Desligado em runs com debug: nada e desbloqueado. */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean has(Ach a) { return (profile.data.achMask & (1 << a.ordinal())) != 0; }

    public int unlockedCount() {
        int n = 0;
        for (Ach a : Ach.values()) if (has(a)) n++;
        return n;
    }

    /** Desbloqueia; devolve true se for nova. */
    public boolean unlock(Ach a) {
        if (!enabled || has(a)) return false;
        profile.data.achMask |= 1 << a.ordinal();
        profile.save();
        pending.add(a);
        return true;
    }

    /** Conquistas desbloqueadas desde a ultima chamada (para alertas no HUD e chat). */
    public Array<Ach> drainPending() {
        Array<Ach> out = new Array<>(pending);
        pending.clear();
        return out;
    }

    /** Chamado a cada frame de luta: mede quanto tempo o combo x5 foi mantido. */
    public void update(float dt, int comboMultiplier) {
        if (comboMultiplier >= 5) {
            comboTimer += dt;
            if (comboTimer >= COMBO_HOLD_TIME) unlock(Ach.COMBO_MAXIMO);
        } else {
            comboTimer = 0f;
        }
    }
}
