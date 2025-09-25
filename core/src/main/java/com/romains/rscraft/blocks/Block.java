package com.romains.rscraft.blocks;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.romains.rscraft.terrain.Chunk;

/**
 * Représente un bloc individuel dans le monde de RSCraft
 *
 * Cette classe encapsule toutes les propriétés d'un bloc :
 * - Type de bloc (air, herbe, pierre, eau, etc.)
 * - Position dans le monde (locale et globale)
 * - Propriétés de rendu (textures, transparence)
 * - Propriétés physiques (collision, transparence)
 *
 * Chaque bloc connaît sa position locale dans son chunk
 * ainsi que sa position globale dans le monde. Les propriétés
 * visuelles sont automatiquement définies selon le type de bloc
 * via le BlockManager.
 */
public class Block {

    // === COORDONNÉES DE TEXTURE ===
    public Vector2 texturePosition;        // Position de la texture principale (côtés)
    public Vector2 topTexturePosition;     // Position de la texture du dessus
    public Vector2 bottomTexturePosition;  // Position de la texture du dessous

    // === POSITIONS DANS LE MONDE ===
    private Vector3 localPosition;         // Position locale dans le chunk (0-7)
    private Vector3 globalPosition;        // Position globale dans le monde

    // === PROPRIÉTÉS DU BLOC ===
    private EBlockType type;               // Type de bloc (herbe, pierre, etc.)
    private boolean isTransparent;         // Bloc transparent (ne bloque pas la vue)
    private boolean isAlphaZero;           // Bloc avec transparence alpha (feuilles)

    /**
     * Constructeur d'un bloc.
     * Calcule automatiquement la position locale à partir de la position globale.
     *
     * @param type Type du bloc (défini les propriétés visuelles et physiques)
     * @param globalPosition Position absolue du bloc dans le monde
     */
    public Block(EBlockType type, Vector3 globalPosition) {
        // Stockage de la position globale
        this.globalPosition = new Vector3(
            globalPosition.x,
            globalPosition.y,
            globalPosition.z
        );

        // Calcul de la position locale dans le chunk via modulo
        this.localPosition = new Vector3(
            globalPosition.x % Chunk.SIZE_X,
            globalPosition.y % Chunk.SIZE_Y,
            globalPosition.z % Chunk.SIZE_Z
        );

        // Définition du type et chargement des propriétés associées
        setType(type);
    }

    public Vector2 getTexturePosition() {
        return texturePosition;
    }

    public Vector2 getTopTexturePosition() {
        return topTexturePosition;
    }

    public Vector2 getBottomTexturePosition() {
        return bottomTexturePosition;
    }

    public Vector3 getLocalPosition() {
        return localPosition;
    }

    public Vector3 getGlobalPosition() {
        return globalPosition;
    }

    public EBlockType getType() {
        return type;
    }

    /**
     * Définit le type du bloc et met à jour toutes ses propriétés.
     * Récupère automatiquement les données depuis le BlockManager.
     *
     * @param type Nouveau type de bloc
     */
    public void setType(EBlockType type) {
        this.type = type;

        // Récupération des propriétés depuis le gestionnaire de blocs
        BlockManager.BlockData blockData = BlockManager.getBlockData(type);

        // Mise à jour des propriétés de transparence
        this.isTransparent = blockData.isTransparent;
        this.isAlphaZero = blockData.isAlphaZero;

        // Mise à jour des coordonnées de texture
        this.texturePosition = blockData.texturePosition;
        this.topTexturePosition = blockData.topTexturePosition;
        this.bottomTexturePosition = blockData.bottomTexturePosition;
    }

    public boolean isTransparent() {
        return isTransparent;
    }

    public boolean isAlphaZero() {
        return isAlphaZero;
    }

    /**
     * Énumération des types de blocs disponibles dans RSCraft.
     * Chaque type définit l'apparence et le comportement d'un bloc.
     */
    public enum EBlockType {
        AIR,    // Bloc invisible et traversable
        GRASS,  // Bloc d'herbe (surface du terrain)
        STONE,  // Bloc de pierre (structure de base)
        WOOD,   // Bloc de bois (troncs d'arbres)
        LEAF,   // Bloc de feuilles (transparence alpha)
        WATER   // Bloc d'eau (liquide transparent)
    }
}
