#!/usr/bin/env python3
"""Genere les models JSON et les item definitions du resource pack Herbalis.

Les fichiers produits sont commites dans le repo : ce script sert a les
regenerer d'un bloc si la structure evolue. Les chemins de textures sont
stables pour permettre le remplacement des PNG par un artiste.
"""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "assets" / "herbalis"

FULL_UV = [0, 0, 16, 16]


def write(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"  {path.relative_to(ROOT)}")


# ------------------------------------------------------------------
# Item definitions (composant item_model, 1.21.x)
# ------------------------------------------------------------------

def item_definition(key: str, model: str) -> None:
    write(ASSETS / "items" / f"{key}.json",
          {"model": {"type": "minecraft:model", "model": model}})


# ------------------------------------------------------------------
# Models plats (items 2D)
# ------------------------------------------------------------------

def flat_item(key: str) -> None:
    write(ASSETS / "models" / "item" / f"{key}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"herbalis:item/{key}"},
    })


# ------------------------------------------------------------------
# Models de plantes (croix d'elements, style Blockbench)
# ------------------------------------------------------------------

def cross_plane(angle: float | None, thin_axis: str) -> dict:
    """Un plan vertical sans epaisseur, eventuellement tourne autour de Y."""
    if thin_axis == "z":
        from_, to = [0.8, 0, 8], [15.2, 16, 8]
        faces = {
            "north": {"uv": FULL_UV, "texture": "#cross"},
            "south": {"uv": FULL_UV, "texture": "#cross"},
        }
    else:
        from_, to = [8, 0, 0.8], [8, 16, 15.2]
        faces = {
            "west": {"uv": FULL_UV, "texture": "#cross"},
            "east": {"uv": FULL_UV, "texture": "#cross"},
        }
    element = {"from": from_, "to": to, "shade": False, "faces": faces}
    if angle is not None:
        element["rotation"] = {
            "origin": [8, 8, 8], "axis": "y", "angle": angle, "rescale": True,
        }
    return element


def plant_model(name: str, texture: str, planes: int) -> None:
    """Croix simple (2 plans) ou etoile a 8 pointes (4 plans)."""
    elements = [cross_plane(45, "z"), cross_plane(-45, "z")]
    if planes == 4:
        elements += [cross_plane(None, "z"), cross_plane(None, "x")]
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "ambientocclusion": False,
        "textures": {
            "particle": f"herbalis:block/{texture}",
            "cross": f"herbalis:block/{texture}",
        },
        "elements": elements,
    })


# ------------------------------------------------------------------
# Pot de culture
# ------------------------------------------------------------------

def box(from_, to, texture, faces=None, uv_lock_full=False) -> dict:
    """Boite simple, toutes faces sur la meme texture avec UV 1:1."""
    element = {"from": from_, "to": to, "faces": {}}
    all_faces = faces or ["north", "south", "west", "east", "up", "down"]
    for face in all_faces:
        if face in ("up", "down"):
            uv = [from_[0], from_[2], to[0], to[2]]
        elif face in ("north", "south"):
            uv = [from_[0], 16 - to[1], to[0], 16 - from_[1]]
        else:
            uv = [from_[2], 16 - to[1], to[2], 16 - from_[1]]
        element["faces"][face] = {"uv": uv, "texture": texture}
    return element


def pot_model() -> None:
    body = box([3, 0, 3], [13, 6, 13], "#side")
    body["faces"]["up"] = {"uv": [3, 3, 13, 13], "texture": "#soil"}
    body["faces"]["down"] = {"uv": [3, 3, 13, 13], "texture": "#bottom"}
    rim_boxes = [
        box([2.4, 5, 2.4], [13.6, 6.6, 3.6], "#rim"),
        box([2.4, 5, 12.4], [13.6, 6.6, 13.6], "#rim"),
        box([2.4, 5, 2.4], [3.6, 6.6, 13.6], "#rim"),
        box([12.4, 5, 2.4], [13.6, 6.6, 13.6], "#rim"),
    ]
    write(ASSETS / "models" / "block" / "pot.json", {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": "herbalis:block/pot_side",
            "side": "herbalis:block/pot_side",
            "rim": "herbalis:block/pot_rim",
            "soil": "herbalis:block/pot_soil",
            "bottom": "herbalis:block/pot_bottom",
        },
        "elements": [body, *rim_boxes],
    })


# ------------------------------------------------------------------
# Rack de sechage (3 etats)
# ------------------------------------------------------------------

def rack_model(name: str, hanging_texture: str | None) -> None:
    elements = [
        box([1, 0, 6.5], [3, 16, 9.5], "#wood"),
        box([13, 0, 6.5], [15, 16, 9.5], "#wood"),
        box([0, 13.5, 7], [16, 15, 9], "#wood"),
    ]
    if hanging_texture:
        hanging = {
            "from": [1, 2.5, 8], "to": [15, 13.5, 8],
            "shade": False,
            "faces": {
                "north": {"uv": [1, 2.5, 15, 13.5], "texture": "#hanging"},
                "south": {"uv": [1, 2.5, 15, 13.5], "texture": "#hanging"},
            },
        }
        elements.append(hanging)
    textures = {
        "particle": "herbalis:block/rack_wood",
        "wood": "herbalis:block/rack_wood",
    }
    if hanging_texture:
        textures["hanging"] = f"herbalis:block/{hanging_texture}"
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": elements,
    })


def main() -> None:
    print("Models :")

    # Items plats.
    for key in ["weed_seed", "watering_can", "fertilizer", "rolling_paper",
                "pouch_empty", "weed_pouch", "weed_bud_fresh", "weed_dried",
                "weed_joint"]:
        flat_item(key)
        item_definition(key, f"herbalis:item/{key}")

    # Pot.
    pot_model()
    item_definition("pot", "herbalis:block/pot")

    # Racks.
    rack_model("drying_rack", None)
    rack_model("drying_rack_full", "rack_hanging_fresh")
    rack_model("drying_rack_ready", "rack_hanging_dry")
    for key, model in [("drying_rack", "drying_rack"),
                       ("drying_rack_full", "drying_rack_full"),
                       ("drying_rack_ready", "drying_rack_ready")]:
        item_definition(key, f"herbalis:block/{model}")

    # Plantes : stages 1-2 en croix simple, 3-4 en etoile 4 plans.
    plant_specs = [
        ("plant_weed_stage_1", "plant_weed_stage_1", 2),
        ("plant_weed_stage_2", "plant_weed_stage_2", 2),
        ("plant_weed_stage_3", "plant_weed_stage_3", 4),
        ("plant_weed_stage_4", "plant_weed_stage_4", 4),
        ("plant_weed_stage_2_dry", "plant_weed_stage_2_dry", 2),
        ("plant_weed_stage_3_dry", "plant_weed_stage_3_dry", 4),
        ("plant_weed_stage_4_dry", "plant_weed_stage_4_dry", 4),
        ("plant_weed_dead", "plant_weed_dead", 2),
    ]
    for name, texture, planes in plant_specs:
        plant_model(name, texture, planes)
        item_definition(name, f"herbalis:block/{name}")

    print("OK")


if __name__ == "__main__":
    main()
