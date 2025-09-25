package com.romains.rscraft.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Vector3;

public class SkyboxShaderProvider extends DefaultShaderProvider {

    private SkyboxShader shader;
    private ModelInstance skyboxModelInstance;
    private float skyboxRadius;
    private Color topColor;
    private Color bottomColor;

    public SkyboxShaderProvider(float skyboxRadius) {
        this.skyboxRadius = skyboxRadius;
        buildSkyboxModel();
    }

    private void buildSkyboxModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        Model model = modelBuilder.createSphere(
            skyboxRadius,
            skyboxRadius,
            skyboxRadius,
            100,
            100,
            new Material(
                ColorAttribute.createDiffuse(Color.BROWN),
                IntAttribute.createCullFace(GL20.GL_NONE)
            ),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        skyboxModelInstance = new ModelInstance(model);
    }

    public void render(ModelBatch batch) {
        batch.render(skyboxModelInstance);
    }

    public void setPosition(Vector3 position) {
        skyboxModelInstance.transform.setToTranslation(position);
    }

    @Override
    protected Shader createShader(Renderable renderable) {
        config.vertexShader = Gdx.files.internal("shaders/skybox.vert").readString();
        config.fragmentShader = Gdx.files.internal("shaders/skybox.frag").readString();

        shader = new SkyboxShader(renderable, config, this);

        return shader;
    }

    public float getSkyboxRadius() {
        return skyboxRadius;
    }

    public Color getTopColor() {
        return topColor;
    }

    public void setTopColor(Color topColor) {
        this.topColor = topColor;
    }

    public Color getBottomColor() {
        return bottomColor;
    }

    public void setBottomColor(Color bottomColor) {
        this.bottomColor = bottomColor;
    }

    public static class SkyboxShader extends DefaultShader {

        private SkyboxShaderProvider provider;

        public SkyboxShader(Renderable renderable, Config config, SkyboxShaderProvider provider) {
            super(renderable, config);
            this.provider = provider;
        }

        @Override
        public void begin(Camera camera, RenderContext context) {
            super.begin(camera, context);

            program.setUniformf("co_radius", provider.getSkyboxRadius());
            program.setUniformf("co_topColor", provider.getTopColor());
            program.setUniformf("co_bottomColor", provider.getBottomColor());
        }
    }
}
