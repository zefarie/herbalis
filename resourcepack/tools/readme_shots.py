#!/usr/bin/env python3
"""Genere les images du README (docs/img/) a partir des models du pack.

S'appuie sur preview_render.py pour le rendu isometrique, puis :
- fusionne pot + plante dans une meme scene (offset SOIL_HEIGHT = 6/16,
  comme DisplayRenderer), occlusion correcte ;
- detoure le fond et recadre chaque rendu (PNG transparents, lisibles
  sur les deux themes GitHub) ;
- compose les planches (banniere, croissance, etats, irrigation,
  sechage, curing) en alignant les modeles sur leur ligne de sol,
  tous a la meme echelle ;
- assemble un GIF de croissance ;
- exporte une icone carree par item pour le tableau des crafts.

Usage :
  .venv/bin/python readme_shots.py
"""

import json
import tempfile
from pathlib import Path

from PIL import Image

import preview_render as pr

pr.SCALE = 16.0
pr.SIZE = 760

ROOT = Path(__file__).resolve().parent.parent.parent
OUT = ROOT / "docs" / "img"
BLOCKS = pr.MODELS / "block"
ITEMS_TEX = pr.TEXTURES / "item"
FONT_TEX = pr.TEXTURES / "font"

BG = (44, 44, 52, 255)  # fond opaque de preview_render
SOIL_Y = 6.0            # SOIL_HEIGHT du DisplayRenderer, en unites de model


def merge_pot_plant(pot_name: str, plant_name: str) -> dict:
    """Pose les elements d'une plante dans un pot, comme en jeu."""
    pot = json.loads((BLOCKS / f"{pot_name}.json").read_text())
    plant = json.loads((BLOCKS / f"{plant_name}.json").read_text())

    textures = dict(pot.get("textures", {}))
    textures.update({f"p_{k}": v for k, v in plant.get("textures", {}).items()})

    elements = list(pot.get("elements", []))
    for element in plant.get("elements", []):
        element = json.loads(json.dumps(element))  # copie profonde
        for key in ("from", "to"):
            element[key][1] += SOIL_Y
        if "rotation" in element:
            element["rotation"]["origin"][1] += SOIL_Y
        for face in element.get("faces", {}).values():
            face["texture"] = "#p_" + face["texture"].lstrip("#")
        elements.append(element)

    return {"textures": textures, "elements": elements}


def render(name: str, merged: dict | None = None) -> Image.Image:
    """Rend un model (ou une scene fusionnee), detoure et recadre."""
    with tempfile.TemporaryDirectory() as tmp:
        if merged is not None:
            model = Path(tmp) / "merged.json"
            model.write_text(json.dumps(merged))
        else:
            model = BLOCKS / f"{name}.json"
        out = Path(tmp) / "render.png"
        pr.render_model(model, out)
        img = Image.open(out).convert("RGBA")

    pixels = img.load()
    for y in range(img.height):
        for x in range(img.width):
            if pixels[x, y] == BG:
                pixels[x, y] = (0, 0, 0, 0)
    return img.crop(img.getbbox())


def strip(images: list[Image.Image], gap: int = 56, margin: int = 24,
          bg=None) -> Image.Image:
    """Aligne des rendus cote a cote sur une meme ligne de sol."""
    width = sum(i.width for i in images) + gap * (len(images) - 1) + 2 * margin
    height = max(i.height for i in images) + 2 * margin
    color = bg if bg is not None else (0, 0, 0, 0)
    sheet = Image.new("RGBA", (width, height), color)
    x = margin
    for img in images:
        sheet.paste(img, (x, height - margin - img.height), img)
        x += img.width + gap
    return sheet


def save(sheet: Image.Image, name: str, max_width: int = 1800) -> None:
    if sheet.width > max_width:
        ratio = max_width / sheet.width
        sheet = sheet.resize((max_width, round(sheet.height * ratio)),
                             Image.LANCZOS)
    sheet.save(OUT / name)
    print(f"  docs/img/{name} ({sheet.width}x{sheet.height})")


def square_icon(img: Image.Image, size: int = 128) -> Image.Image:
    """Icone carree pour les tableaux du README."""
    img.thumbnail((size, size), Image.NEAREST)
    icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    icon.paste(img, ((size - img.width) // 2, (size - img.height) // 2), img)
    return icon


def sprite(path: Path, factor: int = 8) -> Image.Image:
    img = Image.open(path).convert("RGBA")
    img = img.resize((img.width * factor, img.height * factor), Image.NEAREST)
    return img.crop(img.getbbox())


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "items").mkdir(exist_ok=True)

    print("Rendus...")
    stages = [merge_pot_plant("pot", f"plant_weed_stage_{n}")
              for n in (1, 2, 3, 4)]
    prime = merge_pot_plant("pot", "plant_weed_stage_4_prime")
    growth = [render("", m) for m in stages + [prime]]

    dry = render("", merge_pot_plant("pot_dry", "plant_weed_stage_3_dry"))
    dead = render("", merge_pot_plant("pot_dry", "plant_weed_dead"))
    drip = render("", merge_pot_plant("pot_drip", "plant_weed_stage_2"))
    fert = render("", merge_pot_plant("pot_fert", "plant_weed_stage_2"))

    blocks = {name: render(name) for name in (
        "tank_cuve_full", "tank_citerne_full", "tank_reservoir_full",
        "silo_full", "uv_lamp", "pipe_12", "pipe_63",
        "drying_rack", "drying_rack_full", "drying_rack_ready",
        "curing_jar", "curing_jar_full", "curing_jar_ready",
        "curing_jar_moldy")}

    print("Planches...")
    save(strip([growth[4], blocks["tank_citerne_full"], blocks["uv_lamp"],
                blocks["silo_full"], blocks["drying_rack_full"],
                blocks["curing_jar_full"]]), "banniere.png")
    save(strip(growth, gap=72), "croissance.png")
    save(strip([fert, drip, dry, dead], gap=88), "etats.png")
    save(strip([blocks["tank_cuve_full"], blocks["tank_citerne_full"],
                blocks["tank_reservoir_full"], blocks["pipe_12"],
                blocks["pipe_63"], blocks["silo_full"], blocks["uv_lamp"]],
               gap=48), "irrigation.png")
    save(strip([blocks["drying_rack"], blocks["drying_rack_full"],
                blocks["drying_rack_ready"]]), "sechage.png")
    save(strip([blocks["curing_jar"], blocks["curing_jar_full"],
                blocks["curing_jar_ready"], blocks["curing_jar_moldy"]],
               gap=88), "curing.png")

    print("GIF de croissance...")
    frames_src = growth + [growth[4]]
    w = max(i.width for i in frames_src) + 48
    h = max(i.height for i in frames_src) + 48
    frames = []
    for img in frames_src:
        frame = Image.new("RGBA", (w, h), BG)
        frame.paste(img, ((w - img.width) // 2, h - 24 - img.height), img)
        frames.append(frame.convert("P", palette=Image.ADAPTIVE))
    frames[0].save(OUT / "croissance.gif", save_all=True,
                   append_images=frames[1:], loop=0,
                   duration=[900, 900, 900, 900, 1600, 1600])
    print("  docs/img/croissance.gif")

    print("Icones d'items...")
    for tex in ("watering_can", "sprayer", "dripper", "fertilizer",
                "pouch_empty", "rolling_paper", "weed_seed",
                "weed_bud_fresh", "weed_dried", "weed_pouch", "weed_joint"):
        icon = square_icon(sprite(ITEMS_TEX / f"{tex}.png"))
        icon.save(OUT / "items" / f"{tex}.png")
    icon = square_icon(sprite(FONT_TEX / "scissors.png"))
    icon.save(OUT / "items" / "shears.png")
    for name in ("pot", "drying_rack_full", "curing_jar_full", "pipe_63",
                 "tank_cuve_full", "tank_citerne_full", "tank_reservoir_full",
                 "silo_full", "uv_lamp"):
        icon = square_icon(render(name).copy())
        icon.save(OUT / "items" / f"{name}.png")
    print(f"  docs/img/items/ ({len(list((OUT / 'items').glob('*.png')))} icones)")


if __name__ == "__main__":
    main()
