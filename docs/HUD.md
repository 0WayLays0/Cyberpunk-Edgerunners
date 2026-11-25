# HUD Chrome Néon (phase 2)

## Langage graphique

| Rôle | Couleur | Usage |
|---|---|---|
| Système | violet néon `#B026FF` | cadres, santé, onglets, accents |
| Données | cyan `#1EF2FF` | RAM, informations, valeurs secondaires |
| Essentiel | blanc `#F5F2FF` | valeurs lues en premier (noms, heure) |
| Danger | orange `#FF8A1F` / rouge `#FF2B4E` | alertes, dégâts, hostiles, erreurs |
| Fond | noir profond `#06040C` | panneaux semi-transparents, jamais opaques |

- **Angles coupés asymétriques** partout : en haut à gauche et en bas à droite, sur les panneaux, la mini-carte et
  même dans les deux polices.
- **Santé** en chevrons, **armure** en plaques inclinées, **RAM** en cellules-puces : trois formes distinctes.
- **Typographie originale** (dessinée pour le mod, `tools/hud_assets.py`) : `hud` (5×7, chiffres tabulaires,
  accents français) pour les noms et valeurs, `micro` (3×5) pour les étiquettes techniques. Les textes libres
  longs (corps de notification) gardent la police du jeu pour rester lisibles.
- **42 icônes 9×9 originales**, blanches, teintées au rendu.
- **Hiérarchie** : les données courantes restent discrètes ; seules les alertes deviennent agressives (rouge,
  clignotement, glitch). Le glitch n'est jamais permanent (20 ticks maximum, suivi d'un temps de recharge).
- **Asymétrie** : bloc vitals en haut à gauche avec armure et RAM décalées, navigation en haut à droite, appels
  au milieu à droite, notifications sous les vitals.

Toutes les formes sont de vrais polygones (`client/hud/draw/HudDraw`), pas des empilements de rectangles :
positions flottantes, donc animations fluides au sous-pixel.

## Composants

| Composant | Classe(s) | Ce qu'il fait réellement |
|---|---|---|
| Santé | `vitals/HealthBar`, `VitalsTracker` | chevrons ; perte : bloc blanc qui refroidit en orange et se vide ; soin : balayage cyan ; absorption : chevrons creux ; ≤ 30 % : rouge + battement + étiquette CRITIQUE (hystérésis jusqu'à 36 %) |
| Armure | `vitals/ArmorBar` | plaques acier (2 points chacune), robustesse affichée ; gain : flash cyan + soulèvement ; perte : plaques orange qui s'éteignent |
| RAM | `vitals/RamCells`, `RamTracker` | cellules disponibles / utilisées, recharge visible dans la cellule suivante (progression synchronisée depuis le serveur et extrapolée), flash blanc à la dépense, apparition des nouvelles cellules, RAM insuffisante : cellules manquantes en rouge + secousse |
| Horloge | `nav/ClockDisplay` | heure du monde (06:00 au lever), deux-points battant à la seconde réelle, numéro du jour, ligne jour/nuit avec la position du soleil, glissement de l'heure quand elle change ; « --:-- / NO SYNC » dans le Nether et l'End |
| Statut | `status/StatusGroup`, `Conditions`, `EffectIcons` | états anormaux (feu, gel, oxygène, faim) en puces d'alerte avec libellé et %, effets en icônes propres au mod avec durée qui se vide et niveau ; remplace la liste vanilla |
| Mini-carte | `nav/*` | voir ci-dessous |
| Ciblage / scan | `scan/*` | voir ci-dessous |
| Notifications | `notify/*` | file à priorités, entrée glissée + balayage + titre tapé, ligne de temps, sortie animée, alertes avec rayures et glitch, notifications à progression (forme des futurs téléversements) |
| Appel entrant | `notify/CallGroup`, `CallCenter` | visage du joueur appelant (ou motif généré depuis le nom), canal, onde animée, touches réelles affichées, accepter/refuser/manqué |
| Effets écran | `fx/*` | flash des bords proportionnel au coup, coin de direction de la source autour du viseur, battement des bords en état critique, déchirure d'écran réelle (copie du framebuffer, tranches décalées avec fantômes rouge/cyan), séquence de démarrage |

### Mini-carte : implémentation

Le client connaît les chunks chargés : `TerrainSampler` lit pour chaque colonne la heightmap de surface
(`WORLD_SURFACE`) et la `MapColor` du bloc du dessus, par tranches de 4096 colonnes par tick (une passe complète
en quelques ticks), et suit le joueur en conservant ce qui est déjà connu. Le rendu est stylisé : luminosité du
terrain en violet, eau en cyan selon la profondeur, lave en orange, relief éclairé du nord-ouest, courbes de niveau
tous les 8 blocs. L'image est recomposée chaque frame (un texel par pixel GUI) avec rotation, masque des coins coupés
et lignes de balayage.

Superpositions : joueur et champ de vision, créatures connues du client (hostiles orange, joueurs blancs, autres
cyan, repère de hauteur), point d'apparition du monde, lieu de la dernière mort, marqueurs personnels (touche B,
accroupi + B pour retirer le plus proche, 9 maximum, enregistrés par monde/serveur dans
`config/chromeneon/markers/`), impulsions de scan, repère N.

**Limites réelles** : seuls les chunks chargés par le client sont connus (au-delà de la distance de rendu : zone
« sans données » hachurée, jamais inventée). Dans les dimensions à plafond (Nether), la carte lit le sol autour de
la hauteur du joueur au lieu du toit. Sous terre dans l'Overworld, elle montre la surface. Les créatures
n'apparaissent que si le serveur les envoie au client (distance de suivi).

### Ciblage et scan : implémentation

`TargetTracker` lance chaque tick un vrai rayon depuis les yeux du joueur : créature la plus proche jusqu'à la
portée configurée (les blocs arrêtent le rayon), et, pendant le scan, le bloc visé. Les crochets se posent sur la
boîte englobante réelle de la cible, projetée avec les matrices exactes du rendu de la frame (`WorldProjection`).
Maintenir la touche de scan (V) remplit l'analyse (durée selon la cible), qui se termine par une impulsion sur la
carte et la révélation des données ; une cible analysée est mémorisée une minute.

Les données viennent de `ScanProvider` enregistrés dans `ScanRegistry` (priorité décroissante) :

| Fournisseur | Cibles | Données réelles |
|---|---|---|
| `LivingScanProvider` | créatures, joueurs | classe (hostile/neutre/passif/joueur), santé, armure, agressivité (synchronisée par le jeu), menace selon la nature (boss = 3) |
| `EntityScanProvider` | autres entités | type, contenu des objets au sol, vitesse |
| `DeviceScanProvider` | blocs à état (portes, leviers, lampes, fours…) | ouvert/fermé, alimenté, signal redstone reçu, accès |
| `ContainerScanProvider` | blocs de stockage | capacité |
| `BlockScanProvider` | tout bloc | dureté, outil, résistance aux explosions |

## API pour les phases suivantes

Côté client (`client.hud.ChromeHud`) :

```java
ChromeHud.notify(NotificationKind.CYBERWARE, title, message);
ChromeHud.notify(Notification.builder(NotificationKind.SCAN, title).icon(HudIcon.UPLOAD).progress(() -> upload).build());
ChromeHud.glitch(0.6F, 12);                 // hack, dysfonctionnement
ChromeHud.incomingCall(new IncomingCall(...));
ChromeHud.ramInsufficient(cost);            // hack refusé faute de RAM
ChromeHud.ping(position, radius, color);    // zone scannée, alarme
ChromeHud.scanners().register(provider);    // caméras, machines piratables
```

Côté serveur (`common.hud.HudMessages`) : `HudMessages.notify(player, kind, title, message)` — utilisé dès
maintenant à la connexion, à l'installation/retrait d'implant et par le scanner de diagnostic.

## Configuration (en jeu : Mods > Chrome Néon > Configurer, ou `config/chromeneon-client.toml`)

HUD activé, échelle (0,5–2), opacité, mode minimaliste, intensité des effets (0 = aucun), glitch, séquence de
démarrage ; pour chaque groupe (vitals, statut, navigation, notifications, appel) : affiché, point d'ancrage
(8 positions) et décalage ; mini-carte (activée, horloge, taille, blocs par pixel, rotation, créatures, marqueurs) ;
nombre de notifications simultanées (0 = désactivées) ; crochets et portée du ciblage. Touche H : afficher/masquer
le HUD (enregistré). Toutes les valeurs s'appliquent sans redémarrer.

Quand les vitals sont affichés, les cœurs et l'armure vanilla sont masqués ; quand le statut est affiché, la liste
d'effets vanilla l'est aussi. La faim, l'air, l'expérience, la barre d'action, le chat et la barre de boss restent
ceux du jeu.

## Tester

```bash
./gradlew test
```

```bash
xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClient -Pchromeneon.autotest=true
```

Le second lance un vrai client, crée un monde survie et parcourt tous les états (voir `client/debug/HudAutoTest`) ;
les captures sont dans `run/chromeneon-autotest/`, le rapport de performance et de disposition dans
`run/logs/latest.log` (`[HudAutoTest]`). Sur un ordinateur avec écran, `xvfb-run` est inutile.

En jeu, sans attendre une situation :

| Commande | Effet |
|---|---|
| `/damage @s 7` puis `/damage @s 10` | dégâts réels, puis état critique |
| `/effect give @s minecraft:poison 20` | effet nocif dans le statut |
| `/chromeneon ram set @s 1` | RAM partiellement utilisée, recharge visible |
| `/chromeneon notify @s alert Intrusion détectée` | notification envoyée par le serveur |
| `/cnhud notify <type>` | notification locale de chaque type |
| `/cnhud call [nom]` | appel entrant de test (Y / N) |
| `/cnhud glitch [intensité]`, `/cnhud ram <coût>`, `/cnhud upload`, `/cnhud ping` | glitch, RAM insuffisante, progression, impulsion |

`/cnhud` ne pilote que l'affichage du client : il ne modifie rien sur le serveur.

## Résultats de la validation (phase 2)

Mesuré dans cet environnement : rendu logiciel Mesa llvmpipe sous Xvfb, sans carte graphique.

- `./gradlew build` : compile ; **25 tests unitaires** passent (file de notifications, transformations de la carte,
  palette et relief, masque des coins, horloge, easing, palette de couleurs, plus les 9 tests de la phase 1).
- `./gradlew runGameTestServer` : **5 GameTests** passent sur la cible serveur dédié (`forgeserverdev`), dont l'envoi
  de notifications HUD à un joueur sans le canal du mod.
- Test automatisé du client (`-Pchromeneon.autotest=true`) : **60 étapes passées** — vrai monde survie, vrais
  dégâts venant d'un squelette sur le côté, santé critique, scans d'une créature et d'un bloc, notification envoyée
  par le serveur, appel accepté au clavier, glitch, RAM insuffisante, nuit, mode minimaliste, carte nord en haut et
  zoomée, puis 7 combinaisons de fenêtre et d'échelle (1280x720 GUI 1, 2 et 3 ; 1920x1080 GUI 2 et 4 ; 960x540 GUI 2 ;
  1920x1080 GUI 2 avec HUD à 1,5) : aucun groupe ne sort de l'écran, ne chevauche un autre groupe, ni ne couvre la
  barre d'action, la barre d'expérience, la faim et l'air, ou le viseur. Seule la bande des barres de boss est
  atteinte par la navigation (en haut à droite) : signalé, sans incidence hors combat de boss.
- Performance (temps CPU du HUD par frame, rendu logiciel, donc pessimiste) : environ 4,6 à 5,7 ms au total selon les passes, dont
  ~1,6 ms pour la mini-carte (recomposition de l'image chaque frame) ; échantillonnage du terrain 0,5 à 1 ms par tick.
  Sur une vraie carte graphique ces chiffres n'ont pas été mesurés.
- Le premier essai a mis en évidence et fait corriger : une copie d'écran du glitch qui lisait une texture vide
  (remplacée par une copie directe du framebuffer, vérifiée par une capture de la copie), des chevauchements
  (titre et code des notifications, appel et horloge, statut et notifications), l'échelle des décalages avec le HUD.

Les captures de `docs/screenshots/` viennent de ce test (aucune n'est une maquette).

## Limites connues

- Les toasts vanilla (succès, recettes, tutoriel) s'affichent en haut à droite, par-dessus la mini-carte, le temps
  de leur apparition.
- La position de chaque groupe est réglable, mais pas leur disposition interne.
- Dans les très petites interfaces (240 pixels GUI de haut), deux notifications au plus sont affichées à la fois
  pour ne pas atteindre la barre d'expérience.
