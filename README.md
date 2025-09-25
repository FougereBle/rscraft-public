# RSCraft 🎮

Un clone de Minecraft développé en Java avec LibGDX dans un but d'amusement et de défi personnel.

## 📖 Description

RSCraft est une implémentation personnelle inspirée de Minecraft, créée pour explorer les concepts de développement de
jeux 3D, de génération procédurale de terrain et d'optimisation des performances. Le projet met l'accent sur :

- **Génération de terrain procédurale** : Création automatique d'un monde infini avec des biomes variés
- **Système de chunks optimisé** : Chargement/déchargement dynamique des portions de terrain
- **Physique réaliste** : Gravité, collision, nage et mécanique de saut
- **Rendu 3D performant** : Optimisations pour le rendu de grandes quantités de blocs
- **Cycle jour/nuit** : Système d'éclairage dynamique
- **Interactions avec le monde** : Placement et destruction de blocs

## 🎯 Fonctionnalités

### ✅ Implémentées

- 🌍 **Génération de terrain procédurale** avec différents types de blocs (herbe, pierre, eau, bois, feuilles)
- 🎮 **Contrôles en première personne** avec caméra libre
- 🏃 **Système de physique** : gravité, collisions, saut
- 🏊 **Mécanique d'eau** : nage, physique sous-marine
- 🔨 **Interaction avec le monde** : casser et placer des blocs
- 🌅 **Cycle jour/nuit** avec éclairage dynamique
- 💧 **Effets visuels** : overlay sous l'eau, brouillard atmosphérique
- 🎯 **Interface utilisateur** : crosshair, informations de debug
- ⚡ **Optimisations** : rendu par chunks, culling, mise à jour différée

### 🔧 Architecture Technique

- **Moteur** : LibGDX (Java)
- **Rendu 3D** : OpenGL avec batching optimisé
- **Gestion mémoire** : Chargement/déchargement dynamique des chunks
- **Patterns utilisés** : Singleton, Builder, Observer
- **Shaders personnalisés** : Gestion de la transparence et des effets visuels

## 🚀 Installation et Lancement

### Prérequis

- **Java JDK 17** ou supérieur
- **Git** pour cloner le repository

### Instructions d'installation

1. **Cloner le repository**
   ```bash
   git clone https://github.com/FougereBle/rscraft-public.git
   cd RSCraft
   ```

2. **Lancer avec Gradle**
   ```bash
   # Sur Windows
   gradlew lwjgl3:run

   # Sur Linux/Mac
   ./gradlew lwjgl3:run
   ```

3. **Alternative : Importer dans un IDE**
    - **IntelliJ IDEA** : File → Open → Sélectionner le dossier du projet
    - **Eclipse** : File → Import → Existing Gradle Project
    - Lancer la classe `Lwjgl3Launcher` dans le module `lwjgl3`

### Build du projet

```bash
# Compiler le projet
./gradlew build

# Créer une distribution
./gradlew lwjgl3:dist
```

## 🎮 Contrôles

| Touche/Action                  | Fonction                    |
|--------------------------------|-----------------------------|
| **ZQSD**                       | Se déplacer                 |
| **Espace**                     | Sauter / Nager vers le haut |
| **Souris**                     | Regarder autour             |
| **Clic gauche**                | Casser un bloc              |
| **Clic droit**                 | Placer un bloc (pierre)     |
| **Échap**                      | Libérer la souris           |
| **Clic gauche** (souris libre) | Capturer la souris          |

## 🏗️ Structure du Projet

```
RSCraft/
├── core/src/main/java/com/romains/rscraft/
│   ├── MainGame.java              # Point d'entrée principal
│   ├── entities/
│   │   ├── Player.java            # Logique du joueur
│   │   └── ModelPartData.java     # Données des modèles 3D
│   ├── terrain/
│   │   ├── Terrain.java           # Gestionnaire du monde
│   │   ├── Chunk.java             # Segments de terrain
│   │   └── DayNightCycle.java     # Cycle jour/nuit
│   ├── blocks/
│   │   ├── Block.java             # Types de blocs
│   │   └── BlockManager.java      # Gestion des propriétés
│   ├── screens/
│   │   ├── GameScreen.java        # Écran de jeu principal
│   │   └── TerrainGenerationLoadingScreen.java
│   ├── shaders/                   # Shaders personnalisés
│   └── helpers/                   # Utilitaires et helpers
├── lwjgl3/                        # Configuration desktop
├── assets/                        # Ressources (textures, modèles)
└── build.gradle                   # Configuration de build
```

## 🛠️ Technologies Utilisées

- **[LibGDX](https://libgdx.com/)** - Framework de développement de jeux multiplateforme
- **Java 17** - Langage de programmation
- **OpenGL** - Rendu graphique 3D
- **Gradle** - Système de build
- **LWJGL3** - Bibliothèque de jeux légère pour Java

## 📊 Performances

Le jeu a été optimisé pour maintenir de bonnes performances :

- **Génération à la demande** : Seuls les chunks visibles sont générés
- **Culling intelligent** : Les faces cachées ne sont pas rendues
- **Mise à jour différée** : Les reconstructions de mesh sont étalées dans le temps
- **Gestion mémoire** : Déchargement automatique des chunks éloignés

## 🎨 Assets

Les textures et modèles sont organisés dans le dossier `assets/` :

- `texture.png` : Atlas de textures des blocs
- `data/entities/` : Modèles 3D des entités
- `fonts/` : Polices pour l'interface utilisateur
- `gui/` : Éléments d'interface (crosshair, overlays)

## 🐛 Debug et Développement

L'interface de debug affiche en temps réel :

- **FPS** : Performances en images par seconde
- **Position** : Coordonnées du joueur dans le monde
- **Chunk** : Coordonnées du chunk actuel

## 📝 Notes de Développement

Ce projet a été créé dans un esprit d'apprentissage et de challenge personnel. Il explore notamment :

- Les algorithmes de génération procédurale
- L'optimisation du rendu 3D en temps réel
- La gestion de la mémoire pour des mondes de grande taille
- Les systèmes de physique simples mais efficaces
- L'architecture logicielle pour les jeux

## 🤝 Contribution

Ce projet étant un exercice personnel, les contributions ne sont pas attendues, mais les suggestions et retours
constructifs sont toujours les bienvenus !

## 📄 Licence

Ce projet est développé à des fins éducatives et de loisir. Il s'inspire de Minecraft mais n'a aucune affiliation
officielle avec Mojang Studios.
