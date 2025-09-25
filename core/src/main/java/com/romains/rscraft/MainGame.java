package com.romains.rscraft;

import com.badlogic.gdx.Game;
import com.romains.rscraft.screens.TerrainGenerationLoadingScreen;

/**
 * Classe principale du jeu RSCraft - Clone de Minecraft
 *
 * Cette classe hérite de Game de LibGDX et sert de point d'entrée principal
 * pour l'application. Elle gère l'initialisation du jeu et le passage vers
 * l'écran de chargement de génération de terrain.
 *
 * Le jeu utilise le pattern Singleton pour permettre un accès global à
 * l'instance principale du jeu depuis n'importe où dans l'application.
 */
public class MainGame extends Game {

    // Instance singleton accessible globalement
    public static MainGame INSTANCE;

    /**
     * Méthode appelée automatiquement au démarrage de l'application.
     * Initialise l'instance singleton et lance l'écran de chargement
     * pour la génération du terrain.
     */
    @Override
    public void create() {
        INSTANCE = this;
        setScreen(new TerrainGenerationLoadingScreen());
    }
}
