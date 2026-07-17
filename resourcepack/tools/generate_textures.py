#!/usr/bin/env python3
"""Genere les textures du resource pack Herbalis (pixel art).

- Items : 16x16 (coherence vanilla en inventaire).
- Pot, rack : 32x32 (densite 2x).
- Plantes : atlas de pieces 64x64 (feuilles de cannabis dentelees a
  5-7 folioles, tige, buds), mappe par les models sculptes de
  generate_models.py. C'est la que le regard se pose : densite 4x.
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


# Regions de bud et de cola dans l'atlas (x0, y0, x1, y1).
BUD_REGIONS = [(0, 42, 16, 58), (18, 42, 30, 62)]


def prime_variant(img: Image.Image) -> Image.Image:
    """Buds givres de trichomes (fenetre de recolte optimale) : voile
    clair et mouchetis blancs, uniquement sur les regions de bud."""
    out = img.copy()
    rng = random.Random(37)
    for x0, y0, x1, y1 in BUD_REGIONS:
        for y in range(y0, y1):
            for x in range(x0, x1):
                r, g, b, a = out.getpixel((x, y))
                if a == 0:
                    continue
                if rng.random() < 0.22:
                    out.putpixel((x, y), (238, 242, 246, 255))
                else:
                    out.putpixel((x, y),
                                 (clamp(r + 24), clamp(g + 24), clamp(b + 20), a))
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
# Atlas des pieces de plante (64x64)
#
# Regions (pixels / UV en 16emes, memes UV relatifs que l'ancien 32x) :
#   feuille large A : (0,0)-(26,22)    uv [0, 0, 6.5, 5.5]
#   feuille large B : (28,0)-(54,22)   uv [7, 0, 13.5, 5.5]
#   feuille petite A: (0,24)-(20,40)   uv [0, 6, 5, 10]
#   feuille petite B: (22,24)-(42,40)  uv [5.5, 6, 10.5, 10]
#   tige            : (56,0)-(64,32)   uv [14, 0, 16, 8]
#   bud             : (0,42)-(16,58)   uv [0, 10.5, 4, 14.5]
#   cola            : (18,42)-(30,62)  uv [4.5, 10.5, 7.5, 15.5]
#
# Deux silhouettes par taille de feuille : les rosettes des models les
# alternent pour casser la repetition.
# ------------------------------------------------------------------

VEIN = (168, 210, 130, 255)

# Folioles : (angle depuis la verticale, longueur relative, demi-largeur px).
FINGERS_LARGE_A = [
    (0, 1.0, 1.3), (30, 0.88, 1.15), (-30, 0.88, 1.15),
    (58, 0.68, 1.0), (-58, 0.68, 1.0), (81, 0.42, 0.8), (-81, 0.42, 0.8),
]
FINGERS_LARGE_B = [
    (6, 0.97, 1.25), (35, 0.84, 1.15), (-26, 0.88, 1.15),
    (62, 0.62, 0.95), (-53, 0.7, 1.0), (84, 0.38, 0.8), (-77, 0.46, 0.8),
]
FINGERS_SMALL_A = [
    (0, 1.0, 1.15), (38, 0.78, 1.0), (-38, 0.78, 1.0),
    (70, 0.46, 0.8), (-70, 0.46, 0.8),
]
FINGERS_SMALL_B = [
    (8, 0.95, 1.1), (43, 0.74, 1.0), (-33, 0.8, 1.0),
    (75, 0.42, 0.8), (-65, 0.5, 0.8),
]


def draw_fan_leaf(img: Image.Image, ox: int, oy: int, w: int, h: int,
                  fingers: list[tuple[float, float, float]]) -> None:
    """Feuille de cannabis : folioles dentelees en eventail depuis un
    petiole, effilees en pointe, nervure centrale claire."""
    node_x = ox + w / 2.0
    base_y = oy + h - 1.0
    stalk = 3.0 if h >= 20 else 2.0
    for i in range(int(stalk) + 1):
        put(img, round(node_x), round(base_y - i), STEM)
    node_y = base_y - stalk
    max_len = h - stalk - 1.5

    for angle, ratio, hw_max in fingers:
        rad = math.radians(angle)
        dx, dy = math.sin(rad), -math.cos(rad)
        perp_x, perp_y = -dy, dx
        length = max_len * ratio
        steps = int(length * 2) + 1
        for k in range(steps):
            t = k / max(1, steps - 1)
            cx = node_x + dx * length * t
            cy = node_y + dy * length * t
            # Profil : etroit a la base, charnu au tiers, effile en pointe.
            if t < 0.35:
                prof = 0.4 + 0.6 * (t / 0.35)
            else:
                prof = max(0.1, 1.0 - 0.95 * ((t - 0.35) / 0.65) ** 1.1)
            hw = hw_max * prof
            # Dents : une saillie sur trois pas, hors base et pointe.
            if 0.2 < t < 0.9 and k % 3 == 0:
                hw += 0.5
            idx = min(3, int(t * 3.6))
            off = -hw
            while off <= hw:
                color = GREEN_RAMP[idx]
                if abs(off) > hw - 0.55:
                    color = GREEN_RAMP[max(0, idx - 1)]
                put(img, round(cx + perp_x * off), round(cy + perp_y * off),
                    color)
                off += 0.5
            # Nervure centrale, en pointille.
            if k % 2 == 0 and 0.05 < t < 0.85:
                put(img, round(cx), round(cy), VEIN)

    # Ombre au point de convergence des folioles : creuse le centre.
    for ddy in range(-3, 1):
        for ddx in range(-2, 3):
            x, y = round(node_x) + ddx, round(node_y) + ddy
            if math.hypot(ddx, ddy) < 2.6 and 0 <= x < img.width \
                    and 0 <= y < img.height and img.getpixel((x, y))[3]:
                img.putpixel((x, y), GREEN_DARK)


def draw_stem_strip(img: Image.Image, ox: int, oy: int, w: int,
                    h: int) -> None:
    """Tige verticale : bords ombres, striures, noeuds clairs."""
    rng = random.Random(31)
    for y in range(h):
        for x in range(w):
            if x in (0, w - 1):
                color = STEM_DARK
            elif x in (1, w - 2):
                color = STEM if (y + x) % 3 else STEM_DARK
            else:
                color = STEM if rng.random() < 0.8 else (108, 142, 68, 255)
            put(img, ox + x, oy + y, color)
    for ny in range(5, h, 9):
        for x in range(2, w - 2):
            put(img, ox + x, oy + ny, GREEN_MID)
        put(img, ox + w // 2, oy + ny, GREEN_LIGHT)


def draw_bud_blob(img: Image.Image, ox: int, oy: int, w: int, h: int,
                  seed: int, taper: float = 0.0) -> None:
    """Masse de calices : blobs imbriques, lisere sombre, pistils orange.

    taper > 0 : silhouette qui s'effile vers le haut (cola)."""
    rng = random.Random(seed)

    def half_width(ny: float) -> float:
        # ny : 0 en haut, 1 en bas. Bords arrondis, effilage optionnel.
        round_cap = math.sin(min(1.0, ny * 4.0) * math.pi / 2) \
            * math.sin(min(1.0, (1 - ny) * 4.0) * math.pi / 2)
        base = w / 2 - 0.6
        narrow = 1.0 - taper * (1.0 - ny)
        return max(1.0, base * narrow * (0.55 + 0.45 * round_cap))

    cx = ox + w / 2.0 - 0.5
    inside = []
    for y in range(h):
        ny = y / (h - 1)
        hw = half_width(ny)
        for x in range(w):
            if abs(ox + x - cx) <= hw:
                inside.append((ox + x, oy + y))
                edge = abs(ox + x - cx) > hw - 1.1 or y in (0, h - 1)
                color = BUD_DARK if edge else (
                    BUD_MID if (x + y) % 2 else BUD_DARK)
                put(img, ox + x, oy + y, color)
    # Calices : petits amas clairs avec ombre portee, en quinconce.
    for _ in range(max(9, w * h // 14)):
        bx, by = inside[rng.randrange(len(inside))]
        put(img, bx, by, BUD_LIGHT)
        put(img, bx - 1, by, BUD_MID)
        put(img, bx + 1, by + 1, BUD_DARK)
    # Pistils : petits crochets orange.
    for _ in range(max(3, w * h // 60)):
        bx, by = inside[rng.randrange(len(inside))]
        put(img, bx, by, PISTIL)
        put(img, bx + rng.choice((-1, 1)), by - 1, PISTIL)
    # Pointes de sugar leaves qui depassent en bas.
    for _ in range(3):
        bx, by = inside[rng.randrange(len(inside))]
        if by > oy + h * 0.6:
            put(img, bx, by, GREEN_LIGHT)


def plant_parts() -> Image.Image:
    img = new(64)

    # Feuilles en eventail : 7 folioles (larges), 5 folioles (petites).
    draw_fan_leaf(img, 0, 0, 26, 22, FINGERS_LARGE_A)
    draw_fan_leaf(img, 28, 0, 26, 22, FINGERS_LARGE_B)
    draw_fan_leaf(img, 0, 24, 20, 16, FINGERS_SMALL_A)
    draw_fan_leaf(img, 22, 24, 20, 16, FINGERS_SMALL_B)

    draw_stem_strip(img, 56, 0, 8, 32)

    # Bud rond et cola effile vers le haut.
    draw_bud_blob(img, 0, 42, 16, 16, seed=12)
    draw_bud_blob(img, 18, 42, 12, 20, seed=13, taper=0.5)
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


def pot_soil_dry() -> Image.Image:
    """Terreau a sec : pale, craquele. Le soin se lit de loin."""
    img = noisy(32, (118, 94, 66, 255),
                [(132, 108, 78, 255), (104, 82, 58, 255)], 0.4, seed=17)
    rng = random.Random(18)
    # Craquelures : segments sombres qui serpentent.
    for _ in range(7):
        x, y = rng.randint(2, 29), rng.randint(2, 29)
        for _ in range(rng.randint(4, 8)):
            img.putpixel((x % 32, y % 32), (78, 60, 42, 255))
            x += rng.choice((-1, 0, 1))
            y += rng.choice((-1, 1))
    return img


def pot_soil_fert() -> Image.Image:
    """Terreau fertilise : sombre et humide, mouchete de nutriments."""
    img = pot_soil()
    rng = random.Random(19)
    for _ in range(14):
        x, y = rng.randint(1, 30), rng.randint(1, 30)
        img.putpixel((x, y), rng.choice(
            [(110, 170, 80, 255), (230, 222, 196, 255)]))
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


def can_metal() -> Image.Image:
    """Metal brosse de l'arrosoir 3D (16x16)."""
    img = Image.new("RGBA", (16, 16), (125, 138, 148, 255))
    rng = random.Random(23)
    for y in range(16):
        shade = rng.choice((-1, 0, 0, 1))
        for x in range(16):
            base = 125 + shade * 9 + rng.choice((-4, 0, 4))
            img.putpixel((x, y), (clamp(base), clamp(base + 13), clamp(base + 23), 255))
    for x in range(16):
        img.putpixel((x, 0), (170, 182, 190, 255))
        img.putpixel((x, 15), (91, 103, 112, 255))
    return img


def joint_wrap() -> Image.Image:
    """Papier roule du joint 3D : pointe torsadee, corps, filtre (16x16)."""
    img = Image.new("RGBA", (16, 16), (240, 240, 232, 255))
    rng = random.Random(29)
    for y in range(16):
        for x in range(16):
            if y == 0:
                img.putpixel((x, y), (176, 174, 158, 255))  # torsade
            elif y >= 12:
                tan = (196, 164, 110, 255) if (x + y) % 2 else (208, 176, 122, 255)
                img.putpixel((x, y), tan)  # filtre
            elif rng.random() < 0.18:
                img.putpixel((x, y), (222, 222, 212, 255))  # grain du papier
    for x in range(16):
        img.putpixel((x, 11), (212, 200, 168, 255))  # lisere du filtre
    return img


def jar_glass() -> Image.Image:
    """Verre de la jarre : cadre visible, interieur transparent."""
    img = new(32)
    frame = (214, 230, 236, 255)
    frame_dark = (168, 190, 200, 255)
    for i in range(32):
        for edge in (0, 1, 30, 31):
            put(img, i, edge, frame if edge in (0, 31) else frame_dark)
            put(img, edge, i, frame if edge in (0, 31) else frame_dark)
    # Reflets discrets dans le vide.
    for x, y in ((6, 5), (7, 6), (8, 7), (24, 20), (25, 21)):
        put(img, x, y, (235, 245, 248, 90))
    return img


def drip_water() -> Image.Image:
    """Eau du reservoir du goutte-a-goutte : bleu calme, reflets fins."""
    img = new(16)
    rng = random.Random(11)
    for y in range(16):
        for x in range(16):
            base = (64, 132, 208, 255)
            if rng.random() < 0.14:
                base = (96, 165, 250, 255)
            elif rng.random() < 0.06:
                base = (147, 197, 253, 255)
            img.putpixel((x, y), base)
    return img


def jar_weed(kind: str) -> Image.Image:
    """Contenu de la jarre : masse de tetes, teinte selon l'etat."""
    if kind == "curing":
        base = (92, 138, 64, 255)
        variants = [(122, 160, 84, 255), (70, 108, 50, 255), (150, 172, 96, 255)]
    elif kind == "ready":
        base = (150, 138, 74, 255)
        variants = [(178, 162, 90, 255), (122, 110, 58, 255), (196, 150, 70, 255)]
    else:  # moisi
        base = (96, 104, 84, 255)
        variants = [(118, 126, 106, 255), (74, 82, 66, 255)]
    img = noisy(32, base, variants, 0.55, seed=41)
    if kind == "moldy":
        rng = random.Random(43)
        for _ in range(26):
            x, y = rng.randint(0, 31), rng.randint(0, 31)
            put(img, x, y, (222, 226, 214, 255))
            if rng.random() < 0.5:
                put(img, x + 1, y, (198, 204, 190, 255))
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
    #  terreau (monticule de terre, pointe de pousse)
    "soil": [
        "........",
        "...X....",
        "........",
        "..XXX...",
        ".XXXXX..",
        "XXXXXXX.",
        "XX.XX.X.",
        "XXXXXXX.",
    ],
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

SPRAYER_MAP = [
    "................",
    "................",
    "...w............",
    "..w.nNN.........",
    "...w...M........",
    ".......M........",
    "......mMMm......",
    ".....GggggG.....",
    ".....Gg..gG.....",
    ".....GllllG.....",
    ".....GllllG.....",
    ".....GLllLG.....",
    ".....GLLLLG.....",
    "......GGGG......",
    "................",
    "................",
]
SPRAYER_PALETTE = {
    "N": (232, 238, 244, 255),
    "n": (148, 163, 184, 255),
    "M": (203, 213, 225, 255),
    "m": (100, 116, 139, 255),
    "G": (134, 160, 172, 255),
    "g": (196, 219, 226, 255),
    "l": (110, 231, 152, 255),
    "L": (64, 180, 108, 255),
    "w": (125, 211, 252, 255),
}

DRIPPER_MAP = [
    "................",
    "....GGGGG.......",
    "...GwwwwwG......",
    "...GwwwwwG......",
    "...GWwwwWG......",
    "....GGGGG.......",
    "......r.........",
    "......r.........",
    "......rrrrr.....",
    "..........r.....",
    "..........r.....",
    "..........d.....",
    "................",
    "..........d.....",
    "................",
    "................",
]
DRIPPER_PALETTE = {
    "G": (134, 160, 172, 255),
    "w": (96, 165, 250, 255),
    "W": (147, 197, 253, 255),
    "r": (146, 116, 68, 255),
    "d": (125, 211, 252, 255),
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
    save(prime_variant(parts), "block/plant_weed_parts_prime.png")

    # Pot et rack (32x).
    save(pot_side(), "block/pot_side.png")
    save(pot_rim(), "block/pot_rim.png")
    save(pot_soil(), "block/pot_soil.png")
    save(pot_soil_dry(), "block/pot_soil_dry.png")
    save(pot_soil_fert(), "block/pot_soil_fert.png")
    save(pot_bottom(), "block/pot_bottom.png")
    save(can_metal(), "item/watering_can_metal.png")
    save(joint_wrap(), "item/weed_joint_wrap.png")
    save(rack_wood(), "block/rack_wood.png")
    save(rack_rope(), "block/rack_rope.png")
    save(rack_bud(dry=False), "block/rack_bud_fresh.png")
    save(rack_bud(dry=True), "block/rack_bud_dry.png")

    # Jarre de curing : verre, contenus (affinage, pret, moisi).
    save(jar_glass(), "block/jar_glass.png")
    save(drip_water(), "block/drip_water.png")
    save(jar_weed("curing"), "block/jar_weed_curing.png")
    save(jar_weed("ready"), "block/jar_weed_ready.png")
    save(jar_weed("moldy"), "block/jar_weed_moldy.png")

    # Glyphes de font.
    for name, rows in GLYPHS.items():
        save(glyph(rows), f"font/{name}.png")

    # Items.
    save(from_map(SEED_MAP, SEED_PALETTE), "item/weed_seed.png")
    save(from_map(SPRAYER_MAP, SPRAYER_PALETTE), "item/sprayer.png")
    save(from_map(DRIPPER_MAP, DRIPPER_PALETTE), "item/dripper.png")
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
