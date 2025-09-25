package com.romains.rscraft.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.romains.rscraft.entities.ModelPartData;

import java.util.HashMap;
import java.util.Map;

public class EntityBuilderHelper {

    private final static Vector3[] VERTICES = new Vector3[]{
        new Vector3(0, 0, 1), // 0
        new Vector3(0, 1, 1), // 1
        new Vector3(1, 1, 1), // 2
        new Vector3(1, 0, 1), // 3

        new Vector3(0, 0, 0), // 4
        new Vector3(0, 1, 0), // 5

        new Vector3(1, 1, 0), // 6
        new Vector3(1, 0, 0) // 7
    };

    private final static Map<String, int[]> FACES = Map.ofEntries(
        Map.entry("top", new int[]{2, 1, 5, 6}),
        Map.entry("bottom", new int[]{3, 0, 4, 7}),
        Map.entry("front", new int[]{4, 7, 6, 5}),
        Map.entry("back", new int[]{3, 0, 1, 2}),
        Map.entry("left", new int[]{0, 4, 5, 1}),
        Map.entry("right", new int[]{7, 3, 2, 6})
    );

    private final static Map<String, Vector3> NORMALS = Map.ofEntries(
        Map.entry("top", new Vector3(0, 0, 1)),
        Map.entry("bottom", new Vector3(0, 0, -1)),
        Map.entry("front", new Vector3(0, -1, 0)),
        Map.entry("back", new Vector3(0, 1, 0)),
        Map.entry("left", new Vector3(1, 0, 0)),
        Map.entry("right", new Vector3(-1, 0, 0))
    );

    private EntityBuilderHelper() {

    }

    public static HashMap<String, ModelPartData> buildEntityFromFile(String fileName) {
        HashMap<String, ModelPartData> instances = new HashMap<>();

        JsonValue jsonValue = new JsonReader().parse(Gdx.files.internal("data/entities/" + fileName).readString());

        for (JsonValue jsonModel : jsonValue.get("models")) {
            ModelBuilder modelBuilder = new ModelBuilder();
            modelBuilder.begin();

            MeshPartBuilder builder = modelBuilder.part(
                jsonModel.name,
                GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                new Material(
                    ColorAttribute.createDiffuse(Color.WHITE),
                    TextureAttribute.createDiffuse(new Texture(Gdx.files.internal(jsonValue.getString("texture"))))
                )
            );

            if (jsonModel.has("model")) {
                if (jsonModel.getString("model").equals("box")) {
                    buildBox(builder, jsonValue, jsonModel);
                }
            } else {
                float uvWidth = 1f / jsonValue.getFloat("textureCols");
                float uvHeight = 1f / jsonValue.getFloat("textureRows");

                for (JsonValue jsonPart : jsonModel.get("parts")) {
                    float u1 = jsonPart.get("uv").getFloat(0) * uvWidth;
                    float u2 = jsonPart.get("uv").getFloat(1) * uvWidth;
                    float v1 = jsonPart.get("uv").getFloat(2) * uvHeight;
                    float v2 = jsonPart.get("uv").getFloat(3) * uvHeight;

                    JsonValue vertices = jsonPart.get("vertices");
                    JsonValue normals = jsonPart.get("normals");

                    builder.setUVRange(u1, v1, u2, v2);
                    builder.rect(
                        vertices.get(0).get(0).asFloat(), vertices.get(0).get(1).asFloat(), vertices.get(0).get(2).asFloat(),
                        vertices.get(1).get(0).asFloat(), vertices.get(1).get(1).asFloat(), vertices.get(1).get(2).asFloat(),
                        vertices.get(2).get(0).asFloat(), vertices.get(2).get(1).asFloat(), vertices.get(2).get(2).asFloat(),
                        vertices.get(3).get(0).asFloat(), vertices.get(3).get(1).asFloat(), vertices.get(3).get(2).asFloat(),
                        normals.get(0).asFloat(), normals.get(1).asFloat(), normals.get(2).asFloat()
                    );
                }
            }

            Model model = modelBuilder.end();

            instances.put(
                jsonModel.name,
                new ModelPartData(
                    new ModelInstance(model),
                    new Vector3(
                        jsonModel.get("offset").getFloat(0),
                        jsonModel.get("offset").getFloat(1),
                        jsonModel.get("offset").getFloat(2)
                    ),
                    new Vector3(
                        jsonModel.get("rotation").getFloat(0),
                        jsonModel.get("rotation").getFloat(1),
                        jsonModel.get("rotation").getFloat(2)
                    ),
                    jsonValue.getFloat("scale", 1)
                )
            );
        }

        return instances;
    }

    private static void buildBox(MeshPartBuilder meshPartBuilder, JsonValue jsonValue, JsonValue jsonModel) {
        buildBoxFace(meshPartBuilder, "top", jsonValue, jsonModel);
        buildBoxFace(meshPartBuilder, "bottom", jsonValue, jsonModel);
        buildBoxFace(meshPartBuilder, "front", jsonValue, jsonModel);
        buildBoxFace(meshPartBuilder, "back", jsonValue, jsonModel);
        buildBoxFace(meshPartBuilder, "left", jsonValue, jsonModel);
        buildBoxFace(meshPartBuilder, "right", jsonValue, jsonModel);
    }

    private static void buildBoxFace(MeshPartBuilder meshPartBuilder, String face, JsonValue jsonValue, JsonValue jsonModel) {
        float scale = jsonValue.getFloat("scale");

        Vector3 size = new Vector3(
            jsonModel.getFloat("width") * scale,
            jsonModel.getFloat("height") * scale,
            jsonModel.getFloat("depht") * scale
        );

        float pivotX = 0;
        float pivotY = 0;
        float pivotZ = 0;

        if (jsonModel.has("pivot")) {
            pivotX = jsonModel.get("pivot").getFloat(0) * scale;
            pivotY = jsonModel.get("pivot").getFloat(1) * scale;
            pivotZ = jsonModel.get("pivot").getFloat(2) * scale;
        }

        float offsetX = 0;
        float offsetY = 0;
        float offsetZ = 0;

        float nbCols = jsonValue.getFloat("textureCols");
        float nbRows = jsonValue.getFloat("textureRows");

        float uvWidth = 1.0f / nbCols;
        float uvHeight = 1.0f / nbRows;

        if (jsonModel.get("uvs").has(face)) {
            float uvu1 = jsonModel.get("uvs").get(face).getFloat(0) * uvWidth;
            float uvu2 = jsonModel.get("uvs").get(face).getFloat(1) * uvWidth;
            float uvv1 = jsonModel.get("uvs").get(face).getFloat(2) * uvHeight;
            float uvv2 = jsonModel.get("uvs").get(face).getFloat(3) * uvHeight;

            meshPartBuilder.setUVRange(uvu1, uvv1, uvu2, uvv2);
        }

        meshPartBuilder.rect(
            VERTICES[FACES.get(face)[0]].x * (size.x) + pivotX + offsetX - size.x / 2f,
            VERTICES[FACES.get(face)[0]].y * (size.y) + pivotY + offsetY - size.y / 2f,
            VERTICES[FACES.get(face)[0]].z * (size.z) + pivotZ + offsetZ - size.z / 2f,

            VERTICES[FACES.get(face)[1]].x * (size.x) + pivotX + offsetX - size.x / 2f,
            VERTICES[FACES.get(face)[1]].y * (size.y) + pivotY + offsetY - size.y / 2f,
            VERTICES[FACES.get(face)[1]].z * (size.z) + pivotZ + offsetZ - size.z / 2f,

            VERTICES[FACES.get(face)[2]].x * (size.x) + pivotX + offsetX - size.x / 2f,
            VERTICES[FACES.get(face)[2]].y * (size.y) + pivotY + offsetY - size.y / 2f,
            VERTICES[FACES.get(face)[2]].z * (size.z) + pivotZ + offsetZ - size.z / 2f,

            VERTICES[FACES.get(face)[3]].x * (size.x) + pivotX + offsetX - size.x / 2f,
            VERTICES[FACES.get(face)[3]].y * (size.y) + pivotY + offsetY - size.y / 2f,
            VERTICES[FACES.get(face)[3]].z * (size.z) + pivotZ + offsetZ - size.z / 2f,

            NORMALS.get(face).x, NORMALS.get(face).y, NORMALS.get(face).z
        );
    }

}
