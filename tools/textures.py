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


# ---------------------------------------------------------------- phase 3: implants, chair, blade
def pix(img, pts, c):
    for x, y in pts:
        img.putpixel((x, y), c + (255,))


def from_art(art, palette):
    img = canvas()
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), palette[ch] + (255,))
    return img


PAL = {"k": VOID, "p": PANEL, "s": STEEL, "S": STEEL_HI, "v": VIOLET, "d": VIOLET_DIM, "c": CYAN, "C": CYAN_DIM,
       "w": (235, 232, 245), "o": (255, 138, 31), "r": (200, 30, 60)}

CHRONOS_ART = [
        "................", ".....kkkkkk.....", "...kkSSSSSSkk...", "..kSsdddddddsSk.", "..kSdvvvvvvvdSk.",
        ".kSdvkkkckkkvdSk", ".kSdvkkkckkkvdSk", ".kSdvkkkccckvdSk", ".kSdvkkkkkkkvdSk", ".kSdvkkkkkkkvdSk",
        "..kSdvvvvvvvdSk.", "..kSsdddddddsSk.", "...kkSSSSSSkk...", ".....kkkkkk.....", "......kSSk......", "................"]
# Three models of the accelerator: the ring's colours and the number of notches under the dial tell them apart
CHRONOS_MODELS = {
    "chronos_mk1": ({"v": CYAN, "d": CYAN_DIM, "c": (235, 232, 245)}, 1),
    "chronos_mk2": ({}, 2),
    "chronos_mk3": ({"v": (255, 138, 31), "d": (110, 50, 10), "c": (255, 220, 120), "S": (230, 200, 150)}, 3),
}

IMPLANTS = {
    "reaper_blades": [
        "..............Sw", ".............Swv", "............Swv.", "...........Swv..", "..........Swv...",
        ".........Swv....", "........Swv.....", ".......Swv......", "......Swv.......", ".....Swv........",
        "....Swv.........", "...kSv..........", "..kssk..........", ".kssdk..........", "kssdk...........", ".kk.............."[:16]],
    "spring_tendons": [
        "................", ".....SSSSSS.....", "......kkkk......", "....vvvvvvvv....", "......kkkk......",
        "....cccccccc....", "......kkkk......", "....vvvvvvvv....", "......kkkk......", "....cccccccc....",
        "......kkkk......", "....vvvvvvvv....", "......kkkk......", ".....SSSSSS.....", "....kSSSSSSk....", "................"],
    "carapace_dermis": [
        "................", ".....kkkkkk.....", "....kSSSSSSk....", "...kSsssssssk...", "..kSsSSkkSSsSk..",
        "..kSsSkvvkSsSk..", "..kssSkvvkSssk..", "..kssSSkkSSssk..", "..kSssssssssSk..", "..kSsSSkkSSsSk..",
        "...kSsSkvkSsk...", "....kSssssSk....", ".....kSssSk.....", "......kSSk......", ".......kk.......", "................"],
    "hemostat_pump": [
        "................", "...kk.....kk....", "..kvvk...kvvk...", ".kvvvvk.kvvvvk..", ".kvvwvvkvvvvvk..",
        ".kvvvvvvvvvvvk..", ".kvvvvvvvvvvvk..", "..kvvvvvvvvvk...", "...kvvvvvvvk....", "....kvvvvvk.....",
        ".....kvvvk......", "......kvk..ccc..", ".......k..cCCc..", "..........cCCc..", "...........cc...", "................"],
    "argus_optics": [
        "................", "................", "....kkkkkkkk....", "..kkSSSSSSSSkk..", ".kSSddddddddSSk.",
        "kSddvvvvvvvvddSk", "kSdvvkkkkkkvvdSk", "kSdvkccccwckvdSk", "kSdvkcCCCCckvdSk", "kSdvvkkkkkkvvdSk",
        "kSddvvvvvvvvddSk", ".kSSddddddddSSk.", "..kkSSSSSSSSkk..", "....kkkkkkkk....", "................", "................"],
    "synapse_coprocessor": [
        "................", "...S.S.S.S.S....", "..kkkkkkkkkkk...", ".SkppppppppppkS.", "..kpccpppccpk...",
        ".SkpcCcpcCcpkS.", "..kppcvvvcppk...", ".Skppvvwvvppk S.", "..kppcvvvcppk...", ".SkpcCcpcCcpkS.",
        "..kpccpppccpk...", ".SkppppppppppkS.", "..kkkkkkkkkkk...", "...S.S.S.S.S....", "................", "................"],
    "neural_matrix": [
        "................", "......cCCc......", "....cC.vv.Cc....", "...C..v..v..C...", "..C.v.c..c.v.C..",
        "..c..vCvvCv..c..", ".C..v.c..c.v..C.", ".c.v.CvwwvC.v.c.", ".c.v.CvwwvC.v.c.", ".C..v.c..c.v..C.",
        "..c..vCvvCv..c..", "..C.v.c..c.v.C..", "...C..v..v..C...", "....cC.vv.Cc....", "......cCCc......", "................"],
}


def chair_frame2():
    img = canvas()
    rect(img, 0, 0, 15, 15, (34, 32, 44))
    for i in range(16):
        img.putpixel((i, 0), (70, 68, 86, 255))
        img.putpixel((i, 15), (16, 14, 24, 255))
    for x in (0, 15):
        for y in range(16):
            img.putpixel((x, y), (24, 22, 32, 255))
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img.putpixel((x, y), (90, 88, 108, 255))
    rect(img, 1, 7, 14, 8, (22, 20, 30))
    return img


def chair_pad2():
    img = canvas()
    rect(img, 0, 0, 15, 15, (30, 22, 46))
    for y in range(0, 16, 4):
        rect(img, 0, y, 15, y, (18, 12, 28))
    for y in range(2, 16, 4):
        for x in range(1, 15, 3):
            img.putpixel((x, y), (48, 36, 72, 255))
    rect(img, 7, 0, 8, 15, (20, 14, 32))
    return img


def chair_light():
    img = canvas()
    for x in range(16):
        c = VIOLET if (x // 4) % 2 == 0 else (200, 80, 255)
        rect(img, x, 0, x, 15, c)
    rect(img, 0, 7, 15, 8, CYAN)
    return img


def blade_texture():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            t = x / 31
            base = int(150 + 70 * (1 - abs(t - 0.45) * 2))
            v = max(60, min(235, base - (y % 8 == 0) * 25))
            img.putpixel((x, y), (v - 10, v - 5, v + 12, 255))
    for y in range(32):
        img.putpixel((0, y), VIOLET + (255,))
        img.putpixel((1, y), (210, 140, 255, 255))
    return img


def afterimage_texture():
    """The accelerator's trail: a pale hologram skin, scanlined, on the player model's 64x64 layout."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for y in range(64):
        for x in range(64):
            a = 210 if y % 2 == 0 else 70
            if y % 16 == 7:
                a = 255
            img.putpixel((x, y), (238, 244, 255, a))
    return img


def phase3():
    for name, art in IMPLANTS.items():
        art = [r.ljust(16, ".")[:16] for r in art][:16]
        from_art(art, PAL).save(OUT / "item" / f"{name}.png")
    for name, (colors, notches) in CHRONOS_MODELS.items():
        art = [list(r) for r in CHRONOS_ART]
        for i in range(notches):  # notches on the bottom rim
            art[14][6 + i * 2 - (notches - 1)] = "c"
        from_art(["".join(r) for r in art], {**PAL, **colors}).save(OUT / "item" / f"{name}.png")
    afterimage_texture().save(OUT / "entity" / "afterimage.png")
    chair_frame2().save(OUT / "block" / "ripperdoc_chair_frame.png")
    chair_pad2().save(OUT / "block" / "ripperdoc_chair_pad.png")
    chair_light().save(OUT / "block" / "ripperdoc_chair_light.png")
    (OUT / "entity").mkdir(parents=True, exist_ok=True)
    blade_texture().save(OUT / "entity" / "reaper_blade.png")
    print("phase 3 textures written")


if __name__ == "__main__":
    phase3()
