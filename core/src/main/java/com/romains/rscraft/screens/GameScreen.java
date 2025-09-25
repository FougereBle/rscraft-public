package com.romains.rscraft.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.romains.rscraft.entities.Player;
import com.romains.rscraft.shaders.TransparentShaderProvider;
import com.romains.rscraft.terrain.Chunk;
import com.romains.rscraft.terrain.DayNightCycle;
import com.romains.rscraft.terrain.Terrain;

/**
 * Écran principal de jeu de RSCraft
 *
 * Cette classe gère l'écran de jeu principal où le joueur peut explorer
 * et interagir avec le monde. Elle coordonne tous les systèmes du jeu :
 *
 * - Rendu 3D du monde et du joueur
 * - Gestion des environnements d'éclairage (jour/nuit)
 * - Interface utilisateur (crosshair, FPS, position, etc.)
 * - Effets visuels (overlay sous l'eau, brouillard)
 * - Contrôles de la souris et initialisation du joueur
 * - Cycle de mise à jour et de rendu du jeu
 *
 * Implémente l'interface Screen de LibGDX pour la gestion des écrans.
 */
public class GameScreen implements Screen {

    private Environment environment;
    private ModelBatch modelBatch;
    private SpriteBatch uiBatch;
    private BitmapFont font;
    private Terrain terrain;
    private Player player;
    private Texture x;
    private Texture wateroverlay;
    private DayNightCycle dayNightCycle;

    @Override
    public void show() {
        initUI();
        init3D();
        initTerrain();
        initPlayer();

        player.setPosition(new Vector3(
            player.getPosition().x,
            terrain.getTerrainHeight(
                MathUtils.floor(player.getPosition().x + 1f / Chunk.SIZE_X),
                MathUtils.floor(player.getPosition().z + 1f / Chunk.SIZE_Z)
            ) + 2.0f,
            player.getPosition().z
        ));

        Gdx.input.setCursorCatched(true);
    }

    private void initUI() {
        uiBatch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("fonts/font-32.fnt"));
        x = new Texture(Gdx.files.internal("gui/x.png"));
        wateroverlay = new Texture(Gdx.files.internal("gui/wateroverlay.png"));
    }

    private void init3D() {
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.75f, 0.75f, 0.75f, 1f));
        environment.set(new ColorAttribute(ColorAttribute.Fog, 144 / 255f, 194 / 255f, 225 / 255f, 1f));

        modelBatch = new ModelBatch(new TransparentShaderProvider());

        dayNightCycle = new DayNightCycle();
    }

    private void initTerrain() {
        terrain = new Terrain();
        terrain.generateStarterChunks(
            MathUtils.floor(Terrain.SIZE_X / 2f),
            MathUtils.floor(Terrain.SIZE_Z / 2f)
        );
    }

    private void initPlayer() {
        player = new Player();
        player.setPosition(new Vector3(
            Terrain.SIZE_X * Chunk.SIZE_X / 2f + Chunk.SIZE_X / 2f - 0.5f,
            Chunk.SIZE_Y,
            Terrain.SIZE_Z * Chunk.SIZE_Z / 2f + Chunk.SIZE_Z / 2f - 0.5f
        ));
    }

    @Override
    public void render(float delta) {
        update(delta);
        draw();
    }

    private void update(float delta) {
        if (delta > 1f / 20f) delta = 1f / 20f;

        dayNightCycle.update(delta);

        float ambiantLight = Math.min(0.75f, Math.max(0.1f, dayNightCycle.getFactor()));

        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, ambiantLight, ambiantLight, ambiantLight, 1f));
        environment.set(new ColorAttribute(ColorAttribute.Fog, 144 / 255f * ambiantLight, 194 / 255f * ambiantLight, 225 / 255f * ambiantLight, 1f));

        processInputs();

        terrain.update(delta);
        player.update(delta);
    }

    private void draw() {
        float skyColor = Math.min(1.0f, Math.max(0.0f, dayNightCycle.getFactor()));

        Gdx.gl20.glClearColor(144 / 255f * skyColor, 194 / 255f * skyColor, 225 / 255f * skyColor, 1.0f);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderEnvironment();
        renderPlayer();
        renderTerrain();
        renderUI();
    }

    public void processInputs() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(false);
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Gdx.input.setCursorCatched(true);
        }
    }

    public void renderEnvironment() {
        dayNightCycle.render(modelBatch, environment);
    }

    public void renderPlayer() {
        player.render(modelBatch, environment);
    }

    public void renderTerrain() {
        terrain.render(modelBatch, environment);
    }

    public void renderUI() {
        uiBatch.begin();

        if (player.isEyesInWater()) {
            float ambiantLight = 0.5f + dayNightCycle.getFactor() * (0.75f - 0.65f);

            uiBatch.setColor(new Color(27f / 255f * ambiantLight, 69f / 255f * ambiantLight, 128f / 255f * ambiantLight, 0.75f));
            uiBatch.draw(wateroverlay, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            uiBatch.setColor(Color.WHITE);
        }

        uiBatch.draw(x, Gdx.graphics.getWidth() / 2.0f - 4f, Gdx.graphics.getHeight() / 2.0f - 4f);
        font.setColor(Color.WHITE);
        font.draw(uiBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - 10);
        font.draw(uiBatch, "Position: " + new Vector3(
            (int) player.getPosition().x,
            (int) player.getPosition().y,
            (int) player.getPosition().z
        ), 10, Gdx.graphics.getHeight() - 30);
        font.draw(uiBatch, "Chunk: " + new Vector2(
            MathUtils.floor(player.getPosition().x / Chunk.SIZE_X),
            MathUtils.floor(player.getPosition().z / Chunk.SIZE_Z)
        ), 10, Gdx.graphics.getHeight() - 50);
        uiBatch.end();
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
