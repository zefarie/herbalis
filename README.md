<p align="center"><img src="docs/img/logo.png" width="110" alt="Logo Herbalis"></p>
<h1 align="center">Herbalis</h1>

<p align="center"><em>Culture, séchage et consommation pour serveur roleplay — Paper/Purpur 1.21.11.</em></p>

<p align="center">
  <img alt="Paper/Purpur 1.21.11" src="https://img.shields.io/badge/Paper%2FPurpur-1.21.11-2f6f4f?style=flat-square">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-b07219?style=flat-square">
  <img alt="78 tests" src="https://img.shields.io/badge/tests-78-4c72b0?style=flat-square">
  <img alt="Resource pack inclus" src="https://img.shields.io/badge/resource%20pack-inclus-8a6d3b?style=flat-square">
</p>

<p align="center"><img src="docs/img/banniere.png" width="880" alt="Plante en fleur, citerne, lampe UV, silo, rack de séchage et jarre de curing"></p>

## En bref

- **Des plantes vivantes** : volumes 3D sculptés (feuilles dentelées, branches, colas), croissance continue — même chunk déchargé ou serveur éteint.
- **Du vrai soin** : eau, lumière, engrais, taille, nuisibles ; le terreau et un hologramme d'état privé racontent tout.
- **Un réseau d'irrigation** : caissons d'eau, tuyaux de cuivre auto-connectés, silo d'engrais, lampe UV. Pas relié, pas d'eau.
- **Une qualité en étoiles** : hydratation, engrais, timing de récolte, curing et génétique des graines sur plusieurs générations.
- **Une consommation cinématique** : montée, plateau, descente, blackout ; tolérance et manque persistants ; le joint se passe de main en main.
- **Zéro dépendance** : pas d'ItemsAdder, Oraxen ou Nexo — le resource pack est inclus et généré par scripts.
- **Multi-drogue par design** : ajouter une drogue = un fichier de config et des assets, zéro code. Scope v1 : la weed.

## Installation

1. Compiler : `./gradlew build packResourcePack`
2. Déposer `build/libs/Herbalis-1.0.0.jar` dans `plugins/`.
3. Héberger `build/resourcepack/Herbalis-ResourcePack.zip` (HTTP) et le déclarer dans `server.properties` :

   ```properties
   resource-pack=https://votre-domaine/Herbalis-ResourcePack.zip
   resource-pack-sha1=<contenu du fichier .sha1>
   require-resource-pack=true
   ```

4. Redémarrer. Le plugin crée `plugins/Herbalis/` avec `config.yml`, `messages.yml`, `drugs/weed.yml` et la base SQLite.

> [!IMPORTANT]
> Le resource pack est indispensable : tout le rendu (plantes, pots, caissons, icônes des hologrammes) vient de lui.
> Le driver SQLite est téléchargé automatiquement par le serveur au premier démarrage (`libraries` du `plugin.yml`).

## De la graine au joint

<table align="center">
<tr>
<td><img src="docs/img/croissance.gif" width="230" alt="Croissance d'un plant, de la pousse à la floraison"></td>
<td>

1. **Poser un pot** — clic droit sur une surface solide.
2. **Planter une graine** — elle porte sa lignée (étoiles).
3. **Entretenir** — eau, lumière, engrais, taille, nuisibles.
4. **Récolter** en fenêtre optimale — têtes fraîches + graines héritées.
5. **Sécher** au rack (1 jour), **affiner** en jarre (+1 étoile).
6. **Conditionner** en pochon, **rouler** le joint.
7. **Fumer** — et passer le joint d'un clic droit sur un joueur.

</td>
</tr>
</table>

<p align="center"><img src="docs/img/croissance.png" width="880" alt="Les quatre stages de croissance puis la fenêtre de récolte optimale"></p>
<p align="center"><sub>4 stages de croissance (~4 jours réels jusqu'à la floraison), puis la variante givrée de la fenêtre optimale.</sub></p>

> [!TIP]
> À la floraison, une **fenêtre optimale de 12 heures** s'ouvre : les buds givrent de trichomes et la plante scintille.
> Récolter dedans maximise la qualité ; après, elle décline. Le HUD vous prévient.

<details>
<summary><b>Le détail : croissance, récolte, fumette</b></summary>

- **Croissance** : 4 stages (pousse, jeune plant, plant mature, plant en fleur), transitions animées par interpolation. La croissance suit le temps réel : chunk déchargé ou serveur éteint, la plante rattrape tout son retard au retour (elle continue aussi de boire, revenez l'arroser). Une plante en extérieur pousse au rythme du soleil, rattrapage compris ; une serre éclairée pousse en continu.
- **Hologrammes** : regarder la plante fait flotter un état au-dessus d'elle, visible de vous seul — nom et stage, eau, terreau (humide, sec, fertilisé), qualité potentielle en étoiles, alertes contextuelles. Pots vides, racks et jarres ont le leur.
- **Récolte** : au stade final, clic droit main vide (ou aux cisailles). La qualité (1 à 5 étoiles) combine hydratation moyenne, engrais, timing de récolte et génétique. La récolte rend aussi **2 ou 3 graines héritées** : la plupart gardent les étoiles de la plante mère, certaines dérivent d'une étoile — on sélectionne sa lignée au fil des générations.
- **Conditionner et rouler** : pochon vide en main, clic droit emballe la meilleure weed séchée de l'inventaire. Pochon + feuille à rouler dans une grille de craft = joint, qualité héritée.
- **Fumer** : un joint contient 3 taffes (configurable). Maintenir clic droit fume une taffe : braise à l'allumage, fumée visible par tous, montée en 15 secondes par paliers avec messages d'ambiance, plateau avec buffs (2 minutes à 1 étoile, 6 minutes à 5 étoiles), descente systématique (lenteur, faim, courte nausée). Le joint entamé revient en main avec ses taffes restantes au lore. Clic droit sur un joueur : on lui **passe le joint**, directement dans sa main libre, messages des deux côtés.

</details>

### L'entretien

<p align="center"><img src="docs/img/etats.png" width="880" alt="Plante sur terreau fertilisé, goutte-à-goutte, plante assoiffée, plante morte"></p>
<p align="center"><sub>Terreau fertilisé · goutte-à-goutte installé · plante à sec qui jaunit · plante morte.</sub></p>

| Soin | L'essentiel |
| --- | --- |
| **Lumière** | Niveau 12 minimum (configurable), sinon croissance figée. Une serre éclairée pousse la nuit. |
| **Arrosage** | Une à deux fois par jour, arrosoir en main (8 charges, se recharge sur l'eau). |
| **Engrais** | Un par stage : accélère le stage en cours et améliore la qualité potentielle. |
| **Goutte-à-goutte** | Craftable, s'installe sur le pot, divise la perte d'eau par deux. |
| **Nuisibles** | Moucherons visibles, croissance divisée par deux ; le pulvérisateur traite en un clic. Ignorés 12 h : -1 étoile, définitif. |
| **Taille** | Un coup de cisailles aux stages 2-3, dans la fenêtre du milieu de stage (le HUD affiche des ciseaux) : +1 à 2 têtes. Hors fenêtre : -1 étoile. Une seule taille par plante. |

> [!WARNING]
> Une plante à sec arrête de pousser, **jaunit au bout de 6 h**, puis **meurt 24 h plus tard** (le pot reste).
> Environ deux jours de négligence lui sont fatals — le goutte-à-goutte ou le réseau d'irrigation sont vos amis.

### Le réseau d'irrigation

<p align="center"><img src="docs/img/irrigation.png" width="880" alt="Cuve, citerne, réservoir industriel, tuyaux de cuivre, silo d'engrais et lampe UV"></p>
<p align="center"><sub>Cuve (16 seaux) · citerne (64) · réservoir industriel (256) · tuyaux de cuivre · silo d'engrais · lampe UV.</sub></p>

- **Caissons d'eau** : remplis au seau, ils abreuvent automatiquement les pots **reliés par des tuyaux de cuivre**. Le niveau se lit sur le caisson (cuve ouverte, jauges en façade).
- **Tuyaux** : se posent sur n'importe quelle face, même en l'air, et se connectent tout seuls entre eux, aux caissons, silos et pots (64 models générés).
- **Silo d'engrais** : chargé en doses (16 par défaut), il fertilise tout seul chaque nouveau stage des plantes reliées. Cassé, il rend ses doses.
- **Lampe horticole UV** : halo violet, panneau LED fullbright et **vraie lumière niveau 15** — les caves deviennent des serres, croissance nocturne comprise.

> [!TIP]
> Comptez **un seau par plante et par jour**, deux fois moins avec un goutte-à-goutte.
> L'hologramme de la plante affiche « reliée au réseau » ou « réseau à sec », celui du caisson son stock et ses pots reliés.

### Séchage et curing

<p align="center"><img src="docs/img/sechage.png" width="720" alt="Rack de séchage vide, chargé de têtes fraîches, puis têtes sèches"></p>

- **Sécher** : suspendre jusqu'à 6 têtes fraîches au rack (clic droit). Un jour en temps réel, serveur éteint compris. Le modèle change selon l'état, des particules signalent de loin que c'est prêt. Retirer trop tôt (sneak + clic droit) coûte de la qualité.

<p align="center"><img src="docs/img/curing.png" width="720" alt="Jarre vide, affinage en cours, weed affinée, jarre moisie"></p>

- **Affiner (optionnel)** : déposer la weed séchée en jarre de curing (6 têtes, contenu visible à travers le verre). Deux jours en temps réel : **+1 étoile**.

> [!WARNING]
> Une jarre oubliée **moisit deux jours après la fin d'affinage** : tout le contenu est ruiné (1 étoile),
> et une seule tête moisie contamine la jarre entière.

### Abus, tolérance, manque

- **Tolérance** : monte à chaque joint, décroît en temps réel (même hors ligne). Haute tolérance = effets plus courts et plus faibles.
- **Addiction** : consommer régulièrement rend addict. Sans dose, symptômes périodiques (tremblements, nausée brève, battements de coeur). Tenir assez longtemps fait retomber l'addiction.

> [!CAUTION]
> **Blackout** : 7 taffes en moins de 10 minutes — écran noir, joueur cloué au sol 30 secondes, caméra qui tangue, réveil vaseux.
> Un joint entier fumé seul et vite, c'est déjà flirter avec la limite.

## Items et crafts

Toute la pipeline se craft avec des matériaux vanilla, sauf les graines (récolte, casse de plante ou `/herbalis give` uniquement).

| | Item | Craft | Usage |
| :---: | --- | --- | --- |
| <img src="docs/img/items/pot.png" width="32"> | Pot de culture | 7 briques (forme pot) | Se pose au sol, socle de la plante |
| <img src="docs/img/items/weed_seed.png" width="32"> | Graine de weed | Pas de craft : récolte ou give | Clic droit sur un pot, porte sa lignée (étoiles) |
| <img src="docs/img/items/watering_can.png" width="32"> | Arrosoir | 1 pépite (bec) + 4 lingots de fer | Arrose (8 charges), se recharge sur l'eau |
| <img src="docs/img/items/shears.png" width="32"> | Cisailles | Craft vanilla (2 lingots de fer) | Taille aux stages 2-3, usure vanilla configurable |
| <img src="docs/img/items/sprayer.png" width="32"> | Pulvérisateur | 1 pépite + 1 lingot + 1 fiole (colonne) | Traite les nuisibles (6 charges), se recharge sur l'eau |
| <img src="docs/img/items/dripper.png" width="32"> | Goutte-à-goutte | 3 verres + 1 bâton + 1 ficelle | S'installe sur un pot, perte d'eau divisée par deux |
| <img src="docs/img/items/pipe_63.png" width="32"> | Tuyau d'irrigation | 3 lingots de cuivre (x4) | Relie caissons, silos et pots ; se pose partout |
| <img src="docs/img/items/tank_cuve_full.png" width="32"> | Cuve d'eau | 4 lingots de cuivre + 5 planches | Caisson de 16 seaux, niveau d'eau visible |
| <img src="docs/img/items/tank_citerne_full.png" width="32"> | Citerne d'eau | 8 lingots de fer + 1 seau | Caisson de 64 seaux, jauges en façade |
| <img src="docs/img/items/tank_reservoir_full.png" width="32"> | Réservoir industriel | 4 blocs de fer + 4 lingots + 1 seau | Caisson de 256 seaux, plus haut qu'un bloc |
| <img src="docs/img/items/silo_full.png" width="32"> | Silo d'engrais | 7 planches + 1 entonnoir | Fertilise tout seul les pots reliés (16 doses) |
| <img src="docs/img/items/uv_lamp.png" width="32"> | Lampe horticole UV | 3 améthystes + 2 verres + 1 redstone + 1 lingot | Vraie lumière niveau 15, panneau violet fullbright |
| <img src="docs/img/items/fertilizer.png" width="32"> | Engrais naturel | 2 poudres d'os + 1 terre (x2) | Un par stage, boost vitesse et qualité |
| <img src="docs/img/items/weed_bud_fresh.png" width="32"> | Tête fraîche | Récolte | Se suspend au rack de séchage |
| <img src="docs/img/items/drying_rack_full.png" width="32"> | Rack de séchage | 3 bâtons + 3 ficelles + 2 bâtons | Sèche jusqu'à 6 têtes |
| <img src="docs/img/items/weed_dried.png" width="32"> | Weed séchée | Rack | Se conditionne en pochon, ou s'affine en jarre |
| <img src="docs/img/items/curing_jar_full.png" width="32"> | Jarre de curing | 5 verres + 1 dalle de chêne | Affine la weed séchée (+1 étoile, gare à la moisissure) |
| <img src="docs/img/items/pouch_empty.png" width="32"> | Pochon vide | 1 cuir + 1 ficelle (x2) | Clic droit pour emballer la weed séchée |
| <img src="docs/img/items/weed_pouch.png" width="32"> | Pochon de weed | Conditionnement | Ingrédient du joint |
| <img src="docs/img/items/rolling_paper.png" width="32"> | Feuille à rouler | 1 papier + 1 canne à sucre (x3) | Ingrédient du joint |
| <img src="docs/img/items/weed_joint.png" width="32"> | Joint | Pochon + feuille à rouler | 3 taffes : se fume, se passe (clic droit sur un joueur) |

> [!NOTE]
> Casser une plante (clic gauche) rend une graine de sa lignée (configurable).
> Casser un pot, un rack ou une jarre rend l'item ; pleins, ils rendent d'abord leur contenu.

## Commandes

| Commande | Permission | Description |
| --- | --- | --- |
| `/herbalis give <joueur> <item> [quantité] [qualité]` | `herbalis.admin` | Donne un item Herbalis (qualité 1 à 5, graines incluses) |
| `/herbalis info` | `herbalis.info` | Détails de la plante, du rack ou de la jarre regardés |
| `/herbalis avance <durée>` | `herbalis.admin` | Avance le temps de la cible regardée (ex : `6h`, `2d`) |
| `/herbalis tolerance <joueur> [reset]` | `herbalis.admin` | Consulte ou remet à zéro tolérance et addiction |
| `/herbalis reload` | `herbalis.admin` | Recharge config, messages et drogues |

Tab completion complète sur tout. Alias : `/herb`.

> [!TIP]
> `/herbalis avance 2d` sur la cible regardée : indispensable pour tester la pipeline sans attendre une vraie semaine.

<details>
<summary><b>Items disponibles pour <code>give</code> et permissions</b></summary>

`pot`, `drying_rack`, `curing_jar`, `watering_can`, `sprayer`, `dripper`, `pipe`, `tank_cuve`, `tank_citerne`, `tank_reservoir`, `silo`, `uv_lamp`, `fertilizer`, `rolling_paper`, `pouch_empty`, `weed_seed`, `weed_bud_fresh`, `weed_dried`, `weed_pouch`, `weed_joint`. La taille se fait aux cisailles vanilla.

| Permission | Défaut | Portée |
| --- | --- | --- |
| `herbalis.plant` | tous | Poser pots et racks, planter, entretenir, casser |
| `herbalis.harvest` | tous | Récolter, sécher, conditionner |
| `herbalis.consume` | tous | Fumer |
| `herbalis.info` | op | `/herbalis info` |
| `herbalis.admin` | op | Toutes les commandes d'administration |

</details>

## Configuration

| Fichier | Rôle |
| --- | --- |
| `config.yml` | Ticks, FX, hologrammes, charges des outils, irrigation, drops… Tout est commenté en français. |
| `drugs/weed.yml` | La définition complète de la weed : stages, fenêtres, nuisibles, curing, effets, blackout, génétique. |
| `messages.yml` | 100 % des textes joueur, en MiniMessage. |

> [!TIP]
> Les durées acceptent `30s`, `8m`, `1h30m`, `2d` ou `1j12h`. Les défauts visent le rythme d'une vraie culture
> (environ une semaine de la graine au joint) — tout se raccourcit pour un serveur au rythme arcade.

### Ajouter une drogue

1. Dupliquer `drugs/weed.yml` en `drugs/<id>.yml` et ajuster.
2. Ajouter les assets au pack : items `<id>_seed`, `<id>_bud_fresh`, `<id>_dried`, `<id>_pouch`, `<id>_joint`, et les models `plant_<id>_stage_1` à `4` (variantes `_dry`, `_prime` et `plant_<id>_dead`).
3. `/herbalis reload`. Aucun code à toucher.

<details>
<summary><b>Tout ce qui se règle</b></summary>

- `config.yml` : tick de croissance, autosave, particules et sons, hologrammes (activation, portée du regard), charges de l'arrosoir et du pulvérisateur, facteur du goutte-à-goutte, irrigation (points d'eau par seau, capacité de chaque taille de caisson), capacité du silo, niveau de lumière de la lampe UV, usure des cisailles à la taille, drops, explosions, cadence du manque.
- `drugs/weed.yml` : durées de stages, lumière minimum, hydratation, engrais, fenêtre de récolte, séchage, taille (stages, fenêtre, bonus, malus), nuisibles (chance, ralentissement, délai de dégâts, malus), curing (durée, moisissure, bonus), effets (montée, plateau, descente), taffes par joint, blackout, tolérance, addiction, poids du calcul de qualité (dont la génétique).
- Les sections `taille`, `curing` et `nuisibles` sont optionnelles : une config antérieure reste valide (défauts raisonnables, nuisibles désactivés et génétique à 0 tant qu'ils ne sont pas déclarés).
- Note v1 : la tolérance et l'addiction sont un profil unique par joueur, partagé entre drogues.

</details>

## Architecture

```
domain/          Métier pur, zéro import Bukkit, testé unitairement :
                 Plant, GrowthEngine, Quality, DryingRack, CuringJar,
                 DrugType, ConsumptionEngine (tolérance, blackout, manque)
application/     Cas d'usage (PlantSeed, Water, Harvest, Consume...)
                 et ports (repositories, environnement)
infrastructure/  Bukkit : rendu Item Display + Interaction, SQLite
                 (écriture asynchrone, write-behind), tickers globaux,
                 listeners, commandes, FX, hologrammes d'état
```

- **Pas de bloc vanilla détourné** : pots, plantes et racks sont des Item Display + Interaction, emplacements protégés.
- **Entités jetables, base souveraine** : les Display sont respawnés depuis SQLite au chargement des chunks ; un crash ne laisse aucun fantôme.
- **Un scheduler global par préoccupation**, jamais une task par plante.
- **Hologrammes privés** : un TextDisplay par joueur, invisible pour les autres, qui suit la cible du regard.
- **Timestamps, pas des ticks comptés** : tout survit aux redémarrages ; trois jours d'absence se simulent fidèlement en une fraction de seconde.
- **Composants 1.21.x** : `item_model`, `consumable` (animation de fumage), `max_damage` (jauge de l'arrosoir).

<details>
<summary><b>Le détail des choix techniques</b></summary>

- **Pas de bloc vanilla détourné** : aucun barrel ni note block sacrifié, et les emplacements sont protégés (blocs, eau, pistons, explosions).
- **Entités jetables** : les Display sont non persistantes, respawnées au chargement des chunks depuis SQLite, les orphelines purgées.
- **Hologrammes privés** : `setVisibleByDefault(false)` + `showEntity`, retirés dès qu'on détourne les yeux.
- **Timestamps** : séchage, curing, tolérance, addiction et sessions d'effets survivent aux redémarrages et aux déconnexions. La croissance aussi : chaque plante garde la date de son dernier tick et rattrape son retard par tranches au retour du chunk (assoiffement, passages de stage, mort compris).

</details>

## Resource pack

Structure dans `resourcepack/`, format 75 (1.21.11). Entièrement généré par scripts (Pillow) : les chemins et régions UV sont stables pour qu'un artiste puisse remplacer les PNG sans toucher aux models.

- Plantes sculptées en éléments : feuilles de cannabis dentelées à 5-7 folioles, branches latérales, colas à pistils, variante givrée en fenêtre optimale.
- Pot conique à trois terreaux (humide, sec, fertilisé), rack aux bouquets qui se resserrent en séchant, jarre au contenu visible.
- **64 models de tuyaux générés par masque de connexions**, caissons en trois tailles et quatre niveaux d'eau, silo à trémie, lampe LED fullbright.
- Arrosoir et joint en models 3D en main, sprites 2D en inventaire (select sur le contexte d'affichage, 1.21.4+).
- Font d'icônes `herbalis:icons` (feuille, goutte, étoiles, ciseaux…) pour les hologrammes et `messages.yml`.

```bash
cd resourcepack/tools
python3 -m venv .venv && .venv/bin/pip install pillow
.venv/bin/python generate_textures.py   # textures
.venv/bin/python generate_models.py     # models
.venv/bin/python preview_render.py --all -o /tmp/previews   # previews iso sans lancer le jeu
.venv/bin/python readme_shots.py        # les images de ce README (docs/img/)
```

`./gradlew packResourcePack` zippe le pack et écrit son SHA-1.

## Tests

```bash
./gradlew test
```

78 tests unitaires sur le domaine et les cas d'usage — croissance et rattrapage hors ligne, sécheresse et mort, nuisibles, réseau d'irrigation (BFS des tuyaux, panne sèche, fertigation), qualité (génétique, malus), séchage, curing et moisissure, taille, graines héritées, tolérance, blackout, manque, parsing des durées.

---

<p align="center"><sub>Toutes les images de ce README sont rendues depuis les models du pack par <code>resourcepack/tools/readme_shots.py</code>.</sub></p>
