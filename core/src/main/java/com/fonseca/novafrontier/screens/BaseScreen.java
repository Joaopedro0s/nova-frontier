package com.fonseca.novafrontier.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fonseca.novafrontier.Assets;
import com.fonseca.novafrontier.Constants;
import com.fonseca.novafrontier.NovaFrontierGame;
import com.fonseca.novafrontier.ui.OptionsPanel;

/** Base das telas: camera, viewport (escala a janela) e ciclo update/draw. */
public abstract class BaseScreen extends ScreenAdapter {
    protected final NovaFrontierGame game;
    protected final SpriteBatch batch;
    protected final Assets assets;
    protected final OrthographicCamera camera = new OrthographicCamera();
    protected final Viewport viewport = new FitViewport(Constants.VIEW_W, Constants.VIEW_H, camera);
    private final Vector3 mouse = new Vector3();
    protected float time;

    protected BaseScreen(NovaFrontierGame game) {
        this.game = game;
        this.batch = game.batch;
        this.assets = game.assets;
        camera.position.set(Constants.VIEW_W / 2f, Constants.VIEW_H / 2f, 0f);
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public final void render(float delta) {
        delta = Math.min(delta, 1f / 30f); // evita saltos apos travadas
        time += delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) OptionsPanel.toggleFullscreen();
        update(delta);

        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        draw(delta);
        batch.end();
    }

    protected abstract void update(float dt);

    protected abstract void draw(float dt);

    /** Posicao do mouse em coordenadas do mundo. */
    protected Vector3 mouse() {
        return viewport.unproject(mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0f));
    }

    protected boolean mouseMoved() {
        return Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0;
    }
}
