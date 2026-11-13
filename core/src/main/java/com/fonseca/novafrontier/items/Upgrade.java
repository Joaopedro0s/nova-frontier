package com.fonseca.novafrontier.items;

/** Melhorias compradas na loja entre as ondas. */
public enum Upgrade {
    HULL("Casco reforcado", "+30 vida maxima", 40, Integer.MAX_VALUE),
    SHIELD("Escudo ampliado", "+25 escudo maximo", 40, Integer.MAX_VALUE),
    ENGINE("Motores", "+8% velocidade (max 5)", 50, 5),
    DAMAGE("Calibre", "+10% dano", 60, Integer.MAX_VALUE),
    REPAIR("Reparo total", "vida e escudo cheios", 30, Integer.MAX_VALUE);

    public final String label;
    public final String description;
    public final int baseCost;
    /** Nivel maximo (o Motor e limitado para o jogador nao ficar rapido demais para a arena). */
    public final int maxLevel;

    Upgrade(String label, String description, int baseCost, int maxLevel) {
        this.label = label;
        this.description = description;
        this.baseCost = baseCost;
        this.maxLevel = maxLevel;
    }

    public boolean isMaxed(int level) { return level >= maxLevel; }

    /** Custo para o proximo nivel (o reparo tem preco fixo). */
    public int cost(int level) {
        if (this == REPAIR) return baseCost;
        return (int) (baseCost * (1f + 0.7f * level));
    }
}
