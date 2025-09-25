package com.romains.rscraft.entities;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

/**
 * Données d'une partie de modèle 3D (bras, tête, etc.)
 *
 * Cette classe encapsule les informations nécessaires pour positionner
 * et orienter une partie d'un modèle 3D complexe (comme le joueur).
 *
 * Utilisée principalement pour le rendu du bras du joueur visible
 * en première personne.
 */
public class ModelPartData {

    public final ModelInstance modelInstance;  // Instance du modèle 3D à rendre
    public final Vector3 offset;               // Décalage par rapport au joueur
    public final Vector3 rotation;             // Rotation de la partie (degrés)
    public final float scale;                  // Échelle de la partie

    /**
     * Constructeur des données d'une partie de modèle.
     *
     * @param modelInstance Instance LibGDX du modèle 3D
     * @param offset Position relative par rapport au centre du joueur
     * @param rotation Angles de rotation (X, Y, Z) en degrés
     * @param scale Facteur d'échelle pour la taille
     */
    public ModelPartData(ModelInstance modelInstance, Vector3 offset, Vector3 rotation, float scale) {
        this.modelInstance = modelInstance;
        this.offset = offset;
        this.rotation = rotation;
        this.scale = scale;
    }
}
