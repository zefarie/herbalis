#!/usr/bin/env python3
"""Genere les textures 16x16 du resource pack Herbalis (pixel art).

Palette coherente : verts naturels pour les plantes, terre cuite pour le
pot, bois chaud pour le rack, kraft pour les pochons. Les PNG produits
sont commites; un artiste peut les remplacer sans toucher aux models.

Usage : .venv/bin/python generate_textures.py
"""

import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "assets" / "herbalis" / "textures"

# ------------------------------------------------------------------
# Palettes
# ------------------------------------------------------------------

GREEN_DARK = (45, 90, 39, 255)
GREEN_MID = (62, 123, 52, 255)
GREEN = (92, 160, 76, 255)
GREEN_LIGHT = (134, 192, 108, 255)
STEM = (93, 124, 58, 255)
STEM_DARK = (74, 99, 46, 255)
BUD_LIGHT = (154, 181, 92, 255)
BUD_MID = (122, 150, 72, 255)
PISTIL = (217, 142, 59, 255)

T = (0, 0, 0, 0)  # transparent


def clamp(v: float) -> int:
    return max(0, min(255, int(v)))


def new16() -> Image.Image:
    return Image.new("RGBA", (16, 16), T)


def save(img: Image.Image, rel: str) -> None:
    path = TEXTURES / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print(f"  {path.relative_to(ROOT)}")


def from_map(rows: list[str], palette: dict[str, tuple]) -> Image.Image:
    img = new16()
    for y, row in enumerate(rows[:16]):
        row = row.ljust(16, ".")
        for x, char in enumerate(row[:16]):
            if char != ".":
                img.putpixel((x, y), palette[char])
    return img


# ------------------------------------------------------------------
# Plantes : tige + folioles procedurales
# ------------------------------------------------------------------

def put(img: Image.Image, x: int, y: int, color: tuple) -> None:
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), color)


GREEN_RAMP = [GREEN_DARK, GREEN_MID, GREEN, GREEN_LIGHT]


def cluster(img, cx: int, cy: int, width: int, height: int,
            rng: random.Random) -> None:
    """Amas de feuillage dense : sombre dessous, clair dessus, bords
    dechiquetes par dithering."""
    half_h = max(1, height // 2)
    for dy in range(-half_h, half_h + 1):
        shrink = (abs(dy) / (half_h + 0.6)) ** 1.5
        span = max(0, round((width / 2) * (1.0 - shrink)))
        for dx in range(-span, span + 1):
            if abs(dx) == span and rng.random() < 0.35:
                continue  # bord irregulier
            t = 0.55 - dy / (height + 0.5) + rng.uniform(-0.2, 0.2)
            idx = max(0, min(len(GREEN_RAMP) - 1, int(t * len(GREEN_RAMP))))
            put(img, cx + dx, cy + dy, GREEN_RAMP[idx])


def leaf_tips(img, cx: int, cy: int, width: int, count: int,
              rng: random.Random) -> None:
    """Pointes de feuilles qui depassent de l'amas (silhouette dentee)."""
    for _ in range(count):
        side = rng.choice((-1, 1))
        x = cx + side * (width // 2)
        y = cy + rng.randint(-1, 1)
        length = rng.randint(2, 3)
        for step in range(length):
            color = GREEN_RAMP[min(3, 1 + step)]
            put(img, x + side * step, y - step // 2, color)


def cola(img, cx: int, top_y: int, tall: int, rng: random.Random) -> None:
    """Tete compacte au sommet (cola) : coeur dense et pistils orange."""
    for dy in range(tall):
        width = 1 if dy in (0, tall - 1) else 2
        for dx in range(-width + 1, width):
            color = BUD_LIGHT if (dx + dy + rng.randint(0, 1)) % 2 else BUD_MID
            put(img, cx + dx, top_y + dy, color)
    put(img, cx - 1, top_y + 1, PISTIL)
    put(img, cx + 1, top_y + tall - 2, PISTIL)


def plant(height: int, clusters: list[tuple[float, int, int]],
          buds: bool, seed: int) -> Image.Image:
    """Plante : tige visible + amas feuillus + pointes de feuilles.

    clusters : liste de (hauteur relative, largeur, hauteur) d'amas.
    """
    rng = random.Random(seed)
    img = new16()
    base_x, base_y = 8, 15
    top_y = base_y - height

    for y in range(top_y, base_y + 1):
        put(img, base_x, y, STEM if y % 2 == 0 else STEM_DARK)

    for rel, width, tall in clusters:
        cy = round(base_y - height * rel)
        cluster(img, base_x, cy, width, tall, rng)
        leaf_tips(img, base_x, cy, width, 2 + width // 3, rng)

    if buds:
        # Cola principale au sommet et deux grappes laterales.
        cola(img, base_x, top_y - 1, 5, rng)
        top_cluster = clusters[-1]
        side = max(2, top_cluster[1] // 2 - 1)
        cy = round(base_y - height * clusters[0][0])
        cola(img, base_x - side - 1, cy - 2, 3, rng)
        cola(img, base_x + side + 1, cy - 1, 3, rng)
    return img


def dry_variant(img: Image.Image) -> Image.Image:
    """Jaunit une plante assoiffee : verts vers paille et ocre."""
    out = new16()
    for y in range(16):
        for x in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            out.putpixel((x, y), (clamp(r + 62), clamp(g - 8), clamp(b - 34), a))
    return out


def dead_variant(img: Image.Image) -> Image.Image:
    """Plante morte : bruns ternes par luminance."""
    out = new16()
    low, high = (66, 48, 30), (148, 112, 68)
    for y in range(16):
        for x in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            t = (0.3 * r + 0.6 * g + 0.1 * b) / 200.0
            t = max(0.0, min(1.0, t))
            color = tuple(clamp(low[i] + (high[i] - low[i]) * t) for i in range(3))
            out.putpixel((x, y), (*color, a))
    return out


# ------------------------------------------------------------------
# Pot, terre, rack
# ------------------------------------------------------------------

def noisy_fill(base: tuple, variants: list[tuple], density: float,
               seed: int) -> Image.Image:
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), base)
    for y in range(16):
        for x in range(16):
            if rng.random() < density:
                img.putpixel((x, y), rng.choice(variants))
    return img


def pot_side() -> Image.Image:
    base = (167, 105, 66, 255)
    dark = (143, 87, 55, 255)
    light = (186, 122, 79, 255)
    img = noisy_fill(base, [dark, light], 0.16, seed=11)
    rng = random.Random(12)
    # Stries verticales discretes, comme de la terre cuite tournee.
    for x in range(0, 16, 4):
        col = x + rng.randint(0, 2)
        for y in range(16):
            if rng.random() < 0.6:
                img.putpixel((col % 16, y), dark)
    # Ombre sous le rebord et lumiere en haut.
    for x in range(16):
        img.putpixel((x, 0), light)
        img.putpixel((x, 15), (120, 72, 45, 255))
    return img


def pot_rim() -> Image.Image:
    base = (196, 130, 86, 255)
    img = noisy_fill(base, [(210, 146, 100, 255), (178, 114, 73, 255)], 0.2, seed=13)
    for x in range(16):
        img.putpixel((x, 0), (219, 155, 108, 255))
        img.putpixel((x, 15), (160, 100, 63, 255))
    return img


def pot_soil() -> Image.Image:
    return noisy_fill((56, 40, 27, 255),
                      [(76, 56, 38, 255), (40, 28, 18, 255), (66, 50, 32, 255)],
                      0.5, seed=14)


def pot_bottom() -> Image.Image:
    return noisy_fill((122, 76, 47, 255),
                      [(104, 63, 39, 255), (137, 88, 56, 255)], 0.3, seed=15)


def rack_wood() -> Image.Image:
    base = (122, 88, 52, 255)
    grain = (96, 68, 40, 255)
    light = (143, 106, 65, 255)
    img = noisy_fill(base, [light], 0.12, seed=21)
    rng = random.Random(22)
    for y in (3, 7, 11, 14):
        for x in range(16):
            if rng.random() < 0.85:
                img.putpixel((x, y), grain)
    # Deux noeuds de bois.
    for cx, cy in ((4, 5), (11, 12)):
        img.putpixel((cx, cy), (78, 54, 32, 255))
        img.putpixel((cx + 1, cy), grain)
    return img


def rack_hanging(dry: bool) -> Image.Image:
    img = new16()
    string = (201, 178, 138, 255)
    if dry:
        bud_a = (150, 138, 74, 255)
        bud_b = (122, 110, 58, 255)
        tip = (170, 120, 56, 255)
    else:
        bud_a = BUD_LIGHT
        bud_b = (98, 138, 70, 255)
        tip = PISTIL
    # Trois bouquets suspendus tete en bas, longueurs variees.
    for column, (x, top, size) in enumerate(((3, 3, 5), (8, 3, 6), (12, 3, 4))):
        for y in range(top, top + 2):
            put(img, x, y, string)
        for dy in range(size):
            width = 2 if dy < size - 2 else 1
            for dx in range(-width + 1, width):
                color = bud_a if (dx + dy + column) % 2 else bud_b
                put(img, x + dx, top + 2 + dy, color)
        put(img, x, top + 2 + size - 1, tip)
    return img


# ------------------------------------------------------------------
# Items (pixel maps)
# ------------------------------------------------------------------

SEED_MAP = [
    "................",
    "................",
    "................",
    "....KK..........",
    "...KoOK.........",
    "...KOOK.........",
    "...KOhK.........",
    "....KK...KK.....",
    "........KoOK....",
    "........KOOK....",
    "........KOhK....",
    "..KK.....KK.....",
    ".KoOK...........",
    ".KOOK...........",
    ".KOhK...........",
    "..KK............",
]
SEED_PALETTE = {
    "K": (74, 50, 32, 255),
    "O": (122, 82, 48, 255),
    "o": (163, 116, 63, 255),
    "h": (95, 63, 38, 255),
}

WATERING_CAN_MAP = [
    "................",
    "................",
    ".....hhhhh......",
    "....h.....h.....",
    "....h.....h.....",
    ".w..h.....h.....",
    "...MMMMMMMMMMM..",
    ".s.MllmmmmmmmM..",
    ".ssMmmmmmmmmmM..",
    "..sMmmmmmmmmmM..",
    "...MmmmmmmmmmM..",
    "...MmmmmmmmmmM..",
    "...MdddddddddM..",
    "....MMMMMMMMM...",
    "................",
    "................",
]
WATERING_CAN_PALETTE = {
    "M": (74, 84, 92, 255),
    "m": (125, 138, 148, 255),
    "l": (170, 182, 190, 255),
    "d": (100, 111, 120, 255),
    "s": (74, 84, 92, 255),
    "h": (74, 84, 92, 255),
    "w": (120, 180, 220, 255),
}

FERTILIZER_MAP = [
    "................",
    "................",
    "......kk........",
    ".....kkkk.......",
    "....KKkkKK......",
    "...KooooooK.....",
    "..KooOOOOooK....",
    "..KoOOOOOOoK....",
    "..KoOLLOOOoK....",
    "..KoOLGLOOoK....",
    "..KoOOGOOOoK....",
    "..KooOOOOooK....",
    "...KooooooK.....",
    "....KKKKKK......",
    "................",
    "................",
]
FERTILIZER_PALETTE = {
    "K": (95, 70, 45, 255),
    "o": (196, 160, 112, 255),
    "O": (168, 132, 88, 255),
    "L": (110, 170, 80, 255),
    "G": (70, 130, 55, 255),
    "k": (120, 90, 58, 255),
}

ROLLING_PAPER_MAP = [
    "................",
    "................",
    "................",
    "................",
    "....WWWWWWWWf...",
    "...WwwwwwwWff...",
    "...WwwwwwwWf....",
    "...WwsssswwW....",
    "...WwwwwwwwW....",
    "...WwwwwwwwW....",
    "...WwsssswwW....",
    "...WwwwwwwwW....",
    "....WWWWWWWW....",
    "................",
    "................",
    "................",
]
ROLLING_PAPER_PALETTE = {
    "W": (183, 183, 173, 255),
    "w": (244, 244, 238, 255),
    "s": (219, 219, 209, 255),
    "f": (255, 255, 252, 255),
}

POUCH_EMPTY_MAP = [
    "................",
    "................",
    "................",
    "....KKKKKKKK....",
    "...KffffffffK...",
    "...KooooooooK...",
    "..KooOOOOOOooK..",
    "..KoOOOOOOOOoK..",
    "..KoOOOOOOOOoK..",
    "..KoOOOOOOOOoK..",
    "..KoOOOOOOOOoK..",
    "..KooOOOOOOooK..",
    "...KooooooooK...",
    "....KKKKKKKK....",
    "................",
    "................",
]
POUCH_PALETTE = {
    "K": (124, 98, 66, 255),
    "o": (214, 184, 138, 255),
    "O": (186, 152, 106, 255),
    "f": (233, 207, 165, 255),
    "B": (92, 138, 64, 255),
    "L": (130, 172, 88, 255),
    "G": (70, 130, 55, 255),
}

POUCH_FULL_MAP = [
    "................",
    "................",
    "....B.LB.L......",
    "....BLBBLB.L....",
    "...KBBLBBLBK....",
    "...KooooooooK...",
    "..KooOOOOOOooK..",
    "..KoOOOOOOOOoK..",
    "..KoOOGGOOOOoK..",
    "..KoOOGGGOOOoK..",
    "..KoOOOOGOOOoK..",
    "..KooOOOOOOooK..",
    "...KooooooooK...",
    "....KKKKKKKK....",
    "................",
    "................",
]

BUD_MAP = [
    "................",
    "................",
    "................",
    "......bbb.......",
    "....bBBLBb......",
    "...bBLLBBBb.....",
    "...BLpBBLBBb....",
    "..bBLBBLBBLb....",
    "..bBBLLBBpBb....",
    "...bBLBBLLBb....",
    "...bBBpLBBb.....",
    "....bBBLBb......",
    ".....bbbb.......",
    "................",
    "................",
    "................",
]
BUD_FRESH_PALETTE = {
    "b": (58, 92, 44, 255),
    "B": (92, 138, 64, 255),
    "L": (130, 172, 88, 255),
    "p": (214, 140, 60, 255),
}
BUD_DRIED_PALETTE = {
    "b": (84, 88, 44, 255),
    "B": (122, 124, 62, 255),
    "L": (156, 156, 90, 255),
    "p": (190, 120, 50, 255),
}

JOINT_MAP = [
    "................",
    "................",
    "................",
    "...........ee...",
    "..........eWWe..",
    ".........WWwW...",
    "........WwwW....",
    ".......WwwsW....",
    "......WwwsW.....",
    ".....WwwsW......",
    "....WwwsW.......",
    "...FffwW........",
    "...FffF.........",
    "....FF..........",
    "................",
    "................",
]
JOINT_PALETTE = {
    "e": (170, 168, 150, 255),
    "W": (190, 190, 180, 255),
    "w": (240, 240, 232, 255),
    "s": (212, 212, 202, 255),
    "F": (172, 138, 88, 255),
    "f": (208, 176, 122, 255),
}


# ------------------------------------------------------------------
# pack.png
# ------------------------------------------------------------------

def pack_icon() -> Image.Image:
    size = 64
    img = Image.new("RGBA", (size, size), (18, 24, 16, 255))
    draw = ImageDraw.Draw(img)
    # Vignette sombre.
    for radius, color in ((30, (24, 32, 20, 255)), (22, (28, 38, 24, 255))):
        draw.ellipse([32 - radius, 32 - radius, 32 + radius, 32 + radius],
                     fill=color)
    # Feuille stylisee : 7 folioles depuis un point bas.
    cx, cy = 32, 44
    angles = (-90, -60, -120, -30, -150, -8, -172)
    lengths = (26, 22, 22, 16, 16, 10, 10)
    import math
    for angle, length in zip(angles, lengths):
        rad = math.radians(angle)
        tip = (cx + math.cos(rad) * length, cy + math.sin(rad) * length)
        side = math.radians(angle + 90)
        width = max(2.2, length * 0.16)
        base_l = (cx + math.cos(side) * width, cy + math.sin(side) * width)
        base_r = (cx - math.cos(side) * width, cy - math.sin(side) * width)
        draw.polygon([base_l, tip, base_r], fill=(86, 150, 70, 255))
        mid = (cx + math.cos(rad) * length * 0.55,
               cy + math.sin(rad) * length * 0.55)
        draw.line([(cx, cy), mid], fill=(126, 186, 100, 255), width=2)
    draw.line([(cx, cy), (cx, cy + 10)], fill=(93, 124, 58, 255), width=2)
    return img


# ------------------------------------------------------------------
# Main
# ------------------------------------------------------------------

def main() -> None:
    print("Textures :")

    # Plantes (4 stages), variantes seches et morte.
    stage_1 = plant(4, [(1.0, 4, 2)], False, seed=101)
    stage_2 = plant(8, [(0.5, 6, 3), (1.0, 5, 3)], False, seed=102)
    stage_3 = plant(13, [(0.3, 9, 3), (0.62, 8, 3), (0.95, 6, 3)],
                    False, seed=103)
    stage_4 = plant(13, [(0.3, 9, 3), (0.62, 8, 3), (0.95, 6, 3)],
                    True, seed=104)

    save(stage_1, "block/plant_weed_stage_1.png")
    save(stage_2, "block/plant_weed_stage_2.png")
    save(stage_3, "block/plant_weed_stage_3.png")
    save(stage_4, "block/plant_weed_stage_4.png")
    save(dry_variant(stage_2), "block/plant_weed_stage_2_dry.png")
    save(dry_variant(stage_3), "block/plant_weed_stage_3_dry.png")
    save(dry_variant(stage_4), "block/plant_weed_stage_4_dry.png")
    save(dead_variant(stage_2), "block/plant_weed_dead.png")

    # Pot et rack.
    save(pot_side(), "block/pot_side.png")
    save(pot_rim(), "block/pot_rim.png")
    save(pot_soil(), "block/pot_soil.png")
    save(pot_bottom(), "block/pot_bottom.png")
    save(rack_wood(), "block/rack_wood.png")
    save(rack_hanging(dry=False), "block/rack_hanging_fresh.png")
    save(rack_hanging(dry=True), "block/rack_hanging_dry.png")

    # Items.
    save(from_map(SEED_MAP, SEED_PALETTE), "item/weed_seed.png")
    save(from_map(WATERING_CAN_MAP, WATERING_CAN_PALETTE), "item/watering_can.png")
    save(from_map(FERTILIZER_MAP, FERTILIZER_PALETTE), "item/fertilizer.png")
    save(from_map(ROLLING_PAPER_MAP, ROLLING_PAPER_PALETTE), "item/rolling_paper.png")
    save(from_map(POUCH_EMPTY_MAP, POUCH_PALETTE), "item/pouch_empty.png")
    save(from_map(POUCH_FULL_MAP, POUCH_PALETTE), "item/weed_pouch.png")
    save(from_map(BUD_MAP, BUD_FRESH_PALETTE), "item/weed_bud_fresh.png")
    save(from_map(BUD_MAP, BUD_DRIED_PALETTE), "item/weed_dried.png")
    save(from_map(JOINT_MAP, JOINT_PALETTE), "item/weed_joint.png")

    # Icone du pack.
    icon = pack_icon()
    icon.save(ROOT / "pack.png")
    print("  pack.png")
    print("OK")


if __name__ == "__main__":
    main()
