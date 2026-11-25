# Chrome Néon

Mod Minecraft cyberpunk : cyberware réparti dans tout le corps, hacking, HUD néon, caméra numérique et VESPER,
l'IA de biopuce. Mod indépendant (aucune dépendance à Arcanes Oubliés ni à un autre mod).

| | |
|---|---|
| Mod ID | `chromeneon` |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.x (développé avec 21.1.262) |
| Java | 21 |
| Build | Gradle 9.2.1 (wrapper) + ModDevGradle 2.0.148 |

## État : phase 1 terminée (fondations)

Voir [docs/ROADMAP.md](docs/ROADMAP.md) pour les phases et [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) pour la
structure du code.

Ce qui existe et fonctionne réellement :

- **Registre des implants** `chromeneon:implant`, synchronisé client/serveur, prêt à recevoir les implants (phase 3).
  Il est volontairement vide : aucun implant factice.
- **Architecture du corps** : 7 emplacements (Système, Optiques, Bras, Jambes, Dermique, Circulatoire, Biopuce),
  chacun avec un nombre de sockets.
- **Règles d'installation** : implant inconnu, déjà installé, emplacement plein, conflit (groupes exclusifs, ex. un
  seul système d'exploitation), humanité insuffisante. Une installation refusée ne modifie rien.
- **Données persistantes du joueur** (attachment NeoForge) : implants par emplacement, implants désactivés, RAM.
  Elles survivent à la mort et à la déconnexion. L'humanité est calculée à partir des implants, pas stockée.
- **RAM** : cellules entières, capacité = base + bonus des implants actifs (plafonnée), régénération d'une cellule
  par période configurable, consommation tout-ou-rien.
- **Synchronisation** serveur → client à la connexion, à la réapparition, au changement de dimension et à chaque
  changement.
- **Configurations** : serveur (humanité, RAM) et client (HUD activé, opacité, glitch), éditables en jeu.
- **Contenu** : Puce neuronale (emblème du mod), Scanner de diagnostic (affiche l'état cyberware du joueur),
  Fauteuil de charcudoc (bloc orientable, encore inerte).
- **HUD** : premier élément, les cellules de RAM à droite de la barre d'action, angles coupés, désactivable.
- **Commande** `/chromeneon` : `status [joueur]`, `ram set <joueur> <cellules>`, `implant install|remove <joueur> <id>`
  (sauf `status` sur soi, réservée aux opérateurs).

## Compiler

```bash
./gradlew build
```

Le jar se trouve dans `build/libs/chrome-neon-<version>-neoforge-1.21.1.jar`. `build` lance aussi les tests JUnit.

## Tester

```bash
./gradlew test
```
Tests unitaires de la logique pure (règles d'installation, RAM, données joueur et leur sérialisation).

```bash
./gradlew runGameTestServer
```
Démarre un vrai serveur dédié Minecraft et exécute les GameTests du mod (registrations, bloc, régénération de la RAM
sur le gestionnaire de tick réel, sauvegarde et rechargement des données d'un joueur). Échoue si un test échoue.

```bash
./gradlew runClient
```
Client de développement. En jeu : onglet créatif « Chrome Néon », clic droit avec le Scanner de diagnostic,
`/chromeneon status`, `/chromeneon ram set @s 1` pour voir les cellules de RAM se recharger dans le HUD.

## Assets

Les textures sont générées par `tools/textures.py` (Python 3 + Pillow) avec la palette du HUD.
Le mod n'utilise que des assets originaux : aucun son, texture ou police tirés de Cyberpunk 2077 ou d'autres jeux.
