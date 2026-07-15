# Herbalis

Plugin de culture et de consommation de substances pour serveur roleplay
Purpur/Paper 1.21.11, avec son resource pack. Scope v1 : la weed, sur une
architecture multi-drogue (ajouter une drogue = un fichier de config et
des assets, zéro refonte).

Le plugin mise tout sur l'immersion : plantes sculptées en volumes 3D
(vraies feuilles de cannabis dentelées à 5-7 folioles, branches
latérales étagées aux stages avancés, colas à pistils) qui grandissent
en continu, se balancent et respirent doucement, chaque plant avec son
orientation et sa taille propres. Le terreau raconte le soin (humide,
sec et craquelé, fertilisé), les buds givrent de trichomes et
scintillent en fenêtre de récolte optimale, une feuille se détache
parfois des plants matures, des gouttes perlent après l'arrosage.
Arrosoir et joint en vrais models 3D en main, particules et sons sur
chaque action (fumée de joint en spirale), HUD en action bar avec une
font d'icônes dessinée pour le pack, effets de consommation
cinématiques (montée, plateau, descente, blackout), tolérance et
manque persistants.

## Installation

1. Compiler le plugin et le resource pack :

   ```bash
   ./gradlew build packResourcePack
   ```

   Résultats :
   - `build/libs/Herbalis-1.0.0.jar`
   - `build/resourcepack/Herbalis-ResourcePack.zip`
   - `build/resourcepack/Herbalis-ResourcePack.sha1`

2. Déposer le jar dans `plugins/` du serveur Purpur 1.21.11.

3. Héberger le zip du resource pack (HTTP) et le déclarer dans
   `server.properties` :

   ```properties
   resource-pack=https://votre-domaine/Herbalis-ResourcePack.zip
   resource-pack-sha1=<contenu du fichier .sha1>
   require-resource-pack=true
   ```

4. Redémarrer. Le plugin crée `plugins/Herbalis/` avec `config.yml`,
   `messages.yml`, `drugs/weed.yml` et la base SQLite `herbalis.db`.

Aucune dépendance externe : pas d'ItemsAdder, Oraxen ou Nexo. Le driver
SQLite est déclaré dans `plugin.yml` (section `libraries`) et téléchargé
par le serveur au premier démarrage.

## Cycle de jeu : de la graine au joint

1. **Poser un pot de culture** : clic droit avec le pot sur une surface
   solide. Le pot est une entité 3D, pas un bloc vanilla détourné.
2. **Planter une graine** : clic droit sur le pot avec une graine de
   weed. La pousse apparaît avec une petite animation de scale.
3. **Entretenir** :
   - **Lumière** : niveau 12 minimum (configurable), sinon la
     croissance est figée et le HUD l'indique. Une serre éclairée
     pousse la nuit.
   - **Arrosage** : l'hydratation baisse avec le temps. Arrosoir en
     main, clic droit sur la plante. L'arrosoir a 8 charges et se
     recharge d'un clic droit sur un bloc d'eau. Une plante à sec
     arrête de pousser, jaunit, puis meurt (le pot reste).
   - **Engrais** : un par stage, accélère le stage en cours et
     améliore la qualité potentielle.
4. **Croissance** : 4 stages (pousse, jeune plant, plant mature, plant
   en fleur), 8 minutes par stage par défaut, transitions animées par
   interpolation. Regarder la plante affiche son état complet en action
   bar : stage en segments, hydratation, qualité potentielle en étoiles,
   alertes.
5. **Récolter** : au stade final, clic droit main vide. Une fenêtre
   optimale de 10 minutes s'ouvre à la floraison : les buds givrent de
   trichomes blancs et la plante scintille. Récolter dedans maximise
   la qualité, après elle décline. La qualité (1 à 5 étoiles)
   combine hydratation moyenne, engrais et timing de récolte.
6. **Sécher** : poser un rack de séchage, y suspendre jusqu'à 6 têtes
   fraîches (clic droit). 20 minutes en temps réel, le séchage continue
   serveur éteint. Le modèle du rack change selon son état et des
   particules discrètes signalent de loin qu'il est prêt. Retirer trop
   tôt (sneak + clic droit) coûte de la qualité.
7. **Conditionner** : pochon vide en main, clic droit : la meilleure
   weed séchée de l'inventaire est emballée, qualité héritée.
8. **Rouler** : pochon + feuille à rouler dans une grille de craft
   (établi ou inventaire) = joint, qualité héritée.
9. **Fumer** : maintenir clic droit. Braise à l'allumage, fumée visible
   par tous les joueurs, montée en 15 secondes par paliers avec
   messages d'ambiance, plateau avec buffs (durée et intensité selon la
   qualité : 2 minutes à 1 étoile, 6 minutes à 5 étoiles), descente
   systématique (lenteur, faim, courte nausée).

### Abus, tolérance, manque

- **Blackout** : 3 joints en moins de 5 minutes : écran noir, joueur
  cloué au sol 30 secondes, caméra qui tangue, réveil vaseux.
- **Tolérance** : monte à chaque joint, décroît en temps réel (même
  hors ligne). Haute tolérance = effets plus courts et plus faibles.
- **Addiction** : consommer régulièrement rend addict. Sans dose,
  symptômes périodiques (tremblements, nausée brève, battements de
  coeur, messages d'ambiance). Tenir assez longtemps fait retomber
  l'addiction.

## Items

| Item | Obtention | Usage |
| --- | --- | --- |
| Pot de culture | `/herbalis give` | Se pose au sol, socle de la plante |
| Graine de weed | Give, ou drop en cassant une plante | Clic droit sur un pot |
| Arrosoir | Give | Arrose (8 charges), se recharge sur l'eau |
| Engrais naturel | Give | Un par stage, boost vitesse et qualité |
| Tête fraîche | Récolte | Se suspend au rack de séchage |
| Rack de séchage | Give | Sèche jusqu'à 6 têtes |
| Weed séchée | Rack | Se conditionne en pochon |
| Pochon vide | Give | Clic droit pour emballer la weed séchée |
| Pochon de weed | Conditionnement | Ingrédient du joint |
| Feuille à rouler | Give | Ingrédient du joint |
| Joint | Craft pochon + feuille | Maintenir clic droit pour fumer |

Casser une plante (clic gauche) rend la graine (configurable). Casser
un pot ou un rack (clic gauche) rend l'item ; un rack plein rend
d'abord ses têtes, fraîches.

## Commandes

| Commande | Permission | Description |
| --- | --- | --- |
| `/herbalis give <joueur> <item> [quantité] [qualité]` | `herbalis.admin` | Donne un item Herbalis (qualité 1 à 5) |
| `/herbalis info` | `herbalis.info` | Détails de la plante ou du rack regardé |
| `/herbalis tolerance <joueur> [reset]` | `herbalis.admin` | Consulte ou remet à zéro tolérance et addiction |
| `/herbalis reload` | `herbalis.admin` | Recharge config, messages et drogues |

Tab completion complète sur tout. Alias : `/herb`.

Items pour `give` : `pot`, `drying_rack`, `watering_can`, `fertilizer`,
`rolling_paper`, `pouch_empty`, `weed_seed`, `weed_bud_fresh`,
`weed_dried`, `weed_pouch`, `weed_joint`.

## Permissions

| Permission | Défaut | Portée |
| --- | --- | --- |
| `herbalis.plant` | tous | Poser pots et racks, planter, entretenir, casser |
| `herbalis.harvest` | tous | Récolter, sécher, conditionner |
| `herbalis.consume` | tous | Fumer |
| `herbalis.info` | op | `/herbalis info` |
| `herbalis.admin` | op | Toutes les commandes d'administration |

## Configuration

- `config.yml` : tick de croissance, autosave, particules et sons,
  HUD, charges de l'arrosoir, drops, explosions, cadence du manque.
  Tout est commenté en français.
- `drugs/weed.yml` : la définition complète de la weed : durées de
  stages, lumière minimum, hydratation, engrais, fenêtre de récolte,
  séchage, effets (montée, plateau, descente), blackout, tolérance,
  addiction, poids du calcul de qualité.
- `messages.yml` : 100 % des textes joueur, en MiniMessage.

### Ajouter une drogue

1. Dupliquer `drugs/weed.yml` en `drugs/<id>.yml` et ajuster.
2. Ajouter les assets au resource pack : `<id>_seed`, `<id>_bud_fresh`,
   `<id>_dried`, `<id>_pouch`, `<id>_joint` (items), et les models
   `plant_<id>_stage_1` à `4`, variantes `_dry` (stages 2 à 4), la
   variante `_prime` du stade final (buds givrés, fenêtre optimale) et
   `plant_<id>_dead`.
3. `/herbalis reload`. Aucun code à toucher.

Note v1 : la tolérance et l'addiction sont un profil unique par joueur,
partagé entre drogues.

## Architecture

```
domain/          Métier pur, zéro import Bukkit, testé unitairement :
                 Plant, GrowthEngine, Quality, DryingRack, DrugType,
                 ConsumptionEngine (tolérance, blackout, manque)
application/     Cas d'usage (PlantSeed, Water, Harvest, Consume...)
                 et ports (repositories, environnement)
infrastructure/  Bukkit : rendu Item Display + Interaction, SQLite
                 (écriture asynchrone, write-behind), tickers globaux,
                 listeners, commandes, FX, HUD
```

Choix techniques notables :

- **Pas de bloc vanilla détourné** : pots, plantes et racks sont des
  Item Display + Interaction. Aucun barrel ni note block sacrifié, et
  les emplacements sont protégés (blocs, eau, pistons, explosions).
- **Entités jetables, base souveraine** : les Display sont non
  persistantes, respawnées au chargement des chunks depuis SQLite, les
  orphelines sont purgées. Un crash ne laisse aucun fantôme.
- **Un scheduler global par préoccupation** (croissance, racks,
  joueurs, HUD, autosave), jamais une task par plante.
- **Timestamps, pas des ticks comptés** : séchage, tolérance, addiction
  et sessions d'effets survivent aux redémarrages et aux déconnexions.
- **Composants 1.21.x** : `item_model` (pas de custom_model_data
  legacy), `consumable` pour l'animation de fumage, `max_damage` pour
  la jauge de l'arrosoir.

## Resource pack

Structure dans `resourcepack/`, format 75 (1.21.11).

- Plantes sculptées en éléments : tige en volume, feuilles de cannabis
  dentelées à 5-7 folioles (deux silhouettes alternées, nervure claire,
  atlas 64x), rosettes au sol et, aux stages 3-4, branches latérales
  inclinées asymétriques portant bouquets de feuilles et colas à
  pistils, cola apical segmenté au stade final, variante givrée de
  trichomes pendant la fenêtre de récolte optimale. Pot conique par
  étages
  avec trois terreaux (humide, sec, fertilisé), rack avec bouquets
  suspendus en volume qui se resserrent en séchant. Textures 32x pour
  les blocs, 16x pour les items (cohérence vanilla en inventaire).
- Arrosoir et joint : models 3D en main et au sol, sprite 2D en
  inventaire (select sur le contexte d'affichage, 1.21.4+).
- Font d'icônes `herbalis:icons` (feuille, goutte, étoiles, segments,
  soleil, ciseaux, sablier, coche...) : glyphes blancs teintés par les
  balises de couleur MiniMessage, utilisés partout dans messages.yml.
- Les textures sont générées par `resourcepack/tools/generate_textures.py`
  (Pillow) et les models par `generate_models.py` ; les chemins et les
  régions UV sont stables pour permettre à un artiste de remplacer les
  PNG sans toucher aux models.
- `preview_render.py` rend n'importe quel model en isométrique sans
  lancer le jeu (z-buffer, ombrage par face, conventions de rotation du
  jeu) : idéal pour itérer sur les models.

```bash
cd resourcepack/tools
python3 -m venv .venv && .venv/bin/pip install pillow
.venv/bin/python generate_textures.py
.venv/bin/python generate_models.py
.venv/bin/python preview_render.py --all -o /tmp/previews
```

`./gradlew packResourcePack` zippe le pack et écrit son SHA-1.

## Tests

```bash
./gradlew test
```

34 tests unitaires sur le domaine : progression de croissance, lumière,
sécheresse et mort, calcul de qualité, malus de séchage, tolérance,
fenêtre de blackout, seuils de manque, chronologie des effets.
