# Feuille de route

Une phase n'est commencée que lorsque la précédente compile, passe ses tests et a été validée en jeu.

1. **Fondations** — terminée. Voir le README pour le détail de ce qui fonctionne.
2. **HUD Chrome Néon** — santé, armure, RAM, mini-carte, heure, scan, appels, alertes, glitch d'événement.
   Langage graphique : angles coupés, barres segmentées à chevrons, RAM en cellules, crochets de scan, glitch réservé
   aux événements, hiérarchie discret/agressif, entièrement configurable. Planches de référence fournies (violet/cyan,
   cadres asymétriques, jauges circulaires, chevrons).
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
- Aucun hack : seule la ressource RAM existe (régénération, consommation testée).
- Le HUD ne contient que les cellules de RAM ; l'option `glitchEffects` n'est encore lue par aucun composant.
- Aucun son original pour le moment (le scanner utilise un son vanilla).
- Pas de recette d'artisanat : les objets sont accessibles en créatif et par commande.
