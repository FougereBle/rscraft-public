package com.romains.rscraft.terrain;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.romains.rscraft.blocks.Block;
import com.romains.rscraft.entities.Player;

/**
 * Gestionnaire principal du terrain de RSCraft
 *
 * Cette classe gère l'ensemble du monde du jeu organisé en chunks (segments de terrain).
 * Fonctionnalités principales :
 * - Génération procédurale du terrain par chunks
 * - Chargement/déchargement dynamique des chunks selon la position du joueur
 * - Gestion des modifications de blocs (ajout/suppression)
 * - Optimisation du rendu en ne construisant que les chunks nécessaires
 * - Système de mise à jour différée pour les performances
 *
 * Le monde est divisé en une grille de chunks de taille fixe, chaque chunk
 * contenant une portion 3D du monde avec ses blocs et sa géométrie.
 */
public class Terrain {

    // === CONFIGURATION DU MONDE ===
    public final static int SIZE_X = 600;              // Nombre de chunks en largeur
    public final static int SIZE_Z = 600;              // Nombre de chunks en profondeur
    public final static int NB_CHUNKS = 16;            // Rayon de chunks à charger autour du joueur
    public final static int UPDATE_DELAY = 10;         // Délai entre les reconstructions de chunks

    // Instance singleton du gestionnaire de terrain
    public static Terrain INSTANCE;

    // === STOCKAGE DES CHUNKS ===
    private Chunk[][] chunks;                          // Grille des chunks chargés
    private boolean[][] chunksToBuild;                 // Chunks nécessitant une reconstruction

    // === GESTION DES MISES À JOUR ===
    private int tickCounter;                           // Compteur pour limiter les reconstructions

    public Terrain() {
        chunks = new Chunk[SIZE_X][SIZE_Z];
        chunksToBuild = new boolean[SIZE_X][SIZE_Z];

        INSTANCE = this;
    }

    public void generateStarterChunks(int caseX, int caseZ) {
        for (int x = caseX - NB_CHUNKS; x < caseX + NB_CHUNKS; x++) {
            for (int z = caseZ - NB_CHUNKS; z < caseZ + NB_CHUNKS; z++) {
                if (x < 0 || x >= SIZE_X) continue;
                if (z < 0 || z >= SIZE_Z) continue;

                if (chunks[x][z] != null) continue;

                generateChunk(x, z);
            }
        }

        for (int x = caseX - NB_CHUNKS; x < caseX + NB_CHUNKS; x++) {
            for (int z = caseZ - NB_CHUNKS; z < caseZ + NB_CHUNKS; z++) {
                if (x < 0 || x >= SIZE_X) continue;
                if (z < 0 || z >= SIZE_Z) continue;

                if (chunks[x][z] == null) continue;

                buildChunk(x, z);
            }
        }
    }

    private void generateChunk(int x, int z) {
        chunks[x][z] = new Chunk(new Vector3(
            x * Chunk.SIZE_X,
            0,
            z * Chunk.SIZE_Z
        ));
        chunks[x][z].generate();

        chunksToBuild[x][z] = true;

        updateNeighbors(x, z);
    }

    private void buildChunk(int x, int z) {
        chunks[x][z].build();
        chunksToBuild[x][z] = false;
    }

    private void updateNeighbors(int x, int z) {
        int up = z - 1;
        int down = z + 1;
        int left = x - 1;
        int right = x + 1;

        updateChunk(left, up);
        updateChunk(left, z);
        updateChunk(left, down);

        updateChunk(x, up);
        updateChunk(x, down);

        updateChunk(right, up);
        updateChunk(right, z);
        updateChunk(right, down);
    }

    private void updateChunk(int x, int z) {
        if (x < 0 || x >= SIZE_X) return;
        if (z < 0 || z >= SIZE_Z) return;

        chunksToBuild[x][z] = true;
    }

    public Block getBlockAt(int globalX, int globalY, int globalZ) {
        int caseX = globalX / Chunk.SIZE_X;
        int caseZ = globalZ / Chunk.SIZE_X;

        if (caseX < 0 || caseX >= SIZE_X) return null;
        if (caseZ < 0 || caseZ >= SIZE_Z) return null;

        if (chunks[caseX][caseZ] == null) return null;

        int localX = ((globalX % Chunk.SIZE_X) + Chunk.SIZE_X) % Chunk.SIZE_X;
        int localY = ((globalY % Chunk.SIZE_Y) + Chunk.SIZE_Y) % Chunk.SIZE_Y;
        int localZ = ((globalZ % Chunk.SIZE_Z) + Chunk.SIZE_Z) % Chunk.SIZE_Z;

        return chunks[caseX][caseZ].getBlockAt(localX, localY, localZ);
    }

    public void removeBlockAt(int globalX, int globalY, int globalZ) {
        int caseX = globalX / Chunk.SIZE_X;
        int caseZ = globalZ / Chunk.SIZE_X;

        if (caseX < 0 || caseX >= SIZE_X) return;
        if (caseZ < 0 || caseZ >= SIZE_Z) return;

        if (chunks[caseX][caseZ] == null) return;

        int localX = ((globalX % Chunk.SIZE_X) + Chunk.SIZE_X) % Chunk.SIZE_X;
        int localY = ((globalY % Chunk.SIZE_Y) + Chunk.SIZE_Y) % Chunk.SIZE_Y;
        int localZ = ((globalZ % Chunk.SIZE_Z) + Chunk.SIZE_Z) % Chunk.SIZE_Z;

        chunks[caseX][caseZ].removeBlockAt(localX, localY, localZ);

        buildChunk(caseX, caseZ);
        smartBuildNeighbors(caseX, caseZ);

        tickCounter = UPDATE_DELAY;
    }

    private void smartBuildNeighbors(int x, int z) {
        int up = z - 1;
        int down = z + 1;
        int left = x - 1;
        int right = x + 1;

        updateChunk(left, up);
        buildChunk(left, z);
        updateChunk(left, down);

        buildChunk(x, up);
        buildChunk(x, down);

        updateChunk(right, up);
        buildChunk(right, z);
        updateChunk(right, down);
    }

    public void setBlockAt(int globalX, int globalY, int globalZ, Block.EBlockType type) {
        setBlockAt(globalX, globalY, globalZ, type, true);
    }

    public void setBlockAt(int globalX, int globalY, int globalZ, Block.EBlockType type, boolean instantRebuild) {
        int caseX = globalX / Chunk.SIZE_X;
        int caseZ = globalZ / Chunk.SIZE_X;

        if (caseX < 0 || caseX >= SIZE_X) return;
        if (caseZ < 0 || caseZ >= SIZE_Z) return;

        if (chunks[caseX][caseZ] == null) {
            generateChunk(caseX, caseZ);
        }

        int localX = ((globalX % Chunk.SIZE_X) + Chunk.SIZE_X) % Chunk.SIZE_X;
        int localY = ((globalY % Chunk.SIZE_Y) + Chunk.SIZE_Y) % Chunk.SIZE_Y;
        int localZ = ((globalZ % Chunk.SIZE_Z) + Chunk.SIZE_Z) % Chunk.SIZE_Z;

        chunks[caseX][caseZ].setBlockAt(localX, localY, localZ, type);

        if (instantRebuild) {
            buildChunk(caseX, caseZ);
        } else {
            updateChunk(caseX, caseZ);
        }

        updateNeighbors(caseX, caseZ);

        tickCounter = UPDATE_DELAY;
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        int caseX = MathUtils.floor(Player.INSTANCE.getPosition().x / Chunk.SIZE_X);
        int caseZ = MathUtils.floor(Player.INSTANCE.getPosition().z / Chunk.SIZE_Z);

        modelBatch.begin(Player.INSTANCE.getCamera());
        for (int x = caseX - NB_CHUNKS; x < caseX + NB_CHUNKS; x++) {
            for (int z = caseZ - NB_CHUNKS; z < caseZ + NB_CHUNKS; z++) {
                if (x < 0 || x >= SIZE_X) continue;
                if (z < 0 || z >= SIZE_Z) continue;

                if (chunks[x][z] == null) continue;

                chunks[x][z].renderOpaque(modelBatch, environment);
            }
        }
        for (int x = caseX - NB_CHUNKS; x < caseX + NB_CHUNKS; x++) {
            for (int z = caseZ - NB_CHUNKS; z < caseZ + NB_CHUNKS; z++) {
                if (x < 0 || x >= SIZE_X) continue;
                if (z < 0 || z >= SIZE_Z) continue;

                if (chunks[x][z] == null) continue;

                chunks[x][z].renderTransparent(modelBatch, environment);
            }
        }
        for (int x = caseX - NB_CHUNKS; x < caseX + NB_CHUNKS; x++) {
            for (int z = caseZ - NB_CHUNKS; z < caseZ + NB_CHUNKS; z++) {
                if (x < 0 || x >= SIZE_X) continue;
                if (z < 0 || z >= SIZE_Z) continue;

                if (chunks[x][z] == null) continue;

                chunks[x][z].renderInvisible(modelBatch, environment);
            }
        }
        modelBatch.end();
    }

    public void update(float delta) {
        if (Player.INSTANCE == null) return;

        tickCounter++;

        int caseX = MathUtils.floor((Player.INSTANCE.getPosition().x + Chunk.SIZE_X / 2f) / Chunk.SIZE_X);
        int caseZ = MathUtils.floor((Player.INSTANCE.getPosition().z + Chunk.SIZE_X / 2f) / Chunk.SIZE_Z);

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                if (x >= caseX - NB_CHUNKS && x <= caseX + NB_CHUNKS && z >= caseZ - NB_CHUNKS && z <= caseZ + NB_CHUNKS)
                    continue;
                if (chunks[x][z] == null) continue;

                removeChunk(x, z);
            }
        }

        for (int x = caseX - NB_CHUNKS; x < caseX + NB_CHUNKS; x++) {
            for (int z = caseZ - NB_CHUNKS; z < caseZ + NB_CHUNKS; z++) {
                if (x < 0 || x >= SIZE_X) continue;
                if (z < 0 || z >= SIZE_Z) continue;

                if (chunks[x][z] != null) continue;

                generateChunk(x, z);
            }
        }

        if (tickCounter < UPDATE_DELAY) return;

        tickCounter = 0;

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                if (chunks[x][z] == null) continue;
                if (!chunksToBuild[x][z]) continue;

                buildChunk(x, z);

                break;
            }
        }
    }

    private void removeChunk(int x, int z) {
        chunks[x][z].dispose();
        chunks[x][z] = null;
    }

    public int getTerrainHeight(int globalX, int globalZ) {
        int x = globalX / Chunk.SIZE_X;
        int z = globalZ / Chunk.SIZE_Z;

        if (x < 0 || x >= SIZE_X) return Chunk.SIZE_Y;
        if (z < 0 || z >= SIZE_Z) return Chunk.SIZE_Y;

        if (chunks[x][z] == null) return Chunk.SIZE_Y;

        int localX = ((globalX % Chunk.SIZE_X) + Chunk.SIZE_X) % Chunk.SIZE_X;
        int localZ = ((globalZ % Chunk.SIZE_Z) + Chunk.SIZE_Z) % Chunk.SIZE_Z;

        return chunks[x][z].getTerrainHeight(localX, localZ);
    }

    public Chunk[][] getChunks() {
        return chunks;
    }
}
