package com.romains.rscraft.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.romains.rscraft.blocks.Block;
import com.romains.rscraft.helpers.EntityBuilderHelper;
import com.romains.rscraft.helpers.TerrainHelper;
import com.romains.rscraft.terrain.Chunk;
import com.romains.rscraft.terrain.Terrain;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe représentant le joueur dans le monde de RSCraft
 *
 * Cette classe gère tous les aspects du joueur :
 * - Physique du mouvement (gravité, collision, saut)
 * - Contrôles (clavier, souris)
 * - Caméra en première personne
 * - Interactions avec le monde (placer/casser des blocs)
 * - Gestion de l'eau et de la nage
 * - Rendu du modèle 3D du bras du joueur
 *
 * Le joueur utilise également le pattern Singleton pour un accès global.
 */
public class Player {

    // === CONSTANTES DE PHYSIQUE ===
    public final static float HEIGHT = 1.8f;              // Taille du joueur en mètres
    public final static float EYE_HEIGHT = 1.6f;          // Hauteur des yeux du joueur
    public final static float SPEED = 5f;                 // Vitesse de déplacement en m/s
    public final static float GRAVITY = -10f;             // Force de gravité
    public final static float WATER_GRAVITY = GRAVITY * 0.1f; // Gravité réduite dans l'eau
    public final static float JUMP_HEIGHT = 5.0f;         // Hauteur de saut normale
    public final static float WATER_JUMP_HEIGHT = 12f;    // Hauteur de saut dans l'eau
    public final static float COYOTE_TIME = 0.3f;         // Temps de grâce pour sauter après avoir quitté le sol

    // Instance singleton du joueur
    public static Player INSTANCE;

    // === GÉOMÉTRIE POUR LE RENDU (actuellement non utilisée) ===
    private final Vector3[] VERTICES = new Vector3[]{
        new Vector3(0, 0, 1), // 0
        new Vector3(0, 1, 1), // 1
        new Vector3(1, 1, 1), // 2
        new Vector3(1, 0, 1), // 3
        new Vector3(0, 0, 0), // 4
        new Vector3(0, 1, 0), // 5
        new Vector3(1, 1, 0), // 6
        new Vector3(1, 0, 0)  // 7
    };

    // Définition des faces d'un cube avec les indices des vertices
    private final Map<String, int[]> FACES = Map.ofEntries(
        Map.entry("top", new int[]{1, 5, 6, 2}),
        Map.entry("bottom", new int[]{0, 4, 7, 3}),
        Map.entry("front", new int[]{0, 1, 2, 3}),
        Map.entry("back", new int[]{7, 6, 5, 4}),
        Map.entry("left", new int[]{4, 5, 1, 0}),
        Map.entry("right", new int[]{3, 2, 6, 7})
    );

    // === COMPOSANTS PRINCIPAUX ===
    private PerspectiveCamera camera;           // Caméra 3D du joueur
    private Vector3 position;                   // Position dans le monde 3D
    private Vector3 velocity;                   // Vélocité pour la physique
    private BoundingBox boundingBox;            // Boîte de collision du joueur

    // === CONTRÔLES DE LA CAMÉRA ===
    private float oldMouseX;                    // Ancienne position X de la souris
    private float oldMouseY;                    // Ancienne position Y de la souris
    private float pitch;                        // Angle de rotation vertical de la caméra

    // === MÉCANIQUES DE JEU ===
    private float waterJumpTimer;               // Timer pour limiter les sauts dans l'eau
    private float coyoteTime;                   // Timer de grâce pour le saut après avoir quitté le sol
    private boolean prevInWater;                // État précédent pour détecter l'entrée dans l'eau

    // === MODÈLE 3D ===
    private HashMap<String, ModelPartData> modelPartsData; // Données des parties du modèle (bras, etc.)

    /**
     * Constructeur du joueur.
     * Initialise la caméra, la position, les variables de contrôle
     * et définit l'instance singleton.
     */
    public Player() {
        // Configuration de la caméra perspective
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.near = 0.1f;  // Distance minimale de rendu
        camera.far = 1000;   // Distance maximale de rendu

        // Initialisation de la position et de la physique
        position = new Vector3(0, 0, 0);
        velocity = new Vector3(0, 0, 0);

        // Initialisation des contrôles souris (-1 = première frame)
        oldMouseX = -1;
        oldMouseY = -1;
        pitch = 0f;

        // Initialisation de la boîte de collision
        boundingBox = new BoundingBox();

        // Définir cette instance comme singleton
        INSTANCE = this;
    }

    /**
     * Construction du modèle 3D du joueur à partir d'un fichier JSON.
     * Charge les données des différentes parties du corps (bras, etc.).
     */
    private void buildModel() {
        modelPartsData = EntityBuilderHelper.buildEntityFromFile("player.json");
    }

    /**
     * Rendu des parties visibles du joueur (principalement le bras).
     * @param modelBatch Batch de rendu pour les modèles 3D
     * @param environment Environnement d'éclairage de la scène
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.begin(camera);
        for (ModelPartData partData : modelPartsData.values()) {
            modelBatch.render(partData.modelInstance, environment);
        }
        modelBatch.end();
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }

    /**
     * Méthode principale de mise à jour du joueur appelée chaque frame.
     * Gère tous les aspects du joueur : physique, contrôles, rendu.
     *
     * @param delta Temps écoulé depuis la dernière frame (en secondes)
     */
    public void update(float delta) {
        // Décrémenter les timers
        waterJumpTimer = Math.max(0, waterJumpTimer - delta);
        coyoteTime = Math.max(0, coyoteTime - delta);

        // Mise à jour de la physique et des contrôles
        handleInWater(delta);     // Gestion de l'état dans l'eau
        handleGravity(delta);     // Application de la gravité
        handleJump(delta);        // Gestion du saut
        handleMovement(delta);    // Calcul du mouvement horizontal
        handleInputs(delta);      // Interaction avec le monde (clic souris)

        // Application du mouvement avec collision
        moveAndSlide(delta);
        // Mise à jour de la caméra
        handleCamera(delta);

        // Construction du modèle si nécessaire (première fois)
        if (modelPartsData == null) {
            buildModel();
            handleArm(0);
        }

        // Mise à jour de la position du bras
        handleArm(delta);
    }

    private void handleArm(float delta) {
        Vector3 rightDirection = camera.direction.cpy().crs(Vector3.Y).nor();
        Vector3 leftDirection = camera.direction.cpy().crs(Vector3.Y).nor().scl(-1);
        Vector3 upDirection = rightDirection.cpy().crs(camera.direction).nor();
        Vector3 downDirection = upDirection.scl(-1);
        Vector3 forwardDirection = camera.direction.cpy().nor();

        for (Map.Entry<String, ModelPartData> entry : modelPartsData.entrySet()) {
            String part = entry.getKey();
            ModelPartData partData = entry.getValue();


            partData.modelInstance.transform.setToTranslation(
                camera.position.cpy()
                    .add(rightDirection.cpy().scl(partData.offset.x * partData.scale))
                    .add(upDirection.cpy().scl(partData.offset.y * partData.scale))
                    .add(forwardDirection.cpy().scl(partData.offset.z * partData.scale))
            );


            if (part.equals("right_arm")) {


                partData.modelInstance.transform.rotateTowardDirection(
                    camera.direction.cpy().nor()
                        .add(leftDirection.scl(0.5f))
                        .add(upDirection.scl(1f)),
                    upDirection
                );
            } else {
                partData.modelInstance.transform.rotate(
                    forwardDirection, partData.rotation.x
                ).rotate(
                    upDirection, partData.rotation.y
                ).rotate(
                    rightDirection, partData.rotation.z
                );
            }
        }
    }

    private void handleInWater(float delta) {
        if (!prevInWater && isInWater()) {
            waterJumpTimer = 0.4f;
        }

        prevInWater = isInWater();
    }

    private void handleGravity(float delta) {
        if (!isOnFloor()) {
            if (isInWater()) {
                velocity.y += WATER_GRAVITY * delta;
                coyoteTime = 0;

                if (velocity.y < WATER_GRAVITY) {
                    velocity.y = WATER_GRAVITY;
                }
            } else {
                velocity.y += GRAVITY * delta;

                if (velocity.y < GRAVITY) {
                    velocity.y = GRAVITY;
                }
            }
        } else {
            coyoteTime = COYOTE_TIME;
        }
    }

    private void handleJump(float delta) {
        if (isInWater()) {
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && waterJumpTimer <= 0) {
                velocity.y += WATER_JUMP_HEIGHT * delta;

                if (velocity.y > JUMP_HEIGHT / 1.5f) {
                    velocity.y = JUMP_HEIGHT / 1.5f;
                }
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && (isOnFloor() || coyoteTime > 0)) {
            velocity.y = JUMP_HEIGHT;
        }
    }

    private void handleMovement(float delta) {
        boolean inputUp = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean inputDown = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean inputLeft = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean inputRight = Gdx.input.isKeyPressed(Input.Keys.D);

        Vector2 inputDir = new Vector2(
            inputLeft ? -1 : inputRight ? 1 : 0,
            inputUp ? 1 : inputDown ? -1 : 0
        );

        Vector3 direction = new Vector3(inputDir.x, 1, inputDir.y);

        velocity.x = direction.x * SPEED;
        velocity.z = direction.z * SPEED;
    }

    /**
     * Gestion des entrées utilisateur pour interagir avec le monde.
     * Clic gauche = casser un bloc, clic droit = placer un bloc.
     *
     * @param delta Temps écoulé depuis la dernière frame
     */
    private void handleInputs(float delta) {
        // Clic gauche : casser un bloc
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            // Lancer un rayon depuis la caméra pour trouver le bloc ciblé
            TerrainHelper.RayPoint rayPoint = TerrainHelper.getRayPoint(
                camera.position,
                camera.direction,
                10f // Portée maximale
            );

            if (rayPoint != null) {
                // Supprimer le bloc à la position trouvée
                Terrain.INSTANCE.removeBlockAt(
                    MathUtils.floor(rayPoint.globalPosition.x),
                    MathUtils.floor(rayPoint.globalPosition.y),
                    MathUtils.floor(rayPoint.globalPosition.z)
                );
            }
        }
        // Clic droit : placer un bloc
        else if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            TerrainHelper.RayPoint rayPoint = TerrainHelper.getRayPoint(
                camera.position,
                camera.direction,
                10f
            );

            if (rayPoint != null) {
                // Calculer la position où placer le bloc (sur la face touchée)
                Vector3 placePosition = rayPoint.globalPosition.cpy().add(rayPoint.normal);

                // Vérifier que le bloc ne sera pas placé dans le joueur
                Vector3 diff = position.cpy().add(new Vector3(0f, HEIGHT / 2f, 0f)).sub(placePosition);
                if (Math.abs(diff.x) <= 0.75f && Math.abs(diff.z) <= 0.75f && Math.abs(diff.y) <= 1.5f)
                    return; // Annuler si trop proche du joueur

                // Placer un bloc de pierre
                Terrain.INSTANCE.setBlockAt(
                    MathUtils.floor(placePosition.x),
                    MathUtils.floor(placePosition.y),
                    MathUtils.floor(placePosition.z),
                    Block.EBlockType.STONE
                );
            }
        }
    }

    private void moveAndSlide(float delta) {
        Vector3 forward = camera.direction.cpy().nor();
        Vector3 right = camera.direction.cpy().crs(Vector3.Y).nor();

        if (!isWaistInWater()) {
            forward.y = 0;
            right.y = 0;
        }

        forward.nor();
        right.nor();

        Vector3 movement = new Vector3();

        movement.add(forward.cpy().scl(velocity.z));
        movement.add(right.cpy().scl(velocity.x));
        movement.nor();

        Vector3 xMovement = new Vector3(movement.x * SPEED * delta, 0, 0);
        Vector3 yMovement = new Vector3(0, velocity.y * delta, 0);
        Vector3 zMovement = new Vector3(0, 0, movement.z * SPEED * delta);

        if (isWaistInWater()) {
            if (Math.abs(movement.y) > 0) {
                yMovement.add(new Vector3(0, (movement.y * SPEED - velocity.y) * delta, 0));
                velocity.y = 0;
            }
        }

        if (!checkCollision(xMovement)) {
            position.add(xMovement);
        }

        if (!checkCollision(zMovement)) {
            position.add(zMovement);
        }

        if (!checkCollision(yMovement)) {
            position.add(yMovement);
        } else {
            velocity.y = 0;
        }

        updateBoundingBox();
    }

    private void handleCamera(float delta) {
        if (oldMouseX != -1 && oldMouseY != -1) {
            float sensitivity = 50f;
            float smoothness = 200f;

            float deltaX = (Gdx.input.getX() - oldMouseX) / smoothness;
            float deltaY = (Gdx.input.getY() - oldMouseY) / smoothness;

            camera.rotate(Vector3.Y, -deltaX * sensitivity);

            Vector3 right = camera.direction.cpy().crs(camera.up).nor();

            float pitchDelta = 0;

            if (pitch - deltaY * sensitivity > -90 && pitch - deltaY * sensitivity < 90) {
                pitch = pitch - deltaY * sensitivity;
                pitchDelta = -deltaY * sensitivity;
            }

            camera.rotate(right, pitchDelta);
            camera.update();
        }

        oldMouseX = Gdx.input.getX();
        oldMouseY = Gdx.input.getY();

        camera.position.set(new Vector3(
            position.x,
            position.y + EYE_HEIGHT,
            position.z
        ));
        camera.update();
    }

    public boolean isOnFloor() {
        return checkCollision(new Vector3(
            0,
            -0.1f,
            0
        ));
    }

    public boolean isInWater() {
        return checkWaterCollision(
            new Vector3(),
            new Vector3(0, HEIGHT / 2f, 0),
            new Vector3(0.5f, HEIGHT / 2f, 0.5f)
        );
    }

    public boolean isEyesInWater() {
        return checkWaterCollision(
            new Vector3(),
            new Vector3(0, EYE_HEIGHT, 0),
            new Vector3(0.5f, 0.1f, 0.5f)
        );
    }

    private boolean checkWaterCollision(Vector3 velocity, Vector3 offset, Vector3 size) {
        boundingBox.set(
            new Vector3(
                position.x + offset.x - size.x / 2f + velocity.x,
                position.y + offset.y - size.y / 2f + velocity.y,
                position.z + offset.z - size.z / 2f + velocity.z
            ),
            new Vector3(
                position.x + offset.x + size.x / 2f + velocity.x,
                position.y + offset.y + size.y / 2f + velocity.y,
                position.z + offset.z + size.z / 2f + velocity.z
            )
        );

        int caseX = MathUtils.floor(position.x / Chunk.SIZE_X);
        int caseZ = MathUtils.floor(position.z / Chunk.SIZE_Z);

        for (int cx = caseX - 1; cx <= caseX + 1; cx++) {
            for (int cz = caseZ - 1; cz <= caseZ + 1; cz++) {
                if (cx < 0 || cx >= Terrain.SIZE_X) continue;
                if (cz < 0 || cz >= Terrain.SIZE_Z) continue;

                Chunk chunk = Terrain.INSTANCE.getChunks()[cx][cz];

                if (chunk == null) continue;

                BoundingBox[][][] boxes = chunk.getWaterBoundingBoxes();

                for (int x = 0; x < boxes.length; x++) {
                    for (int z = 0; z < boxes[x].length; z++) {
                        for (int y = 0; y < boxes[x][z].length; y++) {
                            if (boxes[x][z][y] == null) continue;

                            if (boundingBox.intersects(boxes[x][z][y])) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean isWaistInWater() {
        return checkWaterCollision(
            new Vector3(),
            new Vector3(0, EYE_HEIGHT / 2 + EYE_HEIGHT / 4, 0),
            new Vector3(0.5f, 0.1f, 0.5f)
        );
    }

    private boolean checkCollision(Vector3 velocity) {
        boundingBox.set(
            new Vector3(position.x - 0.25f + velocity.x, position.y + velocity.y, position.z - 0.25f + velocity.z),
            new Vector3(position.x + 0.25f + velocity.x, position.y + HEIGHT + velocity.y, position.z + 0.25f + velocity.z)
        );

        int caseX = MathUtils.floor(position.x / Chunk.SIZE_X);
        int caseZ = MathUtils.floor(position.z / Chunk.SIZE_Z);

        for (int cx = caseX - 1; cx <= caseX + 1; cx++) {
            for (int cz = caseZ - 1; cz <= caseZ + 1; cz++) {
                if (cx < 0 || cx >= Terrain.SIZE_X) continue;
                if (cz < 0 || cz >= Terrain.SIZE_Z) continue;

                Chunk chunk = Terrain.INSTANCE.getChunks()[cx][cz];

                if (chunk == null) continue;

                BoundingBox[][][] boxes = chunk.getBoundingBoxes();

                for (int x = 0; x < boxes.length; x++) {
                    for (int z = 0; z < boxes[x].length; z++) {
                        for (int y = 0; y < boxes[x][z].length; y++) {
                            if (boxes[x][z][y] == null) continue;

                            if (boundingBox.intersects(boxes[x][z][y])) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private void updateBoundingBox() {
        boundingBox.set(
            new Vector3(position.x - 0.25f, position.y, position.z - 0.25f),
            new Vector3(position.x + 0.25f, position.y + HEIGHT, position.z + 0.25f)
        );
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 position) {
        this.position = position;
    }
}
