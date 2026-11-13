package com.fonseca.novafrontier.items;

/** Afixos de arma (so Rara ou superior): alteram o comportamento dos projeteis no World. */
public enum WeaponAffix {
    CALIBRADA("Calibrada", "+12% chance de critico"),
    PERFURA("Perfura", "+1 inimigo perfurado"),
    CRIOGENICA("Criogenica", "-30% velocidade do alvo por 2s"),
    VAMPIRICA("Vampirica", "15% de chance de curar 3 vida ou 5 escudo ao abater"),
    COMANDO("Comando", "aliados atiram 25% mais rapido");

    public final String label;
    public final String description;

    WeaponAffix(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
