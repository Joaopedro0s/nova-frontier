package com.fonseca.novafrontier.systems;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.fonseca.novafrontier.Palette;
import com.fonseca.novafrontier.entities.Player;
import com.fonseca.novafrontier.items.PassiveModule;
import com.fonseca.novafrontier.items.Upgrade;
import com.fonseca.novafrontier.items.Weapon;

/** Loja estilo draft do Modo Torre: 3 opcoes gratuitas sorteadas, o jogador escolhe so uma. */
public final class Draft {
    private Draft() {}

    public static final int OPTIONS = 3;

    public static final class Option {
        public final String title;
        public final String description;
        public final Color color;
        private final Runnable effect;

        Option(String title, String description, Color color, Runnable effect) {
            this.title = title;
            this.description = description;
            this.color = color;
            this.effect = effect;
        }

        public void apply() { effect.run(); }
    }

    /** Sorteia 3 opcoes distintas para o estado atual do jogador. */
    public static Array<Option> roll(final Player p, final World w) {
        Array<Option> pool = new Array<>();

        // +1 nivel de um upgrade especifico
        for (final Upgrade u : Upgrade.values()) {
            if (u == Upgrade.REPAIR || u.isMaxed(p.upgradeLevels[u.ordinal()])) continue;
            pool.add(new Option("+1 " + u.label, u.description, Palette.GREEN, new Runnable() {
                @Override public void run() {
                    p.applyUpgrade(u);
                    w.feed.system("Draft: +1 " + u.label);
                }
            }));
        }

        // cura 50% da vida + arma atual sobe de raridade (ou +1 Calibre se ja for Lendaria)
        pool.add(new Option("Reparo + Arma", "Cura 50% da vida e a arma atual sobe de raridade",
                Palette.ORANGE, new Runnable() {
            @Override public void run() {
                p.heal(p.maxHp * 0.5f);
                Weapon cur = p.weapon();
                Weapon up = cur.upgraded();
                if (up == cur) {
                    p.applyUpgrade(Upgrade.DAMAGE);
                    w.feed.system("Draft: reparo + Calibre (arma ja e Lendaria)");
                } else {
                    p.weapons.set(p.current, up);
                    w.feed.post("Draft: " + up.name() + " [" + up.affixText() + "]", up.rarity.color);
                    w.checkLegendaryCollector();
                }
            }
        }));

        // modulos passivos ainda nao equipados (se houver slot)
        if (p.modules.size < PassiveModule.MAX_SLOTS) {
            for (final PassiveModule m : PassiveModule.values()) {
                if (p.hasModule(m)) continue;
                pool.add(new Option("Modulo: " + m.label, m.description, m.color, new Runnable() {
                    @Override public void run() {
                        p.equipModule(m);
                        w.feed.post("Draft: modulo " + m.label, m.color);
                    }
                }));
            }
        }

        pool.shuffle();
        Array<Option> out = new Array<>();
        for (int i = 0; i < OPTIONS && i < pool.size; i++) out.add(pool.get(i));
        return out;
    }
}
