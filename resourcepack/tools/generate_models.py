#!/usr/bin/env python3
"""Genere les models JSON, item definitions et font du resource pack.

Les plantes sont sculptees en volumes : tige en boite, feuilles en
quads inclines disposes en rosettes, buds en petits cubes. Les regions
UV pointent dans l'atlas plant_weed_parts.png (voir generate_textures).

Convention de rotation Minecraft (verifiee sur block/lectern.json) :
un angle positif autour de +X abaisse le cote nord de l'element.

Usage : .venv/bin/python generate_models.py
"""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "assets" / "herbalis"

FULL_UV = [0, 0, 16, 16]

# Regions UV de l'atlas de pieces (en 16emes de texture).
# Deux silhouettes par taille de feuille, alternees dans les rosettes.
LEAF_UVS = {
    ("large", 0): [0, 0, 6.5, 5.5],
    ("large", 1): [7, 0, 13.5, 5.5],
    ("small", 0): [0, 6, 5, 10],
    ("small", 1): [5.5, 6, 10.5, 10],
}
STEM_UV = [14, 0, 16, 8]
BUD_UV = [0, 10.5, 4, 14.5]
COLA_UV = [4.5, 10.5, 7.5, 15.5]


def write(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"  {path.relative_to(ROOT)}")


def item_definition(key: str, model: str) -> None:
    write(ASSETS / "items" / f"{key}.json",
          {"model": {"type": "minecraft:model", "model": model}})


def flat_item(key: str) -> None:
    write(ASSETS / "models" / "item" / f"{key}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"herbalis:item/{key}"},
    })


# ------------------------------------------------------------------
# Briques d'elements
# ------------------------------------------------------------------

def box(from_, to, texture, uv=None) -> dict:
    """Boite pleine. Sans uv explicite, mapping 1:1 sur les coordonnees."""
    element = {"from": list(from_), "to": list(to), "faces": {}}
    for face in ("north", "south", "west", "east", "up", "down"):
        if uv is not None:
            face_uv = uv
        elif face in ("up", "down"):
            face_uv = [from_[0], from_[2], to[0], to[2]]
        elif face in ("north", "south"):
            face_uv = [from_[0], 16 - to[1], to[0], 16 - from_[1]]
        else:
            face_uv = [from_[2], 16 - to[1], to[2], 16 - from_[1]]
        element["faces"][face] = {"uv": face_uv, "texture": texture}
    return element


def leaf(base_dir: str, y: float, length: float, width: float,
         droop: float, size: str, yaw: float | None = None,
         variant: int = 0) -> dict:
    """Feuille : quad horizontal sans epaisseur partant de la tige.

    base_dir : n / s / e / w. droop positif = pointe vers le sol.
    yaw : rotation optionnelle autour de Y (feuilles diagonales,
    exclusif avec droop). variant : silhouette 0 ou 1 de l'atlas.
    """
    uv = LEAF_UVS[(size, variant % 2)]
    hw = width / 2
    gap = 0.4  # attache au bord de la tige

    match base_dir:
        case "n":
            from_, to = [8 - hw, y, 8 - gap - length], [8 + hw, y, 8 - gap]
            axis, angle, rot = "x", droop, 0
            origin = [8, y, 8 - gap]
        case "s":
            from_, to = [8 - hw, y, 8 + gap], [8 + hw, y, 8 + gap + length]
            axis, angle, rot = "x", -droop, 180
            origin = [8, y, 8 + gap]
        case "e":
            from_, to = [8 + gap, y, 8 - hw], [8 + gap + length, y, 8 + hw]
            axis, angle, rot = "z", droop, 90
            origin = [8 + gap, y, 8]
        case "w":
            from_, to = [8 - gap - length, y, 8 - hw], [8 - gap, y, 8 + hw]
            axis, angle, rot = "z", -droop, 270
            origin = [8 - gap, y, 8]
        case _:
            raise ValueError(base_dir)

    if yaw is not None:
        axis, angle = "y", yaw
        origin = [8, y, 8]

    element = {
        "from": from_, "to": to,
        "shade": False,
        "faces": {
            "up": {"uv": uv, "texture": "#parts", "rotation": rot},
            "down": {"uv": uv, "texture": "#parts", "rotation": rot},
        },
    }
    if angle:
        element["rotation"] = {
            "origin": origin, "axis": axis,
            "angle": angle, "rescale": False,
        }
    return element


def stem(height: float, half: float = 0.4) -> dict:
    element = box([8 - half, 0, 8 - half], [8 + half, height, 8 + half],
                  "#parts", uv=STEM_UV)
    element["faces"]["up"]["uv"] = [14, 0, 16, 2]
    element["faces"]["down"]["uv"] = [14, 0, 16, 2]
    return element


def bud_cube(center_x: float, y: float, center_z: float, size: float) -> dict:
    half = size / 2
    return box([center_x - half, y, center_z - half],
               [center_x + half, y + size, center_z + half],
               "#parts", uv=BUD_UV)


# ------------------------------------------------------------------
# Plantes sculptees
# ------------------------------------------------------------------

def rosette(y: float, length: float, width: float, droop: float,
            size: str) -> list[dict]:
    """Quatre feuilles cardinales inclinees, silhouettes alternees."""
    return [leaf(d, y, length, width, droop, size, variant=i)
            for i, d in enumerate("nsew")]


def rosette_diagonal(y: float, length: float, width: float,
                     size: str) -> list[dict]:
    """Quatre feuilles a plat en diagonale (variete de canopee)."""
    return [
        leaf("n", y, length, width, 0, size, yaw=45, variant=1),
        leaf("n", y + 0.15, length, width, 0, size, yaw=-45, variant=0),
        leaf("s", y + 0.3, length, width, 0, size, yaw=45, variant=0),
        leaf("s", y + 0.45, length, width, 0, size, yaw=-45, variant=1),
    ]


def plant_stage_1() -> list[dict]:
    return [
        stem(3.8),
        leaf("n", 2.4, 3.5, 3.0, 22.5, "small", variant=0),
        leaf("s", 2.8, 3.2, 2.8, 22.5, "small", variant=1),
        leaf("e", 3.1, 2.8, 2.6, 22.5, "small", variant=1),
        leaf("w", 3.4, 2.6, 2.4, 22.5, "small", variant=0),
    ]


def plant_stage_2() -> list[dict]:
    return [
        stem(7.5),
        *rosette(3.6, 4.5, 3.6, 22.5, "small"),
        *rosette_diagonal(5.8, 4.0, 3.2, "small"),
        leaf("n", 7.2, 3.5, 3.0, -22.5, "small", variant=1),
        leaf("s", 7.4, 3.2, 2.8, -22.5, "small", variant=0),
    ]


def plant_stage_3() -> list[dict]:
    return [
        stem(11.5, half=0.5),
        *rosette(3.8, 6.5, 5.5, 22.5, "large"),
        *rosette_diagonal(6.8, 5.5, 4.6, "large"),
        *rosette(9.4, 4.5, 3.6, 22.5, "small"),
        leaf("n", 11.2, 3.5, 3.0, -22.5, "small", variant=1),
        leaf("s", 11.4, 3.2, 2.8, -22.5, "small", variant=0),
    ]


def plant_stage_4() -> list[dict]:
    cola = box([7.2, 11.3, 7.2], [8.8, 14.6, 8.8], "#parts", uv=COLA_UV)
    cola["faces"]["up"]["uv"] = BUD_UV
    cola["faces"]["down"]["uv"] = BUD_UV
    tip = box([7.6, 14.6, 7.6], [8.4, 15.4, 8.4], "#parts", uv=BUD_UV)
    return [
        *plant_stage_3(),
        cola,
        tip,
        bud_cube(8.0, 4.6, 3.6, 1.5),
        bud_cube(8.0, 5.0, 12.4, 1.5),
        bud_cube(12.2, 4.4, 8.0, 1.5),
        bud_cube(3.8, 5.2, 8.0, 1.5),
    ]


def plant_dead() -> list[dict]:
    return [
        stem(6.0),
        leaf("n", 5.0, 4.5, 3.4, 45, "small"),
        leaf("s", 5.4, 4.0, 3.0, 45, "small"),
        leaf("w", 5.8, 3.6, 2.8, 45, "small"),
    ]


def plant_model(name: str, elements: list[dict], parts_texture: str) -> None:
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "ambientocclusion": False,
        "textures": {
            "particle": parts_texture,
            "parts": parts_texture,
        },
        "elements": elements,
    })


# ------------------------------------------------------------------
# Pot de culture (conique, par etages)
# ------------------------------------------------------------------

def pot_model(name: str, soil_texture: str) -> None:
    """Le terreau raconte l'etat de la plante : humide, sec ou fertilise."""
    foot = box([5.2, 0, 5.2], [10.8, 1.8, 10.8], "#side")
    foot["faces"]["down"] = {"uv": [5.2, 5.2, 10.8, 10.8], "texture": "#bottom"}
    mid = box([4.1, 1.8, 4.1], [11.9, 4.0, 11.9], "#side")
    # L'etage haut s'arrete sous le terreau pour ne jamais le recouvrir.
    top = box([3.1, 4.0, 3.1], [12.9, 5.5, 12.9], "#side")
    soil = box([3.6, 4.6, 3.6], [12.4, 6, 12.4], "#side")
    soil["faces"]["up"] = {"uv": [3.6, 3.6, 12.4, 12.4], "texture": "#soil"}
    rim = [
        box([2.6, 5.2, 2.6], [13.4, 6.6, 3.9], "#rim"),
        box([2.6, 5.2, 12.1], [13.4, 6.6, 13.4], "#rim"),
        box([2.6, 5.2, 2.6], [3.9, 6.6, 13.4], "#rim"),
        box([12.1, 5.2, 2.6], [13.4, 6.6, 13.4], "#rim"),
    ]
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": "herbalis:block/pot_side",
            "side": "herbalis:block/pot_side",
            "rim": "herbalis:block/pot_rim",
            "soil": f"herbalis:block/{soil_texture}",
            "bottom": "herbalis:block/pot_bottom",
        },
        "elements": [foot, mid, top, soil, *rim],
    })


# ------------------------------------------------------------------
# Rack de sechage (bouquets en volume)
# ------------------------------------------------------------------

def rack_model(name: str, buds_texture: str | None,
               shrink: float = 1.0) -> None:
    """shrink < 1 : les bouquets se resserrent en sechant (etat pret)."""
    elements = [
        box([0.4, 0, 6.3], [3.4, 1.4, 9.7], "#wood"),
        box([12.6, 0, 6.3], [15.6, 1.4, 9.7], "#wood"),
        box([1, 0, 6.9], [2.8, 15.2, 9.1], "#wood"),
        box([13.2, 0, 6.9], [15, 15.2, 9.1], "#wood"),
        box([0, 13.6, 7.1], [16, 15, 8.9], "#wood"),
    ]
    bunches = ((4.6, 0.0), (8.0, -0.9), (11.4, 0.5))
    if buds_texture is None:
        # Rack vide : trois bouts de corde qui pendent.
        for x, drop in bunches:
            elements.append(box([x - 0.2, 11.4 + drop, 7.85],
                                [x + 0.2, 13.6, 8.15], "#rope", uv=FULL_UV))
    else:
        big_w, big_h = 1.5 * shrink, 3 * shrink
        small_w, small_h = 0.95 * shrink, 1.4 * shrink
        for x, drop in bunches:
            top = 10.8 + drop
            elements.append(box([x - 0.2, top, 7.85],
                                [x + 0.2, 13.6, 8.15], "#rope", uv=FULL_UV))
            elements.append(box([x - big_w, top - big_h, 8 - 0.95 * shrink],
                                [x + big_w, top, 8 + 0.95 * shrink],
                                "#buds", uv=FULL_UV))
            elements.append(box([x - small_w, top - big_h - small_h,
                                 8 - 0.5 * shrink],
                                [x + small_w, top - big_h, 8 + 0.5 * shrink],
                                "#buds", uv=FULL_UV))
    textures = {
        "particle": "herbalis:block/rack_wood",
        "wood": "herbalis:block/rack_wood",
        "rope": "herbalis:block/rack_rope",
    }
    if buds_texture:
        textures["buds"] = f"herbalis:block/{buds_texture}"
    write(ASSETS / "models" / "block" / f"{name}.json", {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": elements,
    })


# ------------------------------------------------------------------
# Models 3D en main (arrosoir, joint)
#
# En inventaire (gui/fixed), l'item definition bascule sur le sprite 2D
# via un select sur minecraft:display_context.
# ------------------------------------------------------------------

HANDHELD_DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, -90, 0], "translation": [0, 1.5, 0],
        "scale": [0.55, 0.55, 0.55],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 90, 0], "translation": [0, 1.5, 0],
        "scale": [0.55, 0.55, 0.55],
    },
    "firstperson_righthand": {
        "rotation": [0, -45, 0], "translation": [0, 2.5, 0],
        "scale": [0.5, 0.5, 0.5],
    },
    "firstperson_lefthand": {
        "rotation": [0, 45, 0], "translation": [0, 2.5, 0],
        "scale": [0.5, 0.5, 0.5],
    },
    "ground": {"translation": [0, 2, 0], "scale": [0.35, 0.35, 0.35]},
    "head": {"scale": [0.8, 0.8, 0.8]},
}


def watering_can_3d() -> None:
    # Bec attache bas sur le corps, qui monte vers l'exterieur,
    # termine par une pomme d'arrosage plus large.
    spout = box([-2.2, 4.4, 7.35], [4.2, 5.8, 8.65], "#metal")
    spout["rotation"] = {"origin": [4.2, 5.1, 8], "axis": "z",
                         "angle": 22.5, "rescale": False}
    tip = box([-3.6, 4.9, 7.0], [-1.9, 6.3, 9.0], "#metal")
    tip["rotation"] = {"origin": [4.2, 5.1, 8], "axis": "z",
                       "angle": 22.5, "rescale": False}
    elements = [
        box([4, 2, 5], [12, 10, 11], "#metal"),
        spout,
        tip,
        box([5.5, 10, 7.5], [6.5, 12.4, 8.5], "#metal"),
        box([10.5, 10, 7.5], [11.5, 12.4, 8.5], "#metal"),
        box([5.5, 12.4, 7.5], [11.5, 13.4, 8.5], "#metal"),
    ]
    write(ASSETS / "models" / "item" / "watering_can_3d.json", {
        "textures": {
            "particle": "herbalis:item/watering_can_metal",
            "metal": "herbalis:item/watering_can_metal",
        },
        "elements": elements,
        "display": HANDHELD_DISPLAY,
    })


def joint_3d() -> None:
    body = box([7.35, 2, 7.35], [8.65, 12, 8.65], "#wrap", uv=[0, 0, 3, 16])
    body["faces"]["up"]["uv"] = [0, 0, 3, 1.5]
    body["faces"]["down"]["uv"] = [0, 13, 3, 14.5]
    display = {
        "thirdperson_righthand": {
            "rotation": [-70, 0, 0], "translation": [0, 2, 1],
            "scale": [0.45, 0.45, 0.45],
        },
        "thirdperson_lefthand": {
            "rotation": [-70, 0, 0], "translation": [0, 2, 1],
            "scale": [0.45, 0.45, 0.45],
        },
        "firstperson_righthand": {
            "rotation": [-55, 15, 0], "translation": [0, 3, 1],
            "scale": [0.5, 0.5, 0.5],
        },
        "firstperson_lefthand": {
            "rotation": [-55, -15, 0], "translation": [0, 3, 1],
            "scale": [0.5, 0.5, 0.5],
        },
        "ground": {"translation": [0, 1, 0], "scale": [0.35, 0.35, 0.35]},
    }
    write(ASSETS / "models" / "item" / "weed_joint_3d.json", {
        "textures": {
            "particle": "herbalis:item/weed_joint_wrap",
            "wrap": "herbalis:item/weed_joint_wrap",
        },
        "elements": [body],
        "display": display,
    })


def item_definition_contextual(key: str, flat_model: str, hand_model: str) -> None:
    """Sprite 2D en inventaire, model 3D en main et au sol."""
    write(ASSETS / "items" / f"{key}.json", {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:display_context",
            "cases": [{
                "when": ["gui", "fixed"],
                "model": {"type": "minecraft:model", "model": flat_model},
            }],
            "fallback": {"type": "minecraft:model", "model": hand_model},
        },
    })


# ------------------------------------------------------------------
# Font d'icones
# ------------------------------------------------------------------

FONT_GLYPHS = [
    ("leaf", ""),
    ("drop", ""),
    ("star_full", ""),
    ("star_empty", ""),
    ("seg_full", ""),
    ("seg_empty", ""),
    ("sun", ""),
    ("scissors", ""),
    ("hourglass", ""),
    ("check", ""),
    ("warning", ""),
    ("smoke", ""),
]


def font_json() -> None:
    providers = [
        {
            "type": "bitmap",
            "file": f"herbalis:font/{name}.png",
            "ascent": 7,
            "height": 8,
            "chars": [char],
        }
        for name, char in FONT_GLYPHS
    ]
    write(ASSETS / "font" / "icons.json", {"providers": providers})


# ------------------------------------------------------------------
# Main
# ------------------------------------------------------------------

def main() -> None:
    print("Models :")

    for key in ["weed_seed", "watering_can", "fertilizer", "rolling_paper",
                "pouch_empty", "weed_pouch", "weed_bud_fresh", "weed_dried",
                "weed_joint"]:
        flat_item(key)
        item_definition(key, f"herbalis:item/{key}")

    # Arrosoir et joint : 3D en main, sprite 2D en inventaire.
    watering_can_3d()
    joint_3d()
    item_definition_contextual("watering_can",
                               "herbalis:item/watering_can",
                               "herbalis:item/watering_can_3d")
    item_definition_contextual("weed_joint",
                               "herbalis:item/weed_joint",
                               "herbalis:item/weed_joint_3d")

    # Pots : le terreau reflete l'etat (humide, sec, fertilise).
    pot_model("pot", "pot_soil")
    pot_model("pot_dry", "pot_soil_dry")
    pot_model("pot_fert", "pot_soil_fert")
    for key in ("pot", "pot_dry", "pot_fert"):
        item_definition(key, f"herbalis:block/{key}")

    rack_model("drying_rack", None)
    rack_model("drying_rack_full", "rack_bud_fresh")
    rack_model("drying_rack_ready", "rack_bud_dry", shrink=0.8)
    for key in ("drying_rack", "drying_rack_full", "drying_rack_ready"):
        item_definition(key, f"herbalis:block/{key}")

    fresh = "herbalis:block/plant_weed_parts"
    dry = "herbalis:block/plant_weed_parts_dry"
    dead = "herbalis:block/plant_weed_parts_dead"
    stages = {
        "plant_weed_stage_1": (plant_stage_1(), fresh),
        "plant_weed_stage_2": (plant_stage_2(), fresh),
        "plant_weed_stage_3": (plant_stage_3(), fresh),
        "plant_weed_stage_4": (plant_stage_4(), fresh),
        "plant_weed_stage_2_dry": (plant_stage_2(), dry),
        "plant_weed_stage_3_dry": (plant_stage_3(), dry),
        "plant_weed_stage_4_dry": (plant_stage_4(), dry),
        "plant_weed_dead": (plant_dead(), dead),
    }
    for name, (elements, texture) in stages.items():
        plant_model(name, elements, texture)
        item_definition(name, f"herbalis:block/{name}")

    font_json()
    print("OK")


if __name__ == "__main__":
    main()
