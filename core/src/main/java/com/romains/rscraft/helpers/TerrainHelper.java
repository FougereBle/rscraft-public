package com.romains.rscraft.helpers;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.romains.rscraft.terrain.Chunk;
import com.romains.rscraft.terrain.Terrain;

/**
 * Utilitaires pour les interactions avec le terrain
 *
 * Cette classe fournit des méthodes statiques pour :
 * - Calcul de raycast (lancer de rayon) dans le monde 3D
 * - Détection de collision rayon/bloc
 * - Calcul de normales de surface pour le placement de blocs
 *
 * Principalement utilisée pour déterminer quel bloc le joueur vise
 * avec sa souris (pour casser ou placer des blocs).
 */
public class TerrainHelper {

    private TerrainHelper() {

    }

    /**
     * Lance un rayon depuis une position vers une direction et trouve le premier bloc touché.
     *
     * @param from Position de départ du rayon (généralement la caméra)
     * @param direction Direction du rayon (généralement la direction de vue)
     * @param distance Distance maximale du rayon
     * @return RayPoint contenant le point d'intersection, la normale et la position du bloc, ou null si aucune collision
     */
    public static RayPoint getRayPoint(Vector3 from, Vector3 direction, float distance) {
        Ray ray = new Ray(from, direction);

        int caseX = MathUtils.floor(from.x / Chunk.SIZE_X);
        int caseZ = MathUtils.floor(from.z / Chunk.SIZE_Z);

        Vector3 rayPoint = null;
        Vector3 blockPosition = new Vector3();
        float minDistance = distance;

        for (int cx = caseX - 1; cx <= caseX + 1; cx++) {
            for (int cz = caseZ - 1; cz <= caseZ + 1; cz++) {
                Chunk chunk = Terrain.INSTANCE.getChunks()[cx][cz];

                if (chunk == null) continue;

                BoundingBox[][][] boxes = chunk.getBoundingBoxes();

                for (int x = 0; x < boxes.length; x++) {
                    for (int z = 0; z < boxes[x].length; z++) {
                        for (int y = 0; y < boxes[x][z].length; y++) {
                            Vector3 intersectionPoint = new Vector3();

                            if (boxes[x][z][y] == null) continue;

                            if (Intersector.intersectRayBounds(ray, boxes[x][z][y], intersectionPoint)) {
                                if (from.dst(intersectionPoint) < minDistance) {
                                    minDistance = from.dst(intersectionPoint);
                                    rayPoint = intersectionPoint;
                                    boxes[x][z][y].getCenter(blockPosition);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (rayPoint == null) return null;

        Vector3 normal = Vector3Helper.getNormal(
            rayPoint,
            new Vector3(blockPosition.x - 0.5f, blockPosition.y - 0.5f, blockPosition.z - 0.5f),
            new Vector3(blockPosition.x + 0.5f, blockPosition.y + 0.5f, blockPosition.z + 0.5f)
        );

        return new RayPoint(
            rayPoint,
            normal,
            blockPosition
        );
    }

    public static class RayPoint {
        public final Vector3 point;
        public final Vector3 normal;
        public final Vector3 globalPosition;

        public RayPoint(Vector3 point, Vector3 normal, Vector3 globalPosition) {
            this.point = point;
            this.normal = normal;
            this.globalPosition = globalPosition;
        }
    }
}
