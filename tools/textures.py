"""Generates Chrome Neon's 16x16 textures (run from the repo root: python3 tools/textures.py).
Kept as code so the palette stays the one of the HUD (client/hud/NeonPalette.java)."""
from pathlib import Path
from PIL import Image

OUT = Path("src/main/resources/assets/chromeneon/textures")
VOID, PANEL, STEEL, STEEL_HI = (7, 6, 13), (24, 19, 40), (52, 50, 66), (92, 90, 110)
VIOLET, VIOLET_DIM, CYAN, CYAN_DIM = (176, 38, 255), (74, 20, 112), (30, 242, 255), (11, 74, 85)


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), c + (255,))


def neural_chip():
    img = canvas()
    for i in range(4, 12, 2):  # pins on all four sides
        for p in ((i, 2), (i, 13), (2, i), (13, i)):
            img.putpixel(p, STEEL_HI + (255,))
    rect(img, 3, 3, 12, 12, PANEL)
    for x, y in ((3, 3), (12, 12)):  # cut corners
        img.putpixel((x, y), (0, 0, 0, 0))
    rect(img, 5, 5, 10, 10, VIOLET_DIM)
    rect(img, 6, 6, 9, 9, VIOLET)
    rect(img, 7, 7, 8, 8, CYAN)
    return img


def diagnostic_scanner():
    img = canvas()
    rect(img, 4, 1, 11, 14, STEEL)
    rect(img, 4, 1, 4, 1, VOID); img.putpixel((4, 1), (0, 0, 0, 0)); img.putpixel((11, 14), (0, 0, 0, 0))
    rect(img, 5, 2, 10, 8, VOID)
    for x in range(5, 11):  # a scan line and a bracket
        img.putpixel((x, 5), CYAN + (255,))
    for p in ((5, 2), (6, 2), (5, 3), (10, 8), (9, 8), (10, 7)):
        img.putpixel(p, VIOLET + (255,))
    for x in (6, 8):
        rect(img, x, 10, x + 1, 11, VIOLET_DIM)
    rect(img, 6, 13, 9, 13, CYAN_DIM)
    return img


def chair_frame():
    img = canvas()
    rect(img, 0, 0, 15, 15, STEEL)
    for i in range(16):
        img.putpixel((i, 0), STEEL_HI + (255,)); img.putpixel((0, i), STEEL_HI + (255,))
        img.putpixel((i, 15), VOID + (255,)); img.putpixel((15, i), VOID + (255,))
    rect(img, 1, 7, 14, 7, VIOLET)  # neon strip
    return img


def chair_pad():
    img = canvas()
    rect(img, 0, 0, 15, 15, PANEL)
    for y in range(1, 16, 3):
        rect(img, 1, y, 14, y, VOID)
    rect(img, 7, 0, 8, 15, CYAN_DIM)
    return img


if __name__ == "__main__":
    for folder, name, fn in (("item", "neural_chip", neural_chip), ("item", "diagnostic_scanner", diagnostic_scanner),
                             ("block", "ripperdoc_chair_frame", chair_frame), ("block", "ripperdoc_chair_pad", chair_pad)):
        (OUT / folder).mkdir(parents=True, exist_ok=True)
        fn().save(OUT / folder / f"{name}.png")
        print("wrote", folder, name)
