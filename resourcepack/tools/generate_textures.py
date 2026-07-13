#!/usr/bin/env python3
"""Genere les textures du resource pack Herbalis (pixel art).

- Items : 16x16 (coherence vanilla en inventaire).
- Pot, rack, plantes : 32x32 (densite 2x, la ou le regard se pose).
- Plantes : atlas de pieces (feuille, tige, bud) mappees par les models
  sculptes de generate_models.py.
- Font : glyphes 8x8 blancs, teintes par les balises de couleur.

Les PNG produits sont commites; un artiste peut les remplacer sans
toucher aux models (chemins et regions UV stables).

Usage : .venv/bin/python generate_textures.py
"""

import math
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
GREEN_RAMP = [GREEN_DARK, GREEN_MID, GREEN, GREEN_LIGHT]
STEM = (93, 124, 58, 255)
STEM_DARK = (74, 99, 46, 255)
BUD_LIGHT = (154, 181, 92, 255)
BUD_MID = (122, 150, 72, 255)
BUD_DARK = (96, 122, 58, 255)
PISTIL = (217, 142, 59, 255)

T = (0, 0, 0, 0)
WHITE = (255, 255, 255, 255)


def clamp(v: float) -> int:
    return max(0, min(255, int(v)))


def new(size: int) -> Image.Image:
    return Image.new("RGBA", (size, size), T)


def put(img: Image.Image, x: int, y: int, color: tuple) -> None:
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def save(img: Image.Image, rel: str) -> None:
    path = TEXTURES / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print(f"  {path.relative_to(ROOT)}")


def from_map(rows: list[str], palette: dict[str, tuple],
             size: int = 16) -> Image.Image:
    img = new(size)
    for y, row in enumerate(rows[:size]):
        row = row.ljust(size, ".")
        for x, char in enumerate(row[:size]):
            if char != ".":
                img.putpixel((x, y), palette[char])
    return img


# ------------------------------------------------------------------
# Remaps (variantes seche et morte)
# ------------------------------------------------------------------

def dry_variant(img: Image.Image) -> Image.Image:
    out = Image.new("RGBA", img.size, T)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            out.putpixel((x, y), (clamp(r + 62), clamp(g - 8), clamp(b - 34), a))
    return out


def dead_variant(img: Image.Image) -> Image.Image:
    out = Image.new("RGBA", img.size, T)
    low, high = (66, 48, 30), (148, 112, 68)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            t = max(0.0, min(1.0, (0.3 * r + 0.6 * g + 0.1 * b) / 200.0))
            color = tuple(clamp(low[i] + (high[i] - low[i]) * t) for i in range(3))
            out.putpixel((x, y), (*color, a))
    return out


# ------------------------------------------------------------------
# Atlas des pieces de plante (32x32)
#
# Regions (pixels / UV en 16emes) :
#   feuille large : (0,0)-(13,11)   uv [0, 0, 6.5, 5.5]
#   feuille petite: (16,0)-(26,8)   uv [8, 0, 13, 4]
#   tige          : (0,16)-(4,32)   uv [0, 8, 2, 16]
#   bud           : (16,16)-(24,24) uv [8, 8, 12, 12]
#   cola          : (24,16)-(30,26) uv [12, 8, 15, 13]
# ------------------------------------------------------------------

def draw_leaf(img: Image.Image, ox: int, oy: int, w: int, h: int,
              specs: list[tuple[float, float]], rng: random.Random) -> None:
    """Feuille en eventail pointant vers le haut, folioles effilees."""
    base_x, base_y = ox + w // 2, oy + h - 1
    max_len = h - 1.2
    for angle, ratio in specs:
        rad = math.radians(angle)
        dx, dy = math.sin(rad), -math.cos(rad)
        length = max_len * ratio
        steps = int(length) + 1
        for step in range(steps):
            t = step / max(1.0, length)
            x = base_x + dx * step
            y = base_y + dy * step
            idx = min(3, int(t * 4.2))
            color = GREEN_RAMP[idx]
            put(img, round(x), round(y), color)
            # Charnue a la base, effilee en pointe.
            if t < 0.7 and length > 4:
                px = round(x - dy * 0.9)
                py = round(y + dx * 0.9)
                if rng.random() < 0.9:
                    put(img, px, py, GREEN_RAMP[max(0, idx - 1)])
            # Foliole centrale un peu plus large a la base.
            if angle == 0 and t < 0.4:
                put(img, round(x + dy * 0.9), round(y - dx * 0.9),
                    GREEN_RAMP[max(0, idx - 1)])
    # Nervure centrale plus claire sur la foliole principale.
    for step in range(2, int(max_len) - 1, 2):
        put(img, base_x, base_y - step, GREEN_LIGHT)


def plant_parts() -> Image.Image:
    img = new(32)
    rng = random.Random(7)

    # Feuille large : 5 folioles.
    draw_leaf(img, 0, 0, 13, 11,
              [(0, 1.0), (32, 0.85), (-32, 0.85), (68, 0.58), (-68, 0.58)], rng)
    # Feuille petite : 3 folioles.
    draw_leaf(img, 16, 0, 10, 8, [(0, 1.0), (45, 0.7), (-45, 0.7)], rng)

    # Tige : 4x16, bord ombre, noeuds clairs.
    for y in range(16, 32):
        for x, color in ((0, STEM_DARK), (1, STEM), (2, STEM), (3, STEM_DARK)):
            put(img, x, y, color)
    for y in (19, 24, 29):
        put(img, 1, y, GREEN_MID)
        put(img, 2, y, GREEN_LIGHT)

    # Bud : 8x8 dense, lisere sombre, pistils.
    for y in range(16, 24):
        for x in range(16, 24):
            edge = x in (16, 23) or y in (16, 23)
            if edge and (x + y) % 2 == 0:
                continue
            color = BUD_DARK if edge else (
                BUD_LIGHT if (x + y) % 2 else BUD_MID)
            put(img, x, y, color)
    put(img, 18, 18, PISTIL)
    put(img, 21, 21, PISTIL)
    put(img, 19, 22, GREEN_LIGHT)

    # Cola : 6x10, pointe effilee vers le haut.
    for i, width in enumerate((2, 3, 3, 3, 3, 2, 2, 1, 1, 1)):
        y = 25 - i + 0  # de bas (25) vers haut (16)
        y = 25 - i
        cx = 27
        for dx in range(-width + 1, width):
            color = BUD_LIGHT if (dx + i) % 2 else BUD_MID
            put(img, cx + dx, y, color)
    put(img, 26, 22, PISTIL)
    put(img, 28, 19, PISTIL)
    put(img, 27, 16, GREEN_LIGHT)
    return img


# ------------------------------------------------------------------
# Pot (32x32, densite 2x) et rack
# ------------------------------------------------------------------

def noisy(size: int, base: tuple, variants: list[tuple], density: float,
          seed: int) -> Image.Image:
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size), base)
    for y in range(size):
        for x in range(size):
            if rng.random() < density:
                img.putpixel((x, y), rng.choice(variants))
    return img


def pot_side() -> Image.Image:
    base = (181, 112, 70, 255)
    dark = (163, 99, 61, 255)
    light = (197, 126, 81, 255)
    img = noisy(32, base, [dark, light], 0.12, seed=11)
    # Sillons de tournage discrets, tous les 8 px.
    for y in range(6, 32, 8):
        for x in range(32):
            if (x + y) % 5 != 0:
                img.putpixel((x, y), dark)
    for x in range(32):
        img.putpixel((x, 0), light)
        img.putpixel((x, 31), (140, 84, 52, 255))
    return img


def pot_rim() -> Image.Image:
    img = noisy(32, (196, 130, 86, 255),
                [(210, 146, 100, 255), (178, 114, 73, 255)], 0.2, seed=13)
    for x in range(32):
        img.putpixel((x, 0), (222, 158, 110, 255))
        img.putpixel((x, 1), (210, 146, 100, 255))
        img.putpixel((x, 31), (156, 98, 62, 255))
    return img


def pot_soil() -> Image.Image:
    img = noisy(32, (56, 40, 27, 255),
                [(76, 56, 38, 255), (40, 28, 18, 255), (66, 50, 32, 255)],
                0.5, seed=14)
    rng = random.Random(15)
    # Quelques petits cailloux et mottes.
    for _ in range(6):
        x, y = rng.randint(2, 29), rng.randint(2, 29)
        img.putpixel((x, y), (108, 96, 84, 255))
        img.putpixel((x + 1, y), (88, 76, 64, 255))
    return img


def pot_bottom() -> Image.Image:
    return noisy(32, (122, 76, 47, 255),
                 [(104, 63, 39, 255), (137, 88, 56, 255)], 0.3, seed=16)


def rack_wood() -> Image.Image:
    base = (122, 88, 52, 255)
    grain = (96, 68, 40, 255)
    light = (143, 106, 65, 255)
    img = noisy(32, base, [light], 0.1, seed=21)
    rng = random.Random(22)
    # Veines horizontales continues, legerement ondulees.
    for band in range(4):
        y = band * 8 + rng.randint(2, 4)
        for x in range(32):
            yy = y + (1 if (x // 7 + band) % 2 else 0)
            if rng.random() < 0.9:
                img.putpixel((x, yy % 32), grain)
    for cx, cy in ((7, 11), (22, 26), (27, 5)):
        img.putpixel((cx, cy), (78, 54, 32, 255))
        img.putpixel((cx + 1, cy), grain)
        img.putpixel((cx, cy + 1), grain)
    return img


def rack_rope() -> Image.Image:
    img = new(8)
    rope = (201, 178, 138, 255)
    rope_dark = (172, 148, 108, 255)
    for y in range(8):
        for x in range(8):
            img.putpixel((x, y), rope_dark if (x + y) % 3 == 0 else rope)
    return img


def rack_bud(dry: bool) -> Image.Image:
    img = new(8)
    if dry:
        a, b, edge = (150, 138, 74, 255), (122, 110, 58, 255), (98, 88, 48, 255)
        tip = (170, 120, 56, 255)
    else:
        a, b, edge = BUD_LIGHT, (98, 138, 70, 255), (66, 98, 48, 255)
        tip = PISTIL
    for y in range(8):
        for x in range(8):
            is_edge = x in (0, 7) or y in (0, 7)
            if is_edge and (x + y) % 2 == 0:
                continue
            img.putpixel((x, y), edge if is_edge else (a if (x + y) % 2 else b))
    img.putpixel((2, 3), tip)
    img.putpixel((5, 5), tip)
    return img


# ------------------------------------------------------------------
# Glyphes de font (8x8, blancs, teintes par la couleur du texte)
# ------------------------------------------------------------------

GLYPHS: dict[str, list[str]] = {
    #  feuille
    "leaf": [
        "...X....",
        ".X.X.X..",
        ".XXXXX..",
        "XXXXXXX.",
        ".XXXXX..",
        "..XXX...",
        "...X....",
        "...X....",
    ],
    #  goutte
    "drop": [
        "...X....",
        "...X....",
        "..XXX...",
        ".XXXXX..",
        ".XXXXX..",
        ".XXXXX..",
        "..XXX...",
        "........",
    ],
    #  etoile pleine
    "star_full": [
        "...X....",
        "..XXX...",
        "XXXXXXX.",
        ".XXXXX..",
        "..XXX...",
        ".XX.XX..",
        "X.....X.",
        "........",
    ],
    #  etoile vide
    "star_empty": [
        "...X....",
        "..X.X...",
        "XX...XX.",
        ".X...X..",
        "..X.X...",
        ".X...X..",
        "X.....X.",
        "........",
    ],
    #  segment plein
    "seg_full": [
        "........",
        "..XXXXX.",
        ".XXXXX..",
        ".XXXXX..",
        "XXXXX...",
        "........",
        "........",
        "........",
    ],
    #  segment vide
    "seg_empty": [
        "........",
        "..XXXXX.",
        ".X...X..",
        ".X...X..",
        "XXXXX...",
        "........",
        "........",
        "........",
    ],
    #  soleil
    "sun": [
        "...X....",
        ".X.X.X..",
        "..XXX...",
        "XXXXXXX.",
        "..XXX...",
        ".X.X.X..",
        "...X....",
        "........",
    ],
    #  ciseaux
    "scissors": [
        "X....X..",
        ".X..X...",
        "..XX....",
        "..XX....",
        ".X..X...",
        "X....X..",
        "........",
        "........",
    ],
    #  sablier
    "hourglass": [
        "XXXXX...",
        ".XXX....",
        "..X.....",
        "..X.....",
        ".XXX....",
        "XXXXX...",
        "........",
        "........",
    ],
    #  coche
    "check": [
        "......X.",
        ".....XX.",
        "....XX..",
        "X..XX...",
        "XXXX....",
        ".XX.....",
        "........",
        "........",
    ],
    #  alerte
    "warning": [
        "...X....",
        "..XXX...",
        "..XXX...",
        ".XX.XX..",
        ".XXXXX..",
        "XXX.XXX.",
        "XXXXXXX.",
        "........",
    ],
    #  fumee
    "smoke": [
        "....XX..",
        "...XX...",
        "....XX..",
        "...XX...",
        "..XX....",
        "...XX...",
        "..XX....",
        "........",
    ],
}


def glyph(rows: list[str]) -> Image.Image:
    return from_map(rows, {"X": WHITE}, size=8)


# ------------------------------------------------------------------
# Items 16x16 (pixel maps)
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
    for radius, color in ((30, (24, 32, 20, 255)), (22, (28, 38, 24, 255))):
        draw.ellipse([32 - radius, 32 - radius, 32 + radius, 32 + radius],
                     fill=color)
    cx, cy = 32, 44
    angles = (-90, -60, -120, -30, -150, -8, -172)
    lengths = (26, 22, 22, 16, 16, 10, 10)
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

    # Atlas de pieces de plante et variantes.
    parts = plant_parts()
    save(parts, "block/plant_weed_parts.png")
    save(dry_variant(parts), "block/plant_weed_parts_dry.png")
    save(dead_variant(parts), "block/plant_weed_parts_dead.png")

    # Pot et rack (32x).
    save(pot_side(), "block/pot_side.png")
    save(pot_rim(), "block/pot_rim.png")
    save(pot_soil(), "block/pot_soil.png")
    save(pot_bottom(), "block/pot_bottom.png")
    save(rack_wood(), "block/rack_wood.png")
    save(rack_rope(), "block/rack_rope.png")
    save(rack_bud(dry=False), "block/rack_bud_fresh.png")
    save(rack_bud(dry=True), "block/rack_bud_dry.png")

    # Glyphes de font.
    for name, rows in GLYPHS.items():
        save(glyph(rows), f"font/{name}.png")

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

    icon = pack_icon()
    icon.save(ROOT / "pack.png")
    print("  pack.png")
    print("OK")


if __name__ == "__main__":
    main()
