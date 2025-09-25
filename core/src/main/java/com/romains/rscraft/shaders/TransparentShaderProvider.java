package com.romains.rscraft.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;

public class TransparentShaderProvider extends DefaultShaderProvider {

    DefaultShader.Config albedoConfig;

    public TransparentShaderProvider() {
        super();

        albedoConfig = new DefaultShader.Config();
        albedoConfig.vertexShader = Gdx.files.internal("shaders/vertex.glsl").readString();
        albedoConfig.fragmentShader = Gdx.files.internal("shaders/fragment.glsl").readString();
        albedoConfig.defaultCullFace = 0;
    }

    @Override
    protected Shader createShader(Renderable renderable) {
        return new DefaultShader(renderable, albedoConfig);
    }
}
