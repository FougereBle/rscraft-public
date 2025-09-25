package com.romains.rscraft.terrain;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import com.romains.rscraft.blocks.Block;
import com.romains.rscraft.blocks.BlockManager;
import com.sudoplay.joise.module.ModuleBasisFunction;

import java.util.Map;

/**
 * Segment de terrain de taille fixe dans le monde de RSCraft
 *
 * Cette classe est l'unité de base de la génération et du rendu du monde.
 * Un chunk contient une portion 3D de 8x8x48 blocs et gère :
 *
 * === GÉNÉRATION DE TERRAIN ===
 * - Génération procédurale via bruit de Perlin
 * - Création de reliefs, falaises et biomes d'eau
 * - Placement automatique d'arbres et de végétation
 * - Bordures du monde avec barrières de bois
 *
 * === OPTIMISATION DU RENDU ===
 * - Séparation des blocs opaques, transparents et alpha
 * - Culling des faces cachées pour réduire les polygones
 * - Ambient Occlusion pour l'ombrage réaliste
 * - Construction de modèles 3D optimisés
 *
 * === GESTION DES COLLISIONS ===
 * - BoundingBox pour chaque bloc solide
 * - Séparation collision terrain/eau pour la physique
 * - Support des modifications dynamiques de blocs
 *
 * === CYCLE DE VIE ===
 * 1. Construction -> Génération -> Build -> Rendu -> Dispose
 * 2. Les chunks sont créés/détruits selon la position du joueur
 */
public class Chunk implements Disposable {

    // === PARAMÈTRES DE GÉNÉRATION ===
    public final static int SEED = 1;                 // Graine pour la génération procédurale
    public final static int WATER_LEVEL = 9;          // Niveau de la mer (blocs en-dessous = eau)

    // === DIMENSIONS DU CHUNK ===
    public final static int SIZE_X = 8;               // Largeur en blocs
    public final static int SIZE_Y = 48;              // Hauteur en blocs
    public final static int SIZE_Z = 8;               // Profondeur en blocs

    private final Vector3[] VERTICES = new Vector3[]{
        new Vector3(0, 0, 1), // 0
        new Vector3(0, 1, 1), // 1
        new Vector3(1, 1, 1), // 2
        new Vector3(1, 0, 1), // 3

        new Vector3(0, 0, 0), // 4
        new Vector3(0, 1, 0), // 5

        new Vector3(1, 1, 0), // 6
        new Vector3(1, 0, 0) // 7
    };

    private final Map<String, int[]> FACES = Map.ofEntries(
        Map.entry("top", new int[]{1, 5, 6, 2}),
        Map.entry("bottom", new int[]{0, 4, 7, 3}),
        Map.entry("front", new int[]{0, 1, 2, 3}),
        Map.entry("back", new int[]{7, 6, 5, 4}),
        Map.entry("left", new int[]{4, 5, 1, 0}),
        Map.entry("right", new int[]{3, 2, 6, 7})
    );

    private Vector3 globalPosition;

    private Block[][][] blocks = new Block[SIZE_X][SIZE_Z][SIZE_Y];

    private ModelInstance modelInstance;
    private ModelInstance transparentModelInstance;
    private ModelInstance alphaModelInstance;

    private BoundingBox[][][] boundingBoxes;
    private BoundingBox[][][] waterBoundingBoxes;

    private Model opaqueModel;
    private Model transparentModel;
    private Model alphaModel;

    public Chunk(Vector3 globalPosition) {
        this.globalPosition = new Vector3(
            globalPosition.x,
            globalPosition.y,
            globalPosition.z
        );
        this.boundingBoxes = new BoundingBox[SIZE_X][SIZE_Z][SIZE_Y];
        this.waterBoundingBoxes = new BoundingBox[SIZE_X][SIZE_Z][SIZE_Y];
    }

    public void generate() {
        generateTerrain();
        generateCliff();
        generateTrees();
    }

    private void generateTerrain() {
        ModuleBasisFunction basis = new ModuleBasisFunction();
        basis.setType(ModuleBasisFunction.BasisType.GRADIENT);
        basis.setSeed(SEED);

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int y = 0; y < SIZE_Y; y++) {
                    if (blocks[x][z][y] == null) {
                        Block block;

                        float noiseScale = 1.0f / 32.0f;
                        float heightFactor = 8f;

                        float value = (float) basis.get(
                            (globalPosition.x + x) * noiseScale,
                            (globalPosition.z + z) * noiseScale
                        );

                        int height = 4 + MathUtils.floor(value * heightFactor);

                        Block.EBlockType blockType = Block.EBlockType.AIR;

                        if (y == height) {
                            blockType = Block.EBlockType.GRASS;
                        } else if (y < height) {
                            blockType = Block.EBlockType.STONE;
                        } else if (y < WATER_LEVEL) {
                            blockType = Block.EBlockType.WATER;
                        }

                        if (globalPosition.x + x == 0) {
                            blockType = Block.EBlockType.WOOD;
                        }
                        if (globalPosition.x + x == Terrain.SIZE_X * Chunk.SIZE_X - 1) {
                            blockType = Block.EBlockType.WOOD;
                        }
                        if (globalPosition.z + z == 0) {
                            blockType = Block.EBlockType.WOOD;
                        }
                        if (globalPosition.z + z == Terrain.SIZE_Z * Chunk.SIZE_Z - 1) {
                            blockType = Block.EBlockType.WOOD;
                        }

                        block = new Block(blockType, new Vector3(
                            globalPosition.x + x,
                            globalPosition.y + y,
                            globalPosition.z + z
                        ));

                        blocks[x][z][y] = block;
                    }
                }
            }
        }
    }

    private void generateCliff() {
        ModuleBasisFunction basis = new ModuleBasisFunction();
        basis.setType(ModuleBasisFunction.BasisType.GRADIENT);
        basis.setSeed(SEED + 1);

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int y = 0; y < SIZE_Y; y++) {
                    float noiseScale = 1.0f / 64.0f;
                    float heightFactor = 32f;

                    if (blocks[x][z][y].getType() == Block.EBlockType.AIR || blocks[x][z][y].getType() == Block.EBlockType.WATER) {
                        Block block;


                        float value = (float) basis.get(
                            (globalPosition.x + x) * noiseScale,
                            (globalPosition.z + z) * noiseScale
                        );

                        int height = 10 + MathUtils.floor(value * heightFactor);

                        Block.EBlockType blockType = Block.EBlockType.AIR;

                        if (y == height) {
                            blockType = Block.EBlockType.GRASS;
                        } else if (y < height) {
                            blockType = Block.EBlockType.STONE;
                        } else if (y < 9) {
                            blockType = Block.EBlockType.WATER;
                        }

                        if (blockType == Block.EBlockType.AIR) {
                            continue;
                        }

                        block = new Block(blockType, new Vector3(
                            globalPosition.x + x,
                            globalPosition.y + y,
                            globalPosition.z + z
                        ));

                        blocks[x][z][y] = block;
                    }
                }
            }
        }
    }

    private void generateTrees() {
        ModuleBasisFunction basis = new ModuleBasisFunction();
        basis.setType(ModuleBasisFunction.BasisType.SIMPLEX);
        basis.setSeed(SEED);

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                float noiseScale = 1.0f / 4.0f;
                float value = (float) basis.get(
                    (globalPosition.x + x) * noiseScale,
                    (globalPosition.z + z) * noiseScale
                );
                float height = value * 6;

                if (height >= 5.8f) {
                    int y = getTerrainHeight(x, z) + 1;
                    generateTree(x, y, z);
                }
            }
        }
    }

    public int getTerrainHeight(int localX, int localZ) {
        for (int y = 0; y < SIZE_Y; y++) {
            if (
                blocks[localX][localZ][y].getType() == Block.EBlockType.AIR ||
                    blocks[localX][localZ][y].getType() == Block.EBlockType.WATER
            ) {
                return y - 1;
            }
        }
        return SIZE_Y;
    }

    private void generateTree(int x, int y, int z) {
        for (int h = 0; h < 6; h++) {
            if (x < 0 || x >= SIZE_X) continue;
            if (y + h < 0 || y + h >= SIZE_Y) continue;
            if (z < 0 || z >= SIZE_Z) continue;

            blocks[x][z][y + h] = new Block(Block.EBlockType.WOOD, new Vector3(
                globalPosition.x + x,
                globalPosition.y + y + h,
                globalPosition.z + z
            ));
        }

        for (int i = -2; i < 3; i++) {
            for (int j = -2; j < 3; j++) {
                for (int h = 3; h < 5; h++) {
                    int globalX = (int) globalPosition.x + x + i;
                    int globalY = (int) globalPosition.y + y + h;
                    int globalZ = (int) globalPosition.z + z + j;

                    Block block = Terrain.INSTANCE.getBlockAt(globalX, globalY, globalZ);

                    if (block == null || block.isTransparent()) {
                        Terrain.INSTANCE.setBlockAt(globalX, globalY, globalZ, Block.EBlockType.LEAF, false);
                    }
                }
            }
        }

        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                for (int h = 5; h < 7; h++) {
                    int globalX = (int) globalPosition.x + x + i;
                    int globalY = (int) globalPosition.y + y + h;
                    int globalZ = (int) globalPosition.z + z + j;

                    Block block = Terrain.INSTANCE.getBlockAt(globalX, globalY, globalZ);

                    if (block == null || block.isTransparent()) {
                        Terrain.INSTANCE.setBlockAt(globalX, globalY, globalZ, Block.EBlockType.LEAF, false);
                    }
                }
            }
        }
    }

    public void build() {
        boundingBoxes = new BoundingBox[SIZE_X][SIZE_Z][SIZE_Y];
        waterBoundingBoxes = new BoundingBox[SIZE_X][SIZE_Z][SIZE_Y];

        buildOpaque();
        buildTransparent();
        buildAlpha();
    }

    private void buildOpaque() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        MeshPartBuilder meshPartBuilder = modelBuilder.part(
            "opaque",
            GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked,
            new Material(
                ColorAttribute.createDiffuse(Color.WHITE),
                TextureAttribute.createDiffuse(BlockManager.getInstance().getTexture())
            )
        );

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int y = 0; y < SIZE_Y; y++) {
                    if (!blocks[x][z][y].isTransparent()) {
                        buildBlock(meshPartBuilder, blocks[x][z][y]);

                        boundingBoxes[x][z][y] = new BoundingBox(
                            new Vector3(
                                globalPosition.x + x,
                                globalPosition.y + y,
                                globalPosition.z + z
                            ),
                            new Vector3(
                                globalPosition.x + x + 1,
                                globalPosition.y + y + 1,
                                globalPosition.z + z + 1
                            )
                        );
                    }
                }
            }
        }

        opaqueModel = modelBuilder.end();

        modelInstance = new ModelInstance(opaqueModel);
        modelInstance.transform.setToTranslation(new Vector3(
            globalPosition.x,
            globalPosition.y,
            globalPosition.z
        ));
    }

    private void buildTransparent() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        MeshPartBuilder meshPartBuilder = modelBuilder.part(
            "transparent",
            GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked,
            new Material(
                ColorAttribute.createDiffuse(Color.WHITE),
                TextureAttribute.createDiffuse(BlockManager.getInstance().getTexture()),
                new BlendingAttribute()
            )
        );

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int y = 0; y < SIZE_Y; y++) {
                    if (blocks[x][z][y].isTransparent() && !blocks[x][z][y].isAlphaZero()) {
                        buildBlock(meshPartBuilder, blocks[x][z][y]);

                        if (blocks[x][z][y].getType() == Block.EBlockType.WATER) {
                            waterBoundingBoxes[x][z][y] = new BoundingBox(
                                new Vector3(
                                    globalPosition.x + x,
                                    globalPosition.y + y,
                                    globalPosition.z + z
                                ),
                                new Vector3(
                                    globalPosition.x + x + 1,
                                    globalPosition.y + y + 1,
                                    globalPosition.z + z + 1
                                )
                            );
                        }
                    }
                }
            }
        }

        transparentModel = modelBuilder.end();

        transparentModelInstance = new ModelInstance(transparentModel);
        transparentModelInstance.transform.setToTranslation(new Vector3(
            globalPosition.x,
            globalPosition.y,
            globalPosition.z
        ));
    }

    private void buildAlpha() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        MeshPartBuilder meshPartBuilder = modelBuilder.part(
            "transparent",
            GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked,
            new Material(
                ColorAttribute.createDiffuse(Color.WHITE),
                TextureAttribute.createDiffuse(BlockManager.getInstance().getTexture())
            )
        );

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int y = 0; y < SIZE_Y; y++) {
                    if (blocks[x][z][y].isAlphaZero()) {
                        buildBlock(meshPartBuilder, blocks[x][z][y]);

                        boundingBoxes[x][z][y] = new BoundingBox(
                            new Vector3(
                                globalPosition.x + x,
                                globalPosition.y + y,
                                globalPosition.z + z
                            ),
                            new Vector3(
                                globalPosition.x + x + 1,
                                globalPosition.y + y + 1,
                                globalPosition.z + z + 1
                            )
                        );
                    }
                }
            }
        }

        alphaModel = modelBuilder.end();

        alphaModelInstance = new ModelInstance(alphaModel);
        alphaModelInstance.transform.setToTranslation(new Vector3(
            globalPosition.x,
            globalPosition.y,
            globalPosition.z
        ));
    }

    private void buildBlock(MeshPartBuilder meshPartBuilder, Block block) {
        if (mustBuildFace(block, Vector3.Y)) {
            buildBlockFace(meshPartBuilder, block, "top");
        }
        if (mustBuildFace(block, Vector3.Y.cpy().scl(-1))) {
            buildBlockFace(meshPartBuilder, block, "bottom");
        }
        if (mustBuildFace(block, Vector3.Z)) {
            buildBlockFace(meshPartBuilder, block, "front");
        }
        if (mustBuildFace(block, Vector3.Z.cpy().scl(-1))) {
            buildBlockFace(meshPartBuilder, block, "back");
        }
        if (mustBuildFace(block, Vector3.X.cpy().scl(-1))) {
            buildBlockFace(meshPartBuilder, block, "left");
        }
        if (mustBuildFace(block, Vector3.X)) {
            buildBlockFace(meshPartBuilder, block, "right");
        }
    }

    private boolean mustBuildFace(Block block, Vector3 direction) {
        if (block.getType() == Block.EBlockType.AIR) {
            return false;
        }

        int x = (int) (block.getLocalPosition().x + direction.x);
        int y = (int) (block.getLocalPosition().y + direction.y);
        int z = (int) (block.getLocalPosition().z + direction.z);


        int globalX = (int) globalPosition.x + x;
        int globalY = (int) globalPosition.y + y;
        int globalZ = (int) globalPosition.z + z;

        Block b = Terrain.INSTANCE.getBlockAt(globalX, globalY, globalZ);

        if (block.getType() == Block.EBlockType.WATER) {
            if (b == null || b.getType() == Block.EBlockType.WATER || b.isAlphaZero()) {
                return false;
            }
        }

        if (b == null) return true;

        return b.isTransparent();
    }

    private void buildBlockFace(MeshPartBuilder meshPartBuilder, Block block, String face) {
        float ao1 = 1.0f;
        float ao2 = 1.0f;
        float ao3 = 1.0f;
        float ao4 = 1.0f;

        if (face.equals("top")) {
            ao1 = getAO(block, new Vector3[]{
                new Vector3(-1, 1, 0),
                new Vector3(0, 1, 1),
                new Vector3(-1, 1, 1),
            });
            ao2 = getAO(block, new Vector3[]{
                new Vector3(0, 1, -1),
                new Vector3(-1, 1, 0),
                new Vector3(-1, 1, -1),
            });
            ao3 = getAO(block, new Vector3[]{
                new Vector3(0, 1, -1),
                new Vector3(1, 1, 0),
                new Vector3(1, 1, -1),
            });
            ao4 = getAO(block, new Vector3[]{
                new Vector3(1, 1, 0),
                new Vector3(0, 1, 1),
                new Vector3(1, 1, 1),
            });
        } else if (face.equals("back")) {
            ao1 = getAO(block, new Vector3[]{
                new Vector3(0, -1, -1),
                new Vector3(1, 0, -1),
                new Vector3(1, -1, -1),
            });
            ao2 = getAO(block, new Vector3[]{
                new Vector3(1, 0, -1),
                new Vector3(0, 1, -1),
                new Vector3(1, 1, -1),
            });
            ao3 = getAO(block, new Vector3[]{
                new Vector3(-1, 0, -1),
                new Vector3(0, 1, -1),
                new Vector3(-1, 1, -1),
            });
            ao4 = getAO(block, new Vector3[]{
                new Vector3(0, -1, -1),
                new Vector3(-1, 0, -1),
                new Vector3(-1, -1, -1),
            });
        } else if (face.equals("front")) {
            ao1 = getAO(block, new Vector3[]{
                new Vector3(-1, 0, 1),
                new Vector3(0, -1, 1),
                new Vector3(-1, -1, 1),
            });
            ao2 = getAO(block, new Vector3[]{
                new Vector3(-1, 1, 1),
                new Vector3(0, 1, 1),
                new Vector3(-1, 0, 1),
            });
            ao3 = getAO(block, new Vector3[]{
                new Vector3(1, 1, 1),
                new Vector3(0, 1, 1),
                new Vector3(1, 0, 1),
            });
            ao4 = getAO(block, new Vector3[]{
                new Vector3(1, 0, 1),
                new Vector3(0, -1, 1),
                new Vector3(1, -1, 1),
            });
        } else if (face.equals("right")) {
            ao1 = getAO(block, new Vector3[]{
                new Vector3(1, -1, 0),
                new Vector3(1, 0, 1),
                new Vector3(1, -1, 1),
            });
            ao2 = getAO(block, new Vector3[]{
                new Vector3(1, 0, 1),
                new Vector3(1, 1, 0),
                new Vector3(1, 1, 1),
            });
            ao3 = getAO(block, new Vector3[]{
                new Vector3(1, 0, -1),
                new Vector3(1, 1, 0),
                new Vector3(1, 1, -1),
            });
            ao4 = getAO(block, new Vector3[]{
                new Vector3(1, -1, 0),
                new Vector3(1, 0, -1),
                new Vector3(1, -1, -1),
            });
        } else if (face.equals("left")) {
            ao1 = getAO(block, new Vector3[]{
                new Vector3(-1, -1, 0),
                new Vector3(-1, 0, -1),
                new Vector3(-1, -1, -1),
            });
            ao2 = getAO(block, new Vector3[]{
                new Vector3(-1, 0, -1),
                new Vector3(-1, 1, 0),
                new Vector3(-1, 1, -1),
            });
            ao3 = getAO(block, new Vector3[]{
                new Vector3(-1, 0, 1),
                new Vector3(-1, 1, 0),
                new Vector3(-1, 1, 1),
            });
            ao4 = getAO(block, new Vector3[]{
                new Vector3(-1, -1, 0),
                new Vector3(-1, 0, 1),
                new Vector3(-1, -1, 1),
            });
        }

        float offset = block.getType() == Block.EBlockType.LEAF ? 0.001f : 0.0f;
        float offsetX = -0.0001f, offsetY = -0.0001f, offsetZ = -0.0001f;

        if (face.equals("top")) offsetY += offset;
        else if (face.equals("bottom")) offsetY += -offset;
        else if (face.equals("front")) offsetZ += offset;
        else if (face.equals("back")) offsetZ += -offset;
        else if (face.equals("left")) offsetX += -offset;
        else if (face.equals("right")) offsetX += offset;

        MeshPartBuilder.VertexInfo v1 = new MeshPartBuilder.VertexInfo().setPos(
            MathUtils.floor(block.getLocalPosition().x + VERTICES[FACES.get(face)[0]].x) + offsetX,
            MathUtils.floor(block.getLocalPosition().y + VERTICES[FACES.get(face)[0]].y) + offsetY,
            MathUtils.floor(block.getLocalPosition().z + VERTICES[FACES.get(face)[0]].z) + offsetZ
        ).setCol(new Color(new Color(ao1, ao1, ao1, 1.0f)));
        MeshPartBuilder.VertexInfo v2 = new MeshPartBuilder.VertexInfo().setPos(
            MathUtils.floor(block.getLocalPosition().x + VERTICES[FACES.get(face)[1]].x) + offsetX,
            MathUtils.floor(block.getLocalPosition().y + VERTICES[FACES.get(face)[1]].y) + offsetY,
            MathUtils.floor(block.getLocalPosition().z + VERTICES[FACES.get(face)[1]].z) + offsetZ
        ).setCol(new Color(new Color(ao2, ao2, ao2, 1.0f)));
        MeshPartBuilder.VertexInfo v3 = new MeshPartBuilder.VertexInfo().setPos(
            MathUtils.floor(block.getLocalPosition().x + VERTICES[FACES.get(face)[2]].x) + offsetX,
            MathUtils.floor(block.getLocalPosition().y + VERTICES[FACES.get(face)[2]].y) + offsetY,
            MathUtils.floor(block.getLocalPosition().z + VERTICES[FACES.get(face)[2]].z) + offsetZ
        ).setCol(new Color(new Color(ao3, ao3, ao3, 1.0f)));
        MeshPartBuilder.VertexInfo v4 = new MeshPartBuilder.VertexInfo().setPos(
            MathUtils.floor(block.getLocalPosition().x + VERTICES[FACES.get(face)[3]].x) + offsetX,
            MathUtils.floor(block.getLocalPosition().y + VERTICES[FACES.get(face)[3]].y) + offsetY,
            MathUtils.floor(block.getLocalPosition().z + VERTICES[FACES.get(face)[3]].z) + offsetZ
        ).setCol(new Color(new Color(ao4, ao4, ao4, 1.0f)));

        int textureWidth = BlockManager.getInstance().getTexture().getWidth();
        int textureHeight = BlockManager.getInstance().getTexture().getHeight();

        float nbCols = (float) textureWidth / (float) BlockManager.TEXTURE_SIZE;
        float nbRows = (float) textureHeight / (float) BlockManager.TEXTURE_SIZE;

        float uvWidth = 1.0f / nbCols;
        float uvHeight = 1.0f / nbRows;

        Vector2 texturePosition = block.getTexturePosition();

        if (face.equals("top")) {
            texturePosition = block.getTopTexturePosition();
        } else if (face.equals("bottom")) {
            texturePosition = block.getBottomTexturePosition();
        }

        Vector2 uv1 = new Vector2(
            texturePosition.x * uvWidth,
            texturePosition.y * uvHeight + uvHeight
        );
        Vector2 uv2 = new Vector2(
            texturePosition.x * uvWidth,
            texturePosition.y * uvHeight
        );
        Vector2 uv3 = new Vector2(
            texturePosition.x * uvWidth + uvWidth,
            texturePosition.y * uvHeight
        );
        Vector2 uv4 = new Vector2(
            texturePosition.x * uvWidth + uvWidth,
            texturePosition.y * uvHeight + uvHeight
        );

        v1.setUV(uv1.x, uv1.y);
        v2.setUV(uv2.x, uv2.y);
        v3.setUV(uv3.x, uv3.y);
        v4.setUV(uv4.x, uv4.y);

        meshPartBuilder.triangle(v1, v2, v3);
        meshPartBuilder.triangle(v1, v3, v4);
    }

    private float getAO(Block block, Vector3[] directions) {
        for (Vector3 direction : directions) {
            int x = (int) (block.getGlobalPosition().x + direction.x);
            int y = (int) (block.getGlobalPosition().y + direction.y);
            int z = (int) (block.getGlobalPosition().z + direction.z);

            if (Terrain.INSTANCE.getBlockAt(x, y, z) == null) continue;
            if (Terrain.INSTANCE.getBlockAt(x, y, z).isTransparent()) continue;

            return 0.5f;
        }

        return 1.0f;
    }

    public void renderOpaque(ModelBatch batch, Environment environment) {
        if (modelInstance == null) return;
        batch.render(modelInstance, environment);
    }

    public void renderTransparent(ModelBatch batch, Environment environment) {
        if (transparentModelInstance == null) return;
        batch.render(transparentModelInstance, environment);
    }

    public void renderInvisible(ModelBatch batch, Environment environment) {
        if (alphaModelInstance == null) return;
        batch.render(alphaModelInstance, environment);
    }

    public BoundingBox[][][] getBoundingBoxes() {
        return boundingBoxes;
    }

    public BoundingBox[][][] getWaterBoundingBoxes() {
        return waterBoundingBoxes;
    }

    public Block getBlockAt(int localX, int localY, int localZ) {
        if (localX < 0 || localX >= SIZE_X) return null;
        if (localY < 0 || localY >= SIZE_Y) return null;
        if (localZ < 0 || localZ >= SIZE_Z) return null;

        return blocks[localX][localZ][localY];
    }

    public void removeBlockAt(int localX, int localY, int localZ) {
        setBlockAt(localX, localY, localZ, Block.EBlockType.AIR);
    }

    public void setBlockAt(int localX, int localY, int localZ, Block.EBlockType type) {
        if (localX < 0 || localX >= SIZE_X) return;
        if (localY < 0 || localY >= SIZE_Y) return;
        if (localZ < 0 || localZ >= SIZE_Z) return;

        blocks[localX][localZ][localY].setType(type);
    }

    @Override
    public void dispose() {
        if (opaqueModel != null) opaqueModel.dispose();
        if (transparentModel != null) transparentModel.dispose();
        if (alphaModel != null) alphaModel.dispose();
    }
}
