package com.romains.rscraft.helpers;

import com.badlogic.gdx.math.Vector3;

public class Vector3Helper {

    private Vector3Helper() {

    }

    public static Vector3 getNormal(Vector3 a, Vector3 min, Vector3 max) {
        float d1 = Math.abs(a.x - min.x);
        float d2 = Math.abs(a.x - max.x);
        float d3 = Math.abs(a.y - min.y);
        float d4 = Math.abs(a.y - max.y);
        float d5 = Math.abs(a.z - min.z);
        float d6 = Math.abs(a.z - max.z);

        Vector3 normal = new Vector3(-1, 0, 0);

        float minD = d1;
        if (d2 < minD) {
            minD = d2;
            normal.set(1, 0, 0);
        }
        if (d3 < minD) {
            minD = d3;
            normal.set(0, -1, 0);
        }
        if (d4 < minD) {
            minD = d4;
            normal.set(0, 1, 0);
        }
        if (d5 < minD) {
            minD = d5;
            normal.set(0, 0, -1);
        }
        if (d6 < minD) {
            normal.set(0, 0, 1);
        }

        return normal;
    }
}
