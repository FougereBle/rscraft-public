package com.romains.rscraft.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.romains.rscraft.entities.Player;
import com.romains.rscraft.shaders.SkyboxShaderProvider;

/**
 * Système de cycle jour/nuit dynamique pour RSCraft
 *
 * Cette classe gère l'ambiance visuelle du jeu en simulant le passage du temps :
 *
 * === CYCLE TEMPOREL ===
 * - Journée de 24 heures virtuelles (240 secondes réelles)
 * - Transitions fluides entre jour, crépuscule et nuit
 * - Facteur d'éclairage variable selon l'heure
 *
 * === ÉLÉMENTS CÉLESTES ===
 * - Soleil et lune qui tournent autour du joueur
 * - Positions calculées selon l'heure du jour
 * - Correction des rotations pour éviter les bugs visuels
 *
 * === RENDU DU CIEL ===
 * - Skybox avec dégradé de couleurs dynamique
 * - Couleurs qui changent selon l'heure (bleu jour, orange crépuscule, noir nuit)
 * - Shader personnalisé pour le rendu atmosphérique
 *
 * === IMPACT SUR L'ÉCLAIRAGE ===
 * - Modification de l'éclairage ambiant global
 * - Influence sur la couleur du brouillard
 * - Création d'une ambiance immersive jour/nuit
 */
public class DayNightCycle {

    // === TAILLES DES ÉLÉMENTS CÉLESTES ===
    private final static float SUN_SIZE = 96;         // Taille du soleil en pixels
    private final static float MOON_SIZE = 64;        // Taille de la lune en pixels

    // === HORAIRES DU CYCLE JOUR/NUIT ===
    private final static float MORNING = 4f;          // Début de l'aube (4h)
    private final static float NOON_START = 5f;       // Début du jour plein (5h)
    private final static float NOON_END = 19f;        // Fin du jour plein (19h)
    private final static float EVENING = 20f;         // Début de la nuit (20h)
    private final static float HOUR_TO_SECONDS = 10;  // Une heure de jeu = 10 secondes réelles

    private final Color dayTopColor = new Color(33 / 255f, 158 / 255f, 188 / 255f, 255 / 255f);      // Bleu clair
    private final Color dayBottomColor = new Color(142 / 255f, 202 / 255f, 230 / 255f, 255 / 255f);   // Bleu très clair
    private final Color sunsetTopColor = new Color(33 / 255f, 158 / 255f, 188 / 255f, 255 / 255f);   // Orange foncé
    private final Color sunsetBottomColor = new Color(251 / 255f, 133 / 255f, 0 / 255f, 255 / 255f); // Orange clair
    private final Color nightTopColor = new Color(13 / 255f, 27 / 255f, 42 / 255f, 255 / 255f);    // Bleu très foncé
    private final Color nightBottomColor = new Color(27 / 255f, 38 / 255f, 59 / 255f, 255 / 255f); // Bleu foncé

    private float time;
    private float factor;

    private ModelInstance sunInstance;
    private ModelInstance moonInstance;

    private Quaternion prevSunQuaternion = new Quaternion();
    private Quaternion prevMoonQuaternion = new Quaternion();

    private ModelBatch skyboxBatch;
    private SkyboxShaderProvider skyboxShader;
    private Color skyTopColor;
    private Color skyBottomColor;

    public DayNightCycle() {
        time = MORNING + 2;

        createSun();
        createMoon();
        createSkyBox();
    }

    private void createSun() {
        ModelBuilder sunBuilder = new ModelBuilder();
        Model sunModel = sunBuilder.createRect(
            -SUN_SIZE / 2.0f, -SUN_SIZE / 2.0f, 0,
            SUN_SIZE / 2.0f, -SUN_SIZE / 2.0f, 0,
            SUN_SIZE / 2.0f, SUN_SIZE / 2.0f, 0,
            -SUN_SIZE / 2.0f, SUN_SIZE / 2.0f, 0,
            0, 0, -1,
            new Material(
                TextureAttribute.createDiffuse(new Texture(Gdx.files.internal("sky/sun.png")))
            ),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates
        );

        sunInstance = new ModelInstance(sunModel);
    }

    private void createMoon() {
        ModelBuilder moonBuilder = new ModelBuilder();
        Model moonModel = moonBuilder.createRect(
            -MOON_SIZE / 2.0f, -MOON_SIZE / 2.0f, 0,
            MOON_SIZE / 2.0f, -MOON_SIZE / 2.0f, 0,
            MOON_SIZE / 2.0f, MOON_SIZE / 2.0f, 0,
            -MOON_SIZE / 2.0f, MOON_SIZE / 2.0f, 0,
            0, 0, -1,
            new Material(
                TextureAttribute.createDiffuse(new Texture(Gdx.files.internal("sky/moon.png")))
            ),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates
        );

        moonInstance = new ModelInstance(moonModel);
    }

    private void createSkyBox() {
        skyboxShader = new SkyboxShaderProvider(1000);
        skyboxBatch = new ModelBatch(skyboxShader);
    }

    public void update(float delta) {
        if (Player.INSTANCE == null) return;

        time = (time + delta / HOUR_TO_SECONDS >= 24) ? 0 : time + delta / HOUR_TO_SECONDS;

        handleFactor(delta);
        handleSunRotation(delta);
        handleMoonRotation(delta);
        handleSkyColor(delta);
    }

    private void handleFactor(float delta) {
        float minValue = 0.0f;
        float maxValue = 1.0f;

        if (time <= MORNING) {
            factor = minValue;
        } else if (time <= NOON_START) {
            factor = (maxValue - minValue) * (time - MORNING) / (NOON_START - MORNING) + minValue;
        } else if (time <= NOON_END) {
            factor = maxValue;
        } else if (time <= EVENING) {
            factor = (maxValue - minValue) * (EVENING - time) / (EVENING - NOON_END) + minValue;
        } else {
            factor = minValue;
        }
    }

    private void handleSunRotation(float delta) {
        Vector3 sunPosition = calculateSunPosition();
        sunInstance.transform.setToTranslation(sunPosition);
        sunInstance.transform.rotateTowardTarget(Player.INSTANCE.getPosition(), Vector3.Y);

        // Permet de corriger un bug de rotation de la texture.
        // Me demandez pas comment ça marche, j'en sais rien.
        fixSunRotation(sunPosition, sunInstance);
    }

    private void handleMoonRotation(float delta) {
        Vector3 moonPosition = calculateMoonPosition();
        moonInstance.transform.setToTranslation(moonPosition);
        moonInstance.transform.rotateTowardTarget(Player.INSTANCE.getPosition(), Vector3.Y);

        // Permet de corriger un bug de rotation de la texture.
        // Me demandez pas comment ça marche, j'en sais rien.
        fixMoonRotation(moonPosition, moonInstance);
    }

    private void handleSkyColor(float delta) {
        skyboxShader.setPosition(Player.INSTANCE.getPosition().cpy().scl(1, 0, 1));

        float sunsetNightStartIn = EVENING - 2;
        float sunsetNightEndIn = EVENING - 1;

        float sunsetNightStartOut = EVENING - 1;
        float sunsetNightEndOut = EVENING;

        float sunsetDayStartIn = MORNING - 1;
        float sunsetDayEndIn = MORNING + 1;

        float sunsetDayStartOut = MORNING + 1;
        float sunsetDayEndOut = MORNING + 3;

        if (time >= sunsetNightStartIn && time <= sunsetNightEndIn) {
            skyBottomColor = dayBottomColor.cpy().lerp(sunsetBottomColor, (time - sunsetNightStartIn) / (sunsetNightEndIn - sunsetNightStartIn));
            skyTopColor = dayTopColor.cpy().lerp(sunsetTopColor, (time - sunsetNightStartIn) / (sunsetNightEndIn - sunsetNightStartIn));
        } else if (time >= sunsetNightStartOut && time <= sunsetNightEndOut) {
            skyBottomColor = sunsetBottomColor.cpy().lerp(nightBottomColor, (time - sunsetNightStartOut) / (sunsetNightEndOut - sunsetNightStartOut));
            skyTopColor = sunsetTopColor.cpy().lerp(nightTopColor, (time - sunsetNightStartOut) / (sunsetNightEndOut - sunsetNightStartOut));
        } else if (time >= sunsetDayStartIn && time <= sunsetDayEndIn) {
            skyBottomColor = nightBottomColor.cpy().lerp(sunsetBottomColor, (time - sunsetDayStartIn) / (sunsetDayEndIn - sunsetDayStartIn));
            skyTopColor = nightTopColor.cpy().lerp(sunsetTopColor, (time - sunsetDayStartIn) / (sunsetDayEndIn - sunsetDayStartIn));
        } else if (time >= sunsetDayStartOut && time <= sunsetDayEndOut) {
            skyBottomColor = sunsetBottomColor.cpy().lerp(dayBottomColor, (time - sunsetDayStartOut) / (sunsetDayEndOut - sunsetDayStartOut));
            skyTopColor = sunsetTopColor.cpy().lerp(dayTopColor, (time - sunsetDayStartOut) / (sunsetDayEndOut - sunsetDayStartOut));
        } else if (time >= sunsetNightEndOut) {
            skyBottomColor = nightBottomColor;
            skyTopColor = nightTopColor;
        } else if (time <= sunsetDayStartIn) {
            skyBottomColor = nightBottomColor;
            skyTopColor = nightTopColor;
        } else {
            skyBottomColor = dayBottomColor;
            skyTopColor = dayTopColor;
        }

        skyboxShader.setTopColor(skyTopColor);
        skyboxShader.setBottomColor(skyBottomColor);
    }

    private Vector3 calculateSunPosition() {
        Vector3 playerPos = Player.INSTANCE.getPosition().cpy().scl(1, 0, 1);

        float sunRadius = 450;

        float sunAngle = calculateSunAngle();

        float sunX = playerPos.x + sunRadius * (float) Math.cos(sunAngle);
        float sunY = playerPos.y + sunRadius * (float) Math.sin(sunAngle);
        float sunZ = playerPos.z;

        return new Vector3(sunX, sunY, sunZ);
    }

    private void fixSunRotation(Vector3 sunPosition, ModelInstance sunInstance) {
        if (time > NOON_END || time < NOON_START) {
            return;
        }

        Quaternion q = new Quaternion();

        sunInstance.transform.getRotation(q);

        Quaternion rel = new Quaternion(q);
        rel.mulLeft(new Quaternion(prevSunQuaternion).conjugate());

        float angleRad = (float) (2.0 * Math.acos(Math.abs(rel.w)));
        float angleDeg = angleRad * MathUtils.radiansToDegrees;

        if (Math.abs(angleDeg - 180f) < 1f) {
            q.mul(new Quaternion(Vector3.Z, 180f));
        } else {
            prevSunQuaternion = q.cpy();
        }

        sunInstance.transform.set(sunPosition, q);
    }

    private Vector3 calculateMoonPosition() {
        Vector3 playerPos = Player.INSTANCE.getPosition().cpy().scl(1, 0, 1);

        float moonRadius = 450;

        float sunAngle = calculateSunAngle();
        float moonAngle = sunAngle + (float) Math.PI;

        float moonX = playerPos.x + moonRadius * (float) Math.cos(moonAngle);
        float moonY = playerPos.y + moonRadius * (float) Math.sin(moonAngle);
        float moonZ = playerPos.z;

        return new Vector3(moonX, moonY, moonZ);
    }

    private void fixMoonRotation(Vector3 moonPosition, ModelInstance moonInstance) {
        if (time < NOON_END && time > NOON_START) {
            return;
        }

        Quaternion q = new Quaternion();

        moonInstance.transform.getRotation(q);

        Quaternion rel = new Quaternion(q);
        rel.mulLeft(new Quaternion(prevMoonQuaternion).conjugate());

        float angleRad = (float) (2.0 * Math.acos(Math.abs(rel.w)));
        float angleDeg = angleRad * MathUtils.radiansToDegrees;

        if (Math.abs(angleDeg - 180f) < 1f) {
            q.mul(new Quaternion(Vector3.Z, 180f));
        } else {
            prevMoonQuaternion = q.cpy();
        }

        moonInstance.transform.set(moonPosition, q);
    }

    private float calculateSunAngle() {
        float dayDuration = EVENING - MORNING;
        float nightDuration = 24 - dayDuration;

        if (time >= MORNING && time <= EVENING) {
            float dayProgress = (time - MORNING) / dayDuration;
            return (float) (Math.PI - dayProgress * Math.PI);
        } else {
            float nightTime = (time > EVENING) ? (time - EVENING) : (time + 24 - EVENING);
            float nightProgress = nightTime / nightDuration;
            return (float) (2 * Math.PI - nightProgress * Math.PI);
        }
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        Matrix4 transform = new Matrix4();
        transform.setTranslation(Player.INSTANCE.getPosition());

        skyboxBatch.begin(Player.INSTANCE.getCamera());
        skyboxShader.render(skyboxBatch);
        skyboxBatch.end();

        modelBatch.begin(Player.INSTANCE.getCamera());
        modelBatch.render(sunInstance);
        modelBatch.render(moonInstance);
        modelBatch.end();
    }

    public float getTime() {
        return time;
    }

    public float getFactor() {
        return factor;
    }
}
