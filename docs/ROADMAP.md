# Feuille de route

Une phase n'est commencée que lorsque la précédente compile, passe ses tests et a été validée en jeu.

1. **Fondations** — terminée. Voir le README pour le détail de ce qui fonctionne.
2. **HUD Chrome Néon** — terminée. Voir [HUD.md](HUD.md) et les [captures](screenshots/).
3. **Cyberware** — terminée. Voir [CYBERWARE.md](CYBERWARE.md) et les [captures](screenshots/phase3/).
4. **Hacking** — mode scanner, niveaux d'accès, file d'upload, hacks sur entités, caméras, portes et machines,
   résistance/sécurité, cooldowns, coûts en RAM.
5. **Caméra numérique** — appareil physique, filtre basse résolution, zoom, batterie, REC, timecode, captures.
6. **VESPER** — IA de biopuce, hologramme, dialogues, personnalité, capacités intégrées aux mécaniques.
7. **Polish** — sons originaux, shaders, animations, optimisation, compatibilité, captures d'écran.

## Pas encore développé (honnêtement)

- Hacking (phase 4) : la RAM est une vraie ressource (dépensée par l'accélérateur et l'analyse tactique), mais
  aucun quickhack n'existe encore.
- Le scanner (V) fonctionne avec les optiques ; le mode scanner qui transforme visuellement le monde arrive en phase 4.
- Biopuce : seul le Coprocesseur Synapse l'occupe ; VESPER arrive en phase 6.
- Appels : le popup fonctionne, aucun système d'appel ne l'utilise encore (testé par `/cnhud call`).
- Mini-carte : limitée aux chunks chargés par le client (voir [HUD.md](HUD.md)).

## Limites connues de la phase 3

- Le ralentissement du Chronos s'applique aux entités. Les blocs, fluides, plantes et la redstone ne sont pas
  ralentis, et les autres joueurs non plus (un serveur ne peut pas ralentir un client).
- Le double saut est appliqué par le client, car Minecraft laisse le client calculer le mouvement du joueur. Le
  serveur valide les charges et l'état, et remet la chute à zéro, mais ne recalcule pas la trajectoire.
- Les lames n'ont pas de modèle dédié dans l'inventaire (elles sont rendues sur le bras quand elles sont sorties).
- Les images rémanentes sont dessinées sur le modèle du joueur, sans armure ni objets tenus.
