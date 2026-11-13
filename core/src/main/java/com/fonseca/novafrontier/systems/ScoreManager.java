package com.fonseca.novafrontier.systems;


/** Pontuacao, combo de abates e recorde (persistido no perfil, ver ProfileManager). */
public class ScoreManager {
    public static final float COMBO_WINDOW = 3f;
    private int score;
    private int streak;
    private float streakTimer;
    private int best;
    /** Multiplicador de pontuacao da dificuldade (Pesadelo = x1.5). */
    private float difficultyMult = 1f;

    public ScoreManager() {
        best = ProfileManager.get().data.bestScore;
    }

    public void setDifficultyMult(float m) { difficultyMult = m; }

    public void update(float dt) {
        if (streakTimer > 0f) {
            streakTimer -= dt;
            if (streakTimer <= 0f) streak = 0;
        }
    }

    /** Registra um abate; devolve os pontos ganhos (com multiplicador de sequencia). */
    public int kill(int basePoints) {
        streak++;
        streakTimer = COMBO_WINDOW;
        int pts = Math.round(basePoints * multiplier() * difficultyMult);
        score += pts;
        return pts;
    }

    public void addBonus(int pts) { score += Math.round(pts * difficultyMult); }

    public void breakStreak() {
        streak = 0;
        streakTimer = 0f;
    }

    /** x1 ate 4 abates, +1 a cada 5, maximo x5. */
    public int multiplier() { return Math.min(5, 1 + streak / 5); }

    public float streakFraction() { return streakTimer / COMBO_WINDOW; }

    public int getScore() { return score; }
    public int getBest() { return best; }

    public boolean commitBest() {
        if (score > best) {
            best = score;
            ProfileManager.get().data.bestScore = best;
            ProfileManager.get().save();
            return true;
        }
        return false;
    }
}
