#!/usr/bin/env python3
"""Previsualise les models JSON du pack en isometrique, sans lancer le jeu.

Rendu logiciel simple : elements -> quads texturees, z-buffer, ombrage
par face facon Minecraft. Suit la convention de rotation du jeu
(verifiee sur block/lectern.json : angle positif sur +X abaisse le nord).

Usage :
  .venv/bin/python preview_render.py <model...> [-o dossier]
  .venv/bin/python preview_render.py --all -o /tmp/previews
"""

import json
import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
MODELS = ROOT / "assets" / "herbalis" / "models"
TEXTURES = ROOT / "assets" / "herbalis" / "textures"

SCALE = 9.0  # pixels par unite de model
SIZE = 340

# Ombrage directionnel facon Minecraft.
FACE_SHADE = {"up": 1.0, "down": 0.55, "north": 0.8, "south": 0.8,
              "west": 0.65, "east": 0.65}

# Ordre des sommets de chaque face d'une boite (x1,y1,z1)-(x2,y2,z2),
# enonces pour que la texture (u croissant a droite, v croissant en bas)
# soit orientee comme dans le jeu.
FACE_CORNERS = {
    "north": lambda a, b: [(b[0], b[1], a[2]), (a[0], b[1], a[2]),
                           (a[0], a[1], a[2]), (b[0], a[1], a[2])],
    "south": lambda a, b: [(a[0], b[1], b[2]), (b[0], b[1], b[2]),
                           (b[0], a[1], b[2]), (a[0], a[1], b[2])],
    "west": lambda a, b: [(a[0], b[1], a[2]), (a[0], b[1], b[2]),
                          (a[0], a[1], b[2]), (a[0], a[1], a[2])],
    "east": lambda a, b: [(b[0], b[1], b[2]), (b[0], b[1], a[2]),
                          (b[0], a[1], a[2]), (b[0], a[1], b[2])],
    "up": lambda a, b: [(a[0], b[1], a[2]), (b[0], b[1], a[2]),
                        (b[0], b[1], b[2]), (a[0], b[1], b[2])],
    "down": lambda a, b: [(a[0], a[1], b[2]), (b[0], a[1], b[2]),
                          (b[0], a[1], a[2]), (a[0], a[1], a[2])],
}


def rotate_mc(point, axis: str, angle_deg: float, origin):
    """Rotation d'element Minecraft (sens horaire vu depuis +axe)."""
    theta = math.radians(angle_deg)
    c, s = math.cos(theta), math.sin(theta)
    x, y, z = (point[i] - origin[i] for i in range(3))
    match axis:
        case "x":
            y, z = y * c + z * s, -y * s + z * c
        case "y":
            x, z = x * c - z * s, x * s + z * c
        case "z":
            x, y = x * c + y * s, -x * s + y * c
    return (x + origin[0], y + origin[1], z + origin[2])


def camera(point):
    """Vue dimetrique : yaw 45 puis pitch 30, projection orthographique."""
    x, y, z = point[0] - 8, point[1] - 8, point[2] - 8
    # Yaw -45 (on regarde l'angle nord-ouest/sud-est).
    ca, sa = math.cos(math.radians(45)), math.sin(math.radians(45))
    x, z = x * ca - z * sa, x * sa + z * ca
    # Pitch 30 vers le bas.
    cb, sb = math.cos(math.radians(30)), math.sin(math.radians(30))
    y, z = y * cb - z * sb, y * sb + z * cb
    return (x, y, z)


def load_texture(ref: str, textures: dict, cache: dict):
    while ref.startswith("#"):
        ref = textures.get(ref[1:], "")
        if not ref:
            return None
    if ref in cache:
        return cache[ref]
    path = ref.split(":", 1)[1] if ":" in ref else ref
    ns = ref.split(":", 1)[0] if ":" in ref else "herbalis"
    if ns != "herbalis":
        cache[ref] = None  # texture vanilla : aplat gris
        return None
    file = TEXTURES / f"{path}.png"
    img = Image.open(file).convert("RGBA") if file.exists() else None
    cache[ref] = img
    return img


def rotate_uv_corners(uv, rotation: int):
    x1, y1, x2, y2 = uv
    corners = [(x1, y1), (x2, y1), (x2, y2), (x1, y2)]
    shift = (rotation // 90) % 4
    return corners[shift:] + corners[:shift]


def render_model(model_path: Path, out_path: Path) -> None:
    data = json.loads(model_path.read_text())
    textures = data.get("textures", {})
    cache: dict = {}

    width = height = SIZE
    color_buffer = Image.new("RGBA", (width, height), (44, 44, 52, 255))
    pixels = color_buffer.load()
    depth = [[-1e9] * width for _ in range(height)]

    def to_screen(p):
        return (width / 2 + p[0] * SCALE, height / 2 + 22 - p[1] * SCALE, p[2])

    for element in data.get("elements", []):
        a, b = element["from"], element["to"]
        rotation = element.get("rotation")
        for face_name, face in element.get("faces", {}).items():
            corners3d = FACE_CORNERS[face_name](a, b)
            if rotation:
                corners3d = [rotate_mc(p, rotation["axis"], rotation["angle"],
                                       rotation["origin"]) for p in corners3d]
            cam = [camera(p) for p in corners3d]
            screen = [to_screen(p) for p in cam]

            texture = load_texture(face["texture"], textures, cache)
            uv = face.get("uv", [0, 0, 16, 16])
            uv_corners = rotate_uv_corners(uv, face.get("rotation", 0))
            shade = FACE_SHADE[face_name] if element.get("shade", True) else 0.95

            draw_quad(pixels, depth, width, height, screen, uv_corners,
                      texture, shade)

    color_buffer = color_buffer.resize((width * 2, height * 2), Image.NEAREST)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    color_buffer.save(out_path)
    print(f"  {out_path}")


def draw_quad(pixels, depth, width, height, screen, uv_corners,
              texture, shade) -> None:
    """Deux triangles, echantillonnage barycentrique, z-buffer."""
    tris = [((0, 1, 2), (0, 1, 2)), ((0, 2, 3), (0, 2, 3))]
    for indices, uv_idx in tris:
        pts = [screen[i] for i in indices]
        uvs = [uv_corners[i] for i in uv_idx]
        min_x = max(0, int(min(p[0] for p in pts)))
        max_x = min(width - 1, int(max(p[0] for p in pts)) + 1)
        min_y = max(0, int(min(p[1] for p in pts)))
        max_y = min(height - 1, int(max(p[1] for p in pts)) + 1)

        (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = pts
        denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
        if abs(denom) < 1e-9:
            continue
        for py in range(min_y, max_y + 1):
            for px in range(min_x, max_x + 1):
                l0 = ((y1 - y2) * (px + 0.5 - x2)
                      + (x2 - x1) * (py + 0.5 - y2)) / denom
                l1 = ((y2 - y0) * (px + 0.5 - x2)
                      + (x0 - x2) * (py + 0.5 - y2)) / denom
                l2 = 1 - l0 - l1
                if l0 < -0.001 or l1 < -0.001 or l2 < -0.001:
                    continue
                z = l0 * z0 + l1 * z1 + l2 * z2
                if z <= depth[py][px]:
                    continue
                u = l0 * uvs[0][0] + l1 * uvs[1][0] + l2 * uvs[2][0]
                v = l0 * uvs[0][1] + l1 * uvs[1][1] + l2 * uvs[2][1]
                color = sample(texture, u, v)
                if color is None or color[3] < 10:
                    continue
                depth[py][px] = z
                pixels[px, py] = (int(color[0] * shade),
                                  int(color[1] * shade),
                                  int(color[2] * shade), 255)


def sample(texture, u: float, v: float):
    if texture is None:
        return (150, 150, 150, 255)
    tx = int(u / 16 * texture.width)
    ty = int(v / 16 * texture.height)
    tx = max(0, min(texture.width - 1, tx))
    ty = max(0, min(texture.height - 1, ty))
    return texture.getpixel((tx, ty))


def main() -> None:
    args = [a for a in sys.argv[1:]]
    out_dir = Path("/tmp/herbalis-previews")
    if "-o" in args:
        idx = args.index("-o")
        out_dir = Path(args[idx + 1])
        del args[idx:idx + 2]

    if "--all" in args:
        names = sorted(p.stem for p in (MODELS / "block").glob("*.json"))
    else:
        names = args
    if not names:
        print(__doc__)
        return

    for name in names:
        # "item/xxx" pour un model d'item, sinon models/block/.
        model = MODELS / (f"{name}.json" if "/" in name
                          else f"block/{name}.json")
        if not model.exists():
            print(f"  model introuvable : {name}")
            continue
        render_model(model, out_dir / f"{name.replace('/', '_')}.png")


if __name__ == "__main__":
    main()
