package com.romains.rscraft.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.romains.rscraft.MainGame;

/**
 * Écran de chargement lors de la génération du monde
 *
 * Cet écran s'affiche brièvement au démarrage du jeu pendant
 * que le terrain initial est généré. Il présente :
 * - Image de fond thématique
 * - Message de chargement centré
 * - Transition automatique vers l'écran de jeu
 *
 * Utilise un système de tick simple pour s'assurer que le
 * terrain a le temps d'être initialisé avant de passer au jeu.
 */
public class TerrainGenerationLoadingScreen implements Screen {

    private SpriteBatch batch;
    private BitmapFont font;

    private Texture background;

    private int tick;

    private GlyphLayout layout;

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("fonts/font-64.fnt"));

        background = new Texture(Gdx.files.internal("screens/loading.png"));

        tick = 0;

        layout = new GlyphLayout();
        layout.setText(font, "Generation du monde...");
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);

        batch.begin();
        batch.draw(background, 0, 0);
        font.draw(
            batch,
            "Generation du monde...",
            Gdx.graphics.getWidth() / 2f - layout.width / 2f,
            Gdx.graphics.getHeight() / 2f + layout.height / 2f
        );
        batch.end();

        if (tick < 2) {
            tick++;

            if (tick == 2) {
                MainGame.INSTANCE.setScreen(new GameScreen());
            }
        }
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
