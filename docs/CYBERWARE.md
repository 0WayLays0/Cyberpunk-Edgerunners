# Cyberware (phase 3)

Tout ce qui est décrit ici est exécuté par le serveur. Le client affiche et demande, il ne décide de rien :
implants, ressources, protection et capacités ne peuvent pas être obtenus en envoyant un paquet.

## Architecture

| Élément | Rôle |
|---|---|
| `chromeneon:body_slot` (registre synchronisé) | Les emplacements du corps : nombre de sockets et région du schéma. D'autres mods peuvent en ajouter. |
| `chromeneon:implant` (registre synchronisé) | Les implants. Chaque implant est une classe (`Implant`) avec des `Properties` (emplacements, coût en humanité, coût en capacité, RAM ajoutée, mode, exemplaires max., groupe exclusif) et des hooks serveur (`onInstalled`, `tick`, `use`, `onStopped`, `onRemoved`, `cooldownFactor`). |
| `ImplantState` | L'état de chaque implant installé : allumé, actif, fin d'activité, fin de recharge, charges, jauge, panne, dernier changement. Sauvegardé avec le joueur. |
| `CyberwareLoadout` | Les implants installés par emplacement, et les règles d'installation (`check`). |
| `CyberwareService` | Seule porte d'entrée des changements : installation, retrait, allumage, utilisation. |
| `Cyberware` | La lecture côté serveur pour les autres systèmes (`of`, `ofType`, `working`) : combat, mouvement, et plus tard hacking et VESPER. |
| `CyberwareEvent` | Événements NeoForge : `Installed`, `Removed`, `Used`, `Malfunction`, `Cooldown` (modifiable). |
| `Cooldowns` | Durée de recharge finale : facteurs des implants installés, puis l'événement `Cooldown` pour les futurs perks et les autres mods. |
| `CyberMath` | Les calculs purs (absorption, injection, pannes, sauts, dilatation, recharges, critiques), testés sans le jeu. |
| Réseau | `SyncCyberStatePayload` (état complet au joueur), `ImplantVisualsPayload` (lames et dilatation visibles par les autres), `CyberwareRequestPayload` et `ImplantTriggerPayload` (demandes du client, limitées à 8 par tick, vérifiées). Tout envoi passe par `ChromeNetwork.send`, qui ignore les connexions sans le canal du mod. |

## Le corps

| Emplacement | Sockets | Implants |
|---|---|---|
| Système (`system`) | 1 | système d'exploitation : Accélérateur Chronos Mk.I / Mk.II / Mk.III (plus tard le cyberdeck) |
| Optiques (`optics`) | 1 | Optiques Argus |
| Biopuce (`biochip`) | 1 | Coprocesseur Synapse (plus tard VESPER) |
| Bras gauche / droit | 1 + 1 | Lames Faucheuse (une paire par bras) |
| Jambes (`legs`) | 1 | Tendons Ressort |
| Système dermique | 2 | Derme Carapace |
| Système circulatoire | 2 | Pompe Hémostat |

Règles, vérifiées dans cet ordre : implant connu, emplacement connu, bon emplacement, nombre d'exemplaires
(`ALREADY_INSTALLED`), socket libre, groupe exclusif (`CONFLICT`, un seul système d'exploitation), humanité, capacité.

## Humanité et capacité

- **Humanité** : 100 au départ (configurable). Chaque implant en coûte une part, recalculée à partir des implants
  installés (jamais stockée, donc jamais désynchronisée). Une installation qui la ferait passer sous zéro est refusée.
- **Tension** : sous 25 % d'humanité, un implant peut tomber en panne pour 10 s. La chance est nulle au seuil et
  monte jusqu'à 50 % par minute à 0. Une panne coupe l'implant (lames rentrées, accélérateur arrêté) puis il revient. Tout est configurable, et les pannes peuvent être désactivées.
- **Capacité cyberware** : 24 de base. Une **Matrice neurale** (objet consommable) en ajoute 2, jusqu'à +16.

## Le fauteuil de charcudoc

Un bloc orientable, avec un modèle 3D original (dossier incliné, accoudoirs, bras chirurgical, bandes lumineuses
émissives) et des anneaux holographiques animés. Clic droit : le joueur s'assoit (entité siège) et l'interface de
chirurgie s'ouvre. Installer ou retirer exige d'être assis dans un fauteuil et de ne pas avoir été blessé depuis
5 s, deux points vérifiés par le serveur. L'implant installé est pris dans l'inventaire, l'implant retiré est rendu.
Un fauteuil n'accueille qu'une personne. Accroupi pour se lever.

## Interfaces

- **Cyberware (K)**, accessible partout : schéma du corps avec chaque emplacement relié par une ligne à son contenu,
  jauges d'humanité et de capacité, fiche technique de l'implant choisi (mécanique et chiffres tirés de la config
  du serveur), état (en ligne, actif, recharge, panne, éteint) et bouton pour l'allumer ou l'éteindre. On n'y installe rien.
- **Chirurgie** (au fauteuil) : la même vue, plus les implants transportés. Choisir un implant met en évidence ses
  emplacements, propose un socket libre et affiche le verdict exact du serveur avant de l'installer (coûts compris).
- **HUD** : en bas à droite, un cadran par implant utilisable : anneau qui se vide pendant l'effet et se remplit
  pendant la recharge, charges en losanges, jauge du blindage, touche associée, rouge en cas de panne.

## Les implants

| Implant | Emplacement | Humanité | Capacité | Mode |
|---|---|---|---|---|
| Accélérateur Chronos Mk.I | Système | 18 | 6 | déclenché |
| Accélérateur Chronos Mk.II | Système | 25 | 8 | déclenché |
| Accélérateur Chronos Mk.III | Système | 35 | 12 | déclenché |
| Lames Faucheuse | Bras G / D | 12 | 6 | activable (×2) |
| Tendons Ressort | Jambes | 10 | 5 | automatique |
| Derme Carapace | Dermique | 15 | 6 | passif |
| Pompe Hémostat | Circulatoire | 10 | 5 | automatique |
| Optiques Argus | Optiques | 8 | 4 | activable |
| Coprocesseur Synapse | Biopuce | 8 | 4 | passif, +2 RAM |

### Accélérateur Chronos : le système d'exploitation de type Sandevistan

Notre équivalent original du Sandevistan, sous un nom propre au mod. Il occupe l'emplacement Système, et un seul
système d'exploitation peut être installé : le futur cyberdeck de la phase 4 prendra le même emplacement, donc
pas de quickhacks avec un Chronos.

- **Ralentissement réel** (touche Z, coûte 2 RAM) : dans un rayon de 24 blocs, toutes les autres entités
  (créatures, projectiles, objets, véhicules) ne vivent qu'un tick sur N. Elles se déplacent, attaquent, tombent
  et volent N fois moins vite pendant que le joueur agit à pleine vitesse. Les projectiles du joueur gardent
  leur vitesse. Les clients ralentissent les mêmes entités au même rythme, donc les animations suivent.
- **Arrêt manuel** : rappuyer sur Z arrête l'effet avant la fin. La recharge part de l'arrêt.
- **Coups critiques** : pendant l'effet, chaque coup du joueur (mêlée ou ses projectiles) a une chance d'être
  critique. Le serveur tire au sort, multiplie les dégâts, puis affiche des particules et joue un son.
- **Recharge réductible** : le Coprocesseur Synapse réduit de 15 % les recharges du système d'exploitation.
  L'événement `CyberwareEvent.Cooldown` permettra à des perks ou d'autres implants de la réduire aussi.
- **Images rémanentes** : pendant l'effet, des copies holographiques du joueur (violet vers cyan, à lignes de
  balayage) restent derrière lui et s'effacent en un peu plus d'une demi-seconde. Elles sont visibles par tous
  les joueurs proches. Bords d'écran colorés et compte à rebours pour l'utilisateur.

| Modèle | Ralentissement | Durée | Recharge | Critique | Dégâts critiques |
|---|---|---|---|---|---|
| Mk.I | 1 tick sur 3 | 5 s | 30 s | 10 % | +50 % |
| Mk.II | 1 tick sur 4 | 6 s | 25 s | 20 % | +50 % |
| Mk.III | 1 tick sur 6 | 8 s | 35 s | 15 % | +100 % |

Recettes : le Mk.I se fabrique, le Mk.II s'obtient en améliorant un Mk.I, et le Mk.III en améliorant un Mk.II.

### Lames Faucheuse

Touche R : les lames sortent des avant-bras (modèle et animation en première et troisième personne, sons). Lames
sorties, main vide : +5 dégâts d'attaque par paire et attaque plus rapide (modificateurs d'attribut transitoires),
son de lame à l'impact, +30 % pendant le ralentissement. **Fente** : en sprintant, frapper dans le vide propulse le
joueur. Chaque créature traversée prend 8 dégâts une seule fois. Si les optiques sont en ligne, la fente se guide
vers la créature ciblée (12 blocs, ligne de vue vérifiée par le serveur). Recharge de 2,5 s, divisée par deux
pendant le ralentissement.

### Tendons Ressort

Un saut supplémentaire en l'air (deux pendant le ralentissement), rechargé à l'atterrissage. Le mouvement du
joueur est calculé par son client dans Minecraft : le client applique l'impulsion, le serveur vérifie les charges
et l'état (en l'air, ni en vol, ni dans un liquide, ni sur une monture), décompte la charge et remet la chute à zéro.

### Derme Carapace

Encaisse 40 % de chaque coup physique (après l'armure vanilla), dans la limite de 20 points d'intégrité. À zéro,
il est percé et ne protège plus rien jusqu'à ce qu'il ait réparé un quart de sa jauge. Il se répare après 4 s sans
dégâts. Le feu, les chutes, la noyade, le gel, la foudre et la magie passent à travers.

### Pompe Hémostat

Sous 30 % de vie, après un coup : injecte 8 PV (moitié aussitôt, moitié en 2 s). 2 charges, rechargées une par
minute, 10 s entre deux injections. C'est un système d'urgence, pas une régénération.

### Optiques Argus

En ligne par défaut (activables dans l'interface). Elles sont nécessaires au ciblage et au scanner du HUD : sans
optiques, la touche V signale « Optiques requises ». Zoom jusqu'à ×4 (maintenir G, molette). **Analyse tactique**
(U, 1 RAM) : marque à travers les murs toutes les créatures à 40 blocs pendant 15 s. Elle est gratuite pendant le
ralentissement.

### Coprocesseur Synapse

+2 cellules de RAM, recharge de la RAM 30 % plus rapide, recharge du système d'exploitation −15 %.

## Interactions

- Chronos → Lames : +30 % de dégâts et fente rechargée deux fois plus vite pendant le ralentissement.
- Chronos → Tendons : un saut aérien de plus.
- Chronos → Optiques : analyse tactique gratuite.
- Optiques → Lames : fente guidée sur la cible.
- Coprocesseur → Chronos : recharge réduite ; → toute la RAM : plus de cellules, recharge plus rapide.
- Humanité basse → toutes : pannes temporaires.

## Commandes (opérateurs)

`/chromeneon implant install <joueur> <implant> <emplacement>` · `/chromeneon implant remove <joueur> <emplacement> [index]`
· `/chromeneon capacity <joueur> <bonus>` · `/chromeneon ram set <joueur> <cellules>` · `/chromeneon status`.
Ces commandes respectent les règles du corps, mais pas l'obligation du fauteuil.

## Configuration

Serveur (`serverconfig/chromeneon-server.toml`, synchronisée aux clients), section par section : `cyberware`,
`humanity`, `ram`, `chronos_accelerator` (rayon, RAM, puis `mk1`, `mk2`, `mk3` : durée, recharge, ralentissement,
chance et dégâts critiques), `reaper_blades`, `spring_tendons`, `carapace_dermis`, `hemostat_pump`, `argus_optics`,
`synapse_coprocessor`. Client : position et échelle des cadrans d'implants (`implants`).

## Tests réellement exécutés

- **JUnit (`./gradlew test`)** : 46 tests au total. Pour la phase 3, ils couvrent les règles d'installation avec un
  catalogue de test (emplacement, sockets, exemplaires par corps, système d'exploitation unique, conflits de
  groupe, humanité et capacité aux limites, retrait, RAM des implants en panne ou éteints, implants inconnus), les
  états, la sérialisation complète, les statistiques du joueur et leur passage sur le réseau, ainsi que
  `CyberMath` (absorption, injection, pannes, sauts aériens, un tick sur N, recharges, critiques).
- **GameTests sur serveur dédié (`./gradlew runGameTestServer`)** : 17 sur 17 réussis. La chirurgie (refus debout,
  en combat, au mauvais emplacement, sans objet ; installation qui consomme l'objet ; retrait qui le rend), le vrai
  fauteuil (le joueur s'y assoit, puis la requête réseau est acceptée), le système d'exploitation unique, la
  sauvegarde et le rechargement d'un joueur avec ses implants et leurs états, et des mesures sur le jeu
  lui-même : le derme réduit de 40 % un vrai coup mais pas le feu, la pompe rend 4 PV puis 4 de plus en 2 s, les
  lames ajoutent +10 d'attaque avec deux paires puis 0 avec un objet en main, un projectile proche ne parcourt
  qu'environ un quart de sa distance pendant le Mk.II, le coprocesseur ramène la recharge de 500 à 425 ticks, les
  coups deviennent critiques (×1,5) pendant l'effet, les sauts aériens sont refusés au sol et après épuisement des
  charges puis rechargés à l'atterrissage, une panne rentre les lames et bloque leur usage.
- **Client automatisé** (`xvfb-run … runClient -Pchromeneon.autotest=true`) : réussi, 106 étapes. Le joueur
  s'assoit dans un fauteuil posé dans le monde, installe 5 implants par l'interface de chirurgie (6 implants
  vérifiés côté client après la synchronisation du serveur), ouvre l'interface Cyberware, sort les lames (vérifié),
  lance l'accélérateur en se déplaçant (dilatation vérifiée, puis arrêt manuel vérifié, avec une recharge de 419 ticks
  restants grâce au coprocesseur), utilise le zoom et l'analyse tactique (vérifiée). Les captures sont dans
  [screenshots/phase3](screenshots/phase3/).

Non testé automatiquement : le rendu avec plusieurs joueurs connectés (images rémanentes et lames vues par un
autre client). Le code passe par le même paquet que pour le joueur lui-même, mais aucune partie à deux clients
n'a été lancée.
