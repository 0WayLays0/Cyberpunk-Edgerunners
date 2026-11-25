# Feuille de route

Une phase n'est commencée que lorsque la précédente compile, passe ses tests et a été validée en jeu.

1. **Fondations** — terminée. Voir le README pour le détail de ce qui fonctionne.
2. **HUD Chrome Néon** — terminée. Voir [HUD.md](HUD.md) et les [captures](screenshots/).
3. **Cyberware** — fauteuil de charcudoc (installation/retrait), interface de gestion, jauge d'humanité, implants
   fonctionnels par emplacement : accélérateur temporel (Système), optiques (scanner, zoom), lames (Bras),
   double saut (Jambes), peau blindée (Dermique), pompe médicale (Circulatoire), biopuce (VESPER). Interactions entre
   implants, activation/désactivation, sons originaux.
4. **Hacking** — mode scanner, niveaux d'accès, file d'upload, hacks sur entités, caméras, portes et machines,
   résistance/sécurité, cooldowns, coûts en RAM.
5. **Caméra numérique** — appareil physique, filtre basse résolution, zoom, batterie, REC, timecode, captures.
6. **VESPER** — IA de biopuce, hologramme, dialogues, personnalité, capacités intégrées aux mécaniques.
7. **Polish** — sons originaux, shaders, animations, optimisation, compatibilité, captures d'écran.

## Pas encore développé (honnêtement)

- Aucun implant n'est enregistré ; les règles et les données sont prêtes, les implants arrivent en phase 3.
- Le fauteuil de charcudoc est un bloc orientable sans interaction.
- Hacking : seule la ressource RAM existe (régénération, consommation testée) et son affichage complet (cellules,
  recharge, RAM insuffisante). Aucun hack ne la consomme encore ; l'affichage « RAM insuffisante » est appelé par
  `/cnhud ram` en attendant.
- Scanner : il fonctionne pour tous (touche V) ; il sera lié aux optiques en phase 3/4. Le mode scanner qui
  transforme visuellement le monde (filtre, surbrillance des cibles à travers le décor) est prévu en phase 4.
- Appels : le popup, ses touches et ses issues fonctionnent, mais aucun système d'appel ne l'utilise encore
  (testé par `/cnhud call`).
- Sons : aucun son original pour le moment (le scanner de diagnostic utilise un son vanilla). Prévu en phase 7.
- Mini-carte : limitée aux chunks chargés par le client (voir [HUD.md](HUD.md)).
- Pas de recette d'artisanat : les objets sont accessibles en créatif et par commande.
