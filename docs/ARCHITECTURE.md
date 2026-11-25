# Architecture de Chrome Néon

Package racine : `com.waylays.chromeneon`.

```
ChromeNeon                 point d'entrée commun : registres, configs, réseau (rien d'autre)
common/
  registry/                un DeferredRegister par type : ModItems, ModBlocks, ModBlockEntities, ModMenus,
                           ModAttachments, ModCreativeTabs, ModImplants (registre custom chromeneon:implant)
  config/                  ServerConfig (équilibrage, par monde, synchronisé), ClientConfig (affichage)
  cyberware/               BodySlot, Implant, CyberwareLoadout (règles d'installation), InstallResult
  hacking/                 RamPool (ressource des hacks)
  player/                  PlayerCyberData (données persistantes), CyberStats (valeurs dérivées),
                           CyberData (accès serveur + sync), CyberReport, SyncedCyberState (miroir côté client)
  network/                 ChromeNetwork (enregistrement, version de protocole), payloads
  hud/                     HudMessages (API serveur des notifications), NotificationKind, HudNotifyPayload,
                           ClientHudBridge (point de branchement du client, sans classe client)
  item/, block/            classes d'items et de blocs avec comportement
server/                    événements serveur (tick RAM, sync) et commandes
client/                    point d'entrée @Mod(dist = CLIENT)
  hud/                     ChromeHud (API), HudEvents, HudLayers, HudGroup (groupe positionné), HudFrame, HudTime
    theme/ draw/ anim/     palette, polices, icônes ; polygones et primitives ; easing et lissage
    vitals/ status/        santé, armure, RAM ; effets et états anormaux
    nav/                   mini-carte (échantillonnage du terrain, rendu, marqueurs, impulsions), horloge
    scan/                  ciblage, projection monde→écran, fournisseurs de scan, superposition
    notify/                file de notifications, cartes, appel entrant
    fx/                    glitch, déchirure d'écran, dégâts et santé critique, séquence de démarrage
  input/ command/ debug/   touches, /cnhud, test automatisé du client et mesures
gametest/                  GameTests exécutés sur un serveur réel
src/test/                  tests JUnit de la logique pure
tools/                     générateurs d'assets
```

## Principes

- **Séparation des côtés.** Tout ce qui importe `net.minecraft.client` est dans `client/`, chargé uniquement par
  `ChromeNeonClient` (`@Mod(dist = Dist.CLIENT)`). Le handler du payload de sync, enregistré dans le code commun,
  n'écrit que dans `SyncedCyberState`, qui n'utilise aucune classe client. Vérifié : le mod démarre sur la cible
  serveur dédié (`forgeserverdev`).
- **Le serveur fait autorité.** Le client ne reçoit que l'état calculé ; aucune décision de gameplay côté client.
  Les futurs paquets client → serveur (activer un hack, installer un implant) devront être validés côté serveur.
- **Logique testable sans jeu.** `CyberwareLoadout`, `RamPool` et `CyberStats` reçoivent des nombres et une fonction
  de recherche d'implant, jamais la config ou le registre directement. `CyberData` fait le lien avec le jeu.
- **Rien de dérivé n'est stocké.** L'humanité et la capacité de RAM se calculent à partir des implants et de la config ;
  elles ne peuvent pas se désynchroniser.
- **Sauvegardes évolutives.** Chaque champ du codec a une valeur par défaut (`optionalFieldOf`) : ajouter un champ
  n'invalide pas les anciennes sauvegardes. Un implant dont le mod a disparu est conservé mais ignoré.
- **Réseau versionné.** Changer le format d'un payload impose d'incrémenter `ChromeNetwork.PROTOCOL_VERSION`.
  Les envois sont ignorés pour les connexions sans le canal du mod (faux joueurs).

## Points d'extension prévus

| Phase | Ajout | Où |
|---|---|---|
| 2 | HUD | fait : voir docs/HUD.md |
| 3 | Implants concrets | sous-classes d'`Implant` enregistrées dans `ModImplants.IMPLANTS`, logique par emplacement dans `common/cyberware/<emplacement>/` |
| 3 | Installation au fauteuil | block entity dans `ModBlockEntities`, menu dans `ModMenus`, paquet C→S validé par `CyberwareLoadout.install` |
| 4 | Hacks | registre `chromeneon:quickhack`, coût en RAM via `RamPool.tryConsume`, cibles via une capability/interface côté serveur |
| 5–6 | Caméra, VESPER | packages `common/camera`, `common/vesper` et leurs pendants `client/` |
