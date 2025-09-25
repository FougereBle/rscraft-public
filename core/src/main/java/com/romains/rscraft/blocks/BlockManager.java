package com.romains.rscraft.blocks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

import java.util.Map;

/**
 * Gestionnaire des types de blocs et de leurs propriétés dans RSCraft
 *
 * Cette classe centralise la définition de tous les types de blocs disponibles
 * dans le jeu, leurs propriétés visuelles et comportementales :
 * - Mapping entre types de blocs et leurs données
 * - Gestion des textures et coordonnées dans l'atlas de textures
 * - Propriétés de transparence et d'affichage
 * - Différenciation des textures par face (haut, côtés, bas)
 *
 * Utilise le pattern Singleton pour un accès global aux données des blocs.
 * Chaque type de bloc est défini avec ses coordonnées de texture et ses propriétés.
 */
public class BlockManager {

    private final static Map<Block.EBlockType, BlockData> MAPPING = Map.ofEntries(
        Map.entry(Block.EBlockType.AIR, new BlockData()
            .transparent(true)
            .alphaZero(false)
            .texture(0, 0, 0, 0, 0, 0)
        ),
        Map.entry(Block.EBlockType.GRASS, new BlockData()
            .transparent(false)
            .alphaZero(false)
            .texture(1, 0, 2, 0, 2, 0)
        ),
        Map.entry(Block.EBlockType.STONE, new BlockData()
            .transparent(false)
            .alphaZero(false)
            .texture(3, 0, 3, 0, 3, 0)
        ),
        Map.entry(Block.EBlockType.WOOD, new BlockData()
            .transparent(false)
            .alphaZero(false)
            .texture(0, 1, 1, 1, 1, 1)
        ),
        Map.entry(Block.EBlockType.LEAF, new BlockData()
            .transparent(true)
            .alphaZero(true)
            .texture(2, 1, 2, 1, 2, 1)
        ),
        Map.entry(Block.EBlockType.WATER, new BlockData()
            .transparent(true)
            .alphaZero(false)
            .texture(3, 1, 3, 1, 3, 1)
        )
    );

    public static int TEXTURE_SIZE = 16;

    private static BlockManager instance;

    private Texture texture;

    public BlockManager() {
        texture = new Texture(Gdx.files.internal("texture.png"));
    }

    public static BlockData getBlockData(Block.EBlockType type) {
        return MAPPING.get(type);
    }

    public static BlockManager getInstance() {
        if (instance == null) {
            instance = new BlockManager();
        }
        return instance;
    }

    public Texture getTexture() {
        return texture;
    }

    static class BlockData {
        public boolean isTransparent;
        public boolean isAlphaZero;
        public Vector2 texturePosition;
        public Vector2 topTexturePosition;
        public Vector2 bottomTexturePosition;

        public BlockData() {
            isTransparent = false;
            isAlphaZero = false;
            texturePosition = new Vector2(0, 0);
            topTexturePosition = new Vector2(0, 0);
            bottomTexturePosition = new Vector2(0, 0);
        }

        public BlockData transparent(boolean isTransparent) {
            this.isTransparent = isTransparent;
            return this;
        }

        public BlockData alphaZero(boolean isAlphaZero) {
            this.isAlphaZero = isAlphaZero;
            return this;
        }

        public BlockData texture(int x, int y, int tx, int ty, int bx, int by) {
            this.texturePosition.set(x, y);
            this.topTexturePosition.set(tx, ty);
            this.bottomTexturePosition.set(bx, by);
            return this;
        }
    }
}
