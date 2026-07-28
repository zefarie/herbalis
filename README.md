<p align="center"><img src="docs/img/logo.png" width="110" alt="Herbalis logo"></p>
<h1 align="center">Herbalis</h1>

<p align="center"><em>Growing, drying and consumption for roleplay servers — Paper/Purpur 1.21.11.</em></p>

<p align="center">
  <a href="https://github.com/mikketa/herbalis/actions/workflows/build.yml"><img alt="build" src="https://github.com/mikketa/herbalis/actions/workflows/build.yml/badge.svg"></a>
  <img alt="Paper/Purpur 1.21.11" src="https://img.shields.io/badge/Paper%2FPurpur-1.21.11-2f6f4f?style=flat-square">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-b07219?style=flat-square">
  <img alt="Resource pack included" src="https://img.shields.io/badge/resource%20pack-included-8a6d3b?style=flat-square">
</p>

<p align="center"><img src="docs/img/banniere.png" width="880" alt="Flowering plant, cistern, UV lamp, silo, drying rack and curing jar"></p>

## At a glance

- **Living plants** — sculpted 3D volumes (serrated leaves, side branches, colas) that keep growing even with the chunk unloaded or the server offline.
- **Real plant care** — water, light, fertilizer, pruning, pests; the soil and a private status hologram tell the whole story.
- **An irrigation network** — water tanks, self-connecting copper pipes, a fertilizer silo, a UV grow lamp. Not connected, no water.
- **Star-based quality** — average hydration, fertilizer, harvest timing, curing and seed genetics selected over generations.
- **Cinematic consumption** — onset, plateau, comedown, blackout; persistent tolerance and withdrawal; joints get passed from hand to hand.
- **Zero dependencies** — no ItemsAdder, Oraxen or Nexo; the resource pack ships with the plugin, fully script-generated.
- **Multi-drug by design** — adding a drug is one config file plus assets, zero code. v1 scope: weed.

## Installation

1. Build: `./gradlew build packResourcePack` — or grab the artifacts from the [latest build](https://github.com/mikketa/herbalis/actions/workflows/build.yml).
2. Drop `build/libs/Herbalis-1.0.0.jar` into your server's `plugins/` folder (Paper/Purpur 1.21.11).
3. Host `build/resourcepack/Herbalis-ResourcePack.zip` over HTTP and declare it in `server.properties`:

   ```properties
   resource-pack=https://your-domain/Herbalis-ResourcePack.zip
   resource-pack-sha1=<contents of the .sha1 file>
   require-resource-pack=true
   ```

4. Restart. The plugin creates `plugins/Herbalis/` with `config.yml`, `messages.yml`, `drugs/weed.yml` and its SQLite database.

> [!IMPORTANT]
> The resource pack is mandatory: every visual (plants, pots, tanks, hologram icons) comes from it.
> The SQLite driver is downloaded automatically by the server on first startup (`libraries` in `plugin.yml`).

## From seed to joint

<table align="center">
<tr>
<td><img src="docs/img/croissance.gif" width="230" alt="A plant growing from sprout to flowering"></td>
<td>

1. **Place a growing pot** — right-click on any solid surface.
2. **Plant a seed** — it carries its lineage (stars).
3. **Take care of it** — water, light, fertilizer, pruning, pests.
4. **Harvest** in the optimal window — fresh buds + inherited seeds.
5. **Dry** on a rack (1 day), **cure** in a jar (+1 star).
6. **Package** into a pouch, **roll** the joint.
7. **Smoke** — or pass the joint with a right-click on a player.

</td>
</tr>
</table>

<p align="center"><img src="docs/img/croissance.png" width="880" alt="The four growth stages, then the optimal harvest window"></p>
<p align="center"><sub>4 growth stages (~4 real-time days to flowering), then the frosted variant of the optimal harvest window.</sub></p>

> [!TIP]
> At flowering, a **12-hour optimal window** opens: buds frost over with trichomes and the plant sparkles.
> Harvesting inside it maximizes quality; afterwards it declines. The HUD warns you.

<details>
<summary><b>The details: growth, harvest, smoking</b></summary>

- **Growth**: 4 stages (sprout, young plant, mature plant, flowering plant), transitions animated by interpolation. Growth follows real time: with the chunk unloaded or the server offline, the plant catches up on everything when it returns (it keeps drinking too — come back and water it). An outdoor plant grows at the sun's pace, catch-up included; a lit greenhouse grows around the clock.
- **Holograms**: looking at a plant floats a status display above it, visible to you only — name and stage, water, soil (moist, dry, fertilized), potential quality in stars, contextual alerts. Empty pots, racks and jars have their own.
- **Harvest**: at the final stage, right-click with an empty hand (or shears). Quality (1 to 5 stars) combines average hydration, fertilizer, harvest timing and genetics. Harvesting also returns **2 or 3 inherited seeds**: most keep the mother plant's stars, some drift by one — you select your lineage over generations.
- **Package and roll**: with an empty pouch in hand, right-click to pack your best dried weed. Pouch + rolling paper in any crafting grid = a joint, quality inherited.
- **Smoking**: a joint holds 3 puffs (configurable). Hold right-click to take one: an ember on lighting, smoke visible to everyone, a 15-second staged onset with ambient messages, a plateau with buffs (2 minutes at 1 star, 6 minutes at 5 stars), then a guaranteed comedown (slowness, hunger, brief nausea). A started joint comes back to your hand with its remaining puffs in the lore. Right-click a player to **pass the joint** straight into their free hand, with messages on both sides.

</details>

### Plant care

<p align="center"><img src="docs/img/etats.png" width="880" alt="Fertilized soil, drip irrigator, thirsty plant, dead plant"></p>
<p align="center"><sub>Fertilized soil · drip irrigator installed · thirsty plant turning yellow · dead plant.</sub></p>

| Care | What matters |
| --- | --- |
| **Light** | Level 12 minimum (configurable), otherwise growth freezes. A lit greenhouse grows at night. |
| **Watering** | Once or twice a day, watering can in hand (8 charges, refills on water). |
| **Fertilizer** | One per stage: speeds up the current stage and raises potential quality. |
| **Drip irrigator** | Craftable, sits on the pot, halves water loss. |
| **Pests** | Visible gnats, growth halved; the sprayer treats them in one click. Ignored for 12 h: −1 star, permanent. |
| **Pruning** | One snip with shears at stage 2-3, inside the mid-stage window (the HUD shows scissors): +1 to 2 buds. Outside the window: −1 star. One pruning per plant. |

> [!WARNING]
> A thirsty plant stops growing, **turns yellow after 6 h**, then **dies 24 h later** (the pot survives).
> Roughly two days of neglect are fatal — the drip irrigator and the irrigation network are your friends.

### The irrigation network

<p align="center"><img src="docs/img/irrigation.png" width="880" alt="Tub, cistern, industrial reservoir, copper pipes, fertilizer silo and UV lamp"></p>
<p align="center"><sub>Tub (16 buckets) · cistern (64) · industrial reservoir (256) · copper pipes · fertilizer silo · UV grow lamp.</sub></p>

- **Water tanks**: filled by bucket, they automatically water every pot **connected to them with copper pipes**. The water level shows on the tank itself (open tub, front gauges).
- **Pipes**: place on any face, even mid-air; they connect on their own to each other and to tanks, silos and pots (64 generated models).
- **Fertilizer silo**: loaded with doses (16 by default), it fertilizes every new stage of connected plants on its own. Break it and it returns its doses.
- **UV grow lamp**: purple halo, fullbright LED panel and **real light level 15** — caves become greenhouses, night growth included.

> [!TIP]
> Budget **one bucket per plant per day**, half that with a drip irrigator.
> The plant's hologram shows “connected to network” or “network dry”; the tank's shows its stock and connected pots.

### Drying and curing

<p align="center"><img src="docs/img/sechage.png" width="720" alt="Drying rack: empty, loaded with fresh buds, then dry buds"></p>

- **Drying**: hang up to 6 fresh buds on a rack (right-click). One real-time day, server downtime included. The model changes with its state and subtle particles show from afar that it's ready. Taking buds out early (sneak + right-click) costs quality.

<p align="center"><img src="docs/img/curing.png" width="720" alt="Curing jar: empty, curing, cured weed, moldy"></p>

- **Curing (optional)**: put dried weed in a curing jar (6 buds, contents visible through the glass). Two real-time days: **+1 star**.

> [!WARNING]
> A forgotten jar **grows mold two days after curing completes**: the whole contents are ruined (1 star),
> and a single moldy bud contaminates the entire jar.

### Abuse, tolerance, withdrawal

- **Tolerance**: rises with every joint, decays in real time (even offline). High tolerance = shorter, weaker effects.
- **Addiction**: regular use makes you addicted. Without a dose, periodic symptoms kick in (trembling, brief nausea, heartbeats). Holding out long enough brings the addiction back down.

> [!CAUTION]
> **Blackout**: 7 puffs within 10 minutes — black screen, pinned to the ground for 30 seconds, swaying camera, groggy wake-up.
> A whole joint smoked alone and fast already flirts with the limit.

## Items and crafts

The whole pipeline is craftable from vanilla materials, except seeds (harvest, plant breaking or `/herbalis give` only).

| | Item | Craft | Use |
| :---: | --- | --- | --- |
| <img src="docs/img/items/pot.png" width="32"> | Growing pot | 7 bricks (pot shape) | Sits on the ground, the plant's base |
| <img src="docs/img/items/weed_seed.png" width="32"> | Weed seed | No craft: harvest or give | Right-click a pot; carries its lineage (stars) |
| <img src="docs/img/items/watering_can.png" width="32"> | Watering can | 1 nugget (spout) + 4 iron ingots | Waters (8 charges), refills on water |
| <img src="docs/img/items/shears.png" width="32"> | Shears | Vanilla craft (2 iron ingots) | Pruning at stages 2-3, configurable vanilla durability |
| <img src="docs/img/items/sprayer.png" width="32"> | Sprayer | 1 nugget + 1 ingot + 1 bottle (column) | Treats pests (6 charges), refills on water |
| <img src="docs/img/items/dripper.png" width="32"> | Drip irrigator | 3 glass + 1 stick + 1 string | Sits on a pot, halves water loss |
| <img src="docs/img/items/pipe_63.png" width="32"> | Irrigation pipe | 3 copper ingots (x4) | Connects tanks, silos and pots; places anywhere |
| <img src="docs/img/items/tank_cuve_full.png" width="32"> | Water tub | 4 copper ingots + 5 planks | 16-bucket tank, water level visible |
| <img src="docs/img/items/tank_citerne_full.png" width="32"> | Water cistern | 8 iron ingots + 1 bucket | 64-bucket tank, front gauges |
| <img src="docs/img/items/tank_reservoir_full.png" width="32"> | Industrial reservoir | 4 iron blocks + 4 ingots + 1 bucket | 256-bucket tank, taller than one block |
| <img src="docs/img/items/silo_full.png" width="32"> | Fertilizer silo | 7 planks + 1 hopper | Auto-fertilizes connected pots (16 doses) |
| <img src="docs/img/items/uv_lamp.png" width="32"> | UV grow lamp | 3 amethyst + 2 glass + 1 redstone + 1 ingot | Real light level 15, fullbright purple panel |
| <img src="docs/img/items/fertilizer.png" width="32"> | Natural fertilizer | 2 bone meal + 1 dirt (x2) | One per stage, speed and quality boost |
| <img src="docs/img/items/weed_bud_fresh.png" width="32"> | Fresh bud | Harvest | Hangs on the drying rack |
| <img src="docs/img/items/drying_rack_full.png" width="32"> | Drying rack | 3 sticks + 3 strings + 2 sticks | Dries up to 6 buds |
| <img src="docs/img/items/weed_dried.png" width="32"> | Dried weed | Rack | Packs into a pouch, or cures in a jar |
| <img src="docs/img/items/curing_jar_full.png" width="32"> | Curing jar | 5 glass + 1 oak slab | Cures dried weed (+1 star, beware the mold) |
| <img src="docs/img/items/pouch_empty.png" width="32"> | Empty pouch | 1 leather + 1 string (x2) | Right-click to pack dried weed |
| <img src="docs/img/items/weed_pouch.png" width="32"> | Weed pouch | Packaging | Joint ingredient |
| <img src="docs/img/items/rolling_paper.png" width="32"> | Rolling paper | 1 paper + 1 sugar cane (x3) | Joint ingredient |
| <img src="docs/img/items/weed_joint.png" width="32"> | Joint | Pouch + rolling paper | 3 puffs: smoke it, pass it (right-click a player) |

> [!NOTE]
> Breaking a plant (left-click) returns a seed of its lineage (configurable).
> Breaking a pot, rack or jar returns the item; if loaded, they drop their contents first.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/herbalis give <player> <item> [amount] [quality]` | `herbalis.admin` | Gives a Herbalis item (quality 1 to 5, seeds included) |
| `/herbalis info` | `herbalis.info` | Details of the plant, rack or jar you're looking at |
| `/herbalis avance <duration>` | `herbalis.admin` | Advances time for the target you're looking at (e.g. `6h`, `2d`) |
| `/herbalis tolerance <player> [reset]` | `herbalis.admin` | Checks or resets tolerance and addiction |
| `/herbalis reload` | `herbalis.admin` | Reloads config, messages and drugs |

Full tab completion on everything. Alias: `/herb`.

> [!TIP]
> `/herbalis avance 2d` on the target you're looking at: essential for testing the pipeline without waiting a real week.

<details>
<summary><b>Items available to <code>give</code>, and permissions</b></summary>

`pot`, `drying_rack`, `curing_jar`, `watering_can`, `sprayer`, `dripper`, `pipe`, `tank_cuve`, `tank_citerne`, `tank_reservoir`, `silo`, `uv_lamp`, `fertilizer`, `rolling_paper`, `pouch_empty`, `weed_seed`, `weed_bud_fresh`, `weed_dried`, `weed_pouch`, `weed_joint`. Pruning uses vanilla shears.

| Permission | Default | Scope |
| --- | --- | --- |
| `herbalis.plant` | everyone | Place pots and racks, plant, care, break |
| `herbalis.harvest` | everyone | Harvest, dry, package |
| `herbalis.consume` | everyone | Smoke |
| `herbalis.info` | op | `/herbalis info` |
| `herbalis.admin` | op | All admin commands |

</details>

## Configuration

| File | Role |
| --- | --- |
| `config.yml` | Ticks, FX, holograms, tool charges, irrigation, drops… fully commented. |
| `drugs/weed.yml` | The complete weed definition: stages, windows, pests, curing, effects, blackout, genetics. |
| `messages.yml` | Every player-facing string, in MiniMessage. Ships in French — translate freely. |

> [!TIP]
> Durations accept `30s`, `8m`, `1h30m`, `2d`. Defaults aim for a real grow's pace
> (about one week from seed to joint) — everything shortens for an arcade-paced server.

### Adding a drug

1. Duplicate `drugs/weed.yml` as `drugs/<id>.yml` and adjust.
2. Add the assets to the pack: items `<id>_seed`, `<id>_bud_fresh`, `<id>_dried`, `<id>_pouch`, `<id>_joint`, and the models `plant_<id>_stage_1` to `4` (`_dry`, `_prime` variants and `plant_<id>_dead`).
3. `/herbalis reload`. No code to touch.

<details>
<summary><b>Everything you can tune</b></summary>

- `config.yml`: growth tick, autosave, particles and sounds, holograms (toggle, gaze range), watering can and sprayer charges, drip irrigator factor, irrigation (water points per bucket, capacity of each tank size), silo capacity, UV lamp light level, shears wear on pruning, drops, explosions, withdrawal cadence.
- `drugs/weed.yml`: stage durations, minimum light, hydration, fertilizer, harvest window, drying, pruning (stages, window, bonus, malus), pests (chance, slowdown, damage delay, malus), curing (duration, mold, bonus), effects (onset, plateau, comedown), puffs per joint, blackout, tolerance, addiction, quality weights (genetics included).
- The `taille` (pruning), `curing` and `nuisibles` (pests) sections are optional: an older config stays valid (sane defaults, pests disabled and genetics at 0 until declared).
- v1 note: tolerance and addiction are a single per-player profile, shared across drugs.

</details>

## Architecture

```
domain/          Pure logic, zero Bukkit imports, unit-tested:
                 Plant, GrowthEngine, Quality, DryingRack, CuringJar,
                 DrugType, ConsumptionEngine (tolerance, blackout, withdrawal)
application/     Use cases (PlantSeed, Water, Harvest, Consume...)
                 and ports (repositories, environment)
infrastructure/  Bukkit: Item Display + Interaction rendering, SQLite
                 (async write-behind), global tickers, listeners,
                 commands, FX, status holograms
```

- **No hijacked vanilla blocks**: pots, plants and racks are Item Displays + Interactions, with protected placements.
- **Disposable entities, authoritative database**: Displays respawn from SQLite on chunk load; a crash leaves no ghosts.
- **One global scheduler per concern**, never one task per plant.
- **Private holograms**: one TextDisplay per player, invisible to others, following your gaze.
- **Timestamps, not counted ticks**: everything survives restarts; three days away are simulated faithfully in a split second.
- **1.21.x components**: `item_model`, `consumable` (smoking animation), `max_damage` (watering can gauge).

<details>
<summary><b>Technical choices in detail</b></summary>

- **No hijacked vanilla blocks**: no sacrificed barrels or note blocks, and placements are protected (blocks, water, pistons, explosions).
- **Disposable entities**: Displays are non-persistent, respawned from SQLite on chunk load, orphans purged.
- **Private holograms**: `setVisibleByDefault(false)` + `showEntity`, removed as soon as you look away.
- **Timestamps**: drying, curing, tolerance, addiction and effect sessions survive restarts and disconnects. Growth too: each plant keeps its last tick date and catches up in slices when the chunk returns (thirst, stage transitions, death included).

</details>

## Resource pack

Lives in `resourcepack/`, pack format 75 (1.21.11). Entirely script-generated (Pillow): paths and UV regions are stable so an artist can replace the PNGs without touching the models.

- Plants sculpted from elements: serrated 5-7 leaflet cannabis leaves, side branches, pistil colas, frosted variant during the optimal window.
- Tiered conical pot with three soils (moist, dry, fertilized), a rack whose bud bunches tighten as they dry, a jar with visible contents.
- **64 pipe models generated from a connection mask**, tanks in three sizes with four water levels each, a hopper silo, a fullbright LED lamp.
- Watering can and joint as 3D hand-held models with 2D inventory sprites (display-context select, 1.21.4+).
- `herbalis:icons` icon font (leaf, drop, stars, scissors…) for holograms and `messages.yml`.

```bash
cd resourcepack/tools
python3 -m venv .venv && .venv/bin/pip install pillow
.venv/bin/python generate_textures.py   # textures
.venv/bin/python generate_models.py     # models
.venv/bin/python preview_render.py --all -o /tmp/previews   # iso previews without launching the game
.venv/bin/python readme_shots.py        # this README's images (docs/img/)
```

`./gradlew packResourcePack` zips the pack and writes its SHA-1.

## Tests

```bash
./gradlew test
```

78 unit tests over the domain and use cases — growth and offline catch-up, thirst and death, pests, irrigation network (pipe BFS, dry spells, fertigation), quality (genetics, maluses), drying, curing and mold, pruning, inherited seeds, tolerance, blackout, withdrawal, duration parsing.

---

<p align="center"><sub>Every image in this README is rendered from the pack's models by <code>resourcepack/tools/readme_shots.py</code>.</sub></p>
