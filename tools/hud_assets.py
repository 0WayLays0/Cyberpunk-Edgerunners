"""Chrome Neon HUD assets (run from the repo root: python3 tools/hud_assets.py).

Fonts: Rajdhani (titles, values) and Share Tech Mono (data, labels), both SIL OFL 1.1 (licences in tools/fonts).
Minecraft rasterises a TrueType glyph at size x oversample pixels; each style is declared once per GUI scale
(oversample = scale) so the HUD's text maps one texel to one screen pixel: sharp, never pixelated. Each style is
shifted so the top of its capitals sits at y = 0 (Minecraft puts the baseline at y = 7 + shift).

Icons: 42 line icons drawn as vectors at 256 px, reduced to 64 px with antialiasing, white on transparent (the HUD
tints them and draws them with linear filtering). The order is HudIcon.java's."""
import json
import math
import shutil
from pathlib import Path
from fontTools.ttLib import TTFont
from PIL import Image, ImageDraw

ROOT = Path("src/main/resources/assets/chromeneon")
FONTS = Path("tools/fonts")
SCALES = range(1, 7)
# style -> (file, size in GUI pixels)
STYLES = {
    "label": ("share-tech-mono-400", 5.0),
    "data": ("share-tech-mono-400", 6.0),
    "body": ("rajdhani-600", 8.0),
    "title": ("rajdhani-700", 10.0),
    "value": ("rajdhani-700", 14.0),
    "display": ("rajdhani-700", 20.0),
}


def fonts():
    out = ROOT / "font"
    out.mkdir(parents=True, exist_ok=True)
    for old in list(out.glob("*.json")) + list(out.glob("*.ttf")):
        old.unlink()
    for f in {v[0] for v in STYLES.values()}:
        shutil.copy(FONTS / f"{f}.ttf", out / f"{f}.ttf")
    for style, (file, size) in STYLES.items():
        t = TTFont(FONTS / f"{file}.ttf")
        cap = t["OS/2"].sCapHeight / t["head"].unitsPerEm * size
        shift = round(cap - 7.0, 3)
        for s in SCALES:
            provider = {"type": "ttf", "file": f"chromeneon:{file}.ttf", "size": size, "oversample": float(s),
                        "shift": [0.0, shift], "skip": ""}
            data = {"providers": [{"type": "space", "advances": {" ": round(size * 0.28, 2)}}, provider,
                                  {"type": "reference", "id": "minecraft:default"}]}
            (out / f"{style}_{s}.json").write_text(json.dumps(data, indent=1))
    # Old bitmap fonts of the first HUD
    for old in ("hud.png", "hud_accented.png", "micro.png"):
        p = ROOT / "textures/font" / old
        if p.exists():
            p.unlink()


# ---------------------------------------------------------------- icons
N, M, W = 256, 26, 17  # canvas, margin, stroke


def P(x, y):
    return (M + x * (N - 2 * M), M + y * (N - 2 * M))


class Pen:
    def __init__(self):
        self.img = Image.new("L", (N, N), 0)
        self.d = ImageDraw.Draw(self.img)

    def line(self, *pts, w=W, closed=False):
        pts = [P(*p) for p in pts]
        if closed:
            pts.append(pts[0])
        self.d.line(pts, fill=255, width=w, joint="curve")
        for p in (pts[0], pts[-1]):  # square-ish caps
            r = w / 2 - 1
            self.d.rectangle((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=255)

    def poly(self, *pts):
        self.d.polygon([P(*p) for p in pts], fill=255)

    def circle(self, cx, cy, r, w=W, fill=False):
        (x, y), rr = P(cx, cy), r * (N - 2 * M)
        if fill:
            self.d.ellipse((x - rr, y - rr, x + rr, y + rr), fill=255)
        else:
            self.d.ellipse((x - rr, y - rr, x + rr, y + rr), outline=255, width=w)

    def arc(self, cx, cy, r, a0, a1, w=W):
        (x, y), rr = P(cx, cy), r * (N - 2 * M)
        self.d.arc((x - rr, y - rr, x + rr, y + rr), a0, a1, fill=255, width=w)

    def cut(self, *pts):
        self.d.polygon([P(*p) for p in pts], fill=0)

    def done(self):
        return self.img.resize((64, 64), Image.LANCZOS)


def icon(fn):
    p = Pen()
    fn(p)
    return p.done()


def eye(p):
    p.arc(.5, .7536, .4736, 212, 328)
    p.arc(.5, .2464, .4736, 32, 148)
    p.circle(.5, .5, .12, fill=True)


def pick(p):
    p.arc(.5, .62, .48, 215, 325)
    p.line((.5, .16), (.5, .92))


def shield(p):
    p.line((.5, .06), (.88, .2), (.84, .56), (.5, .94), (.16, .56), (.12, .2), closed=True)


def tri(p):
    p.line((.5, .08), (.94, .88), (.06, .88), closed=True)
    p.line((.5, .36), (.5, .62))
    p.circle(.5, .75, .045, fill=True)


ICONS = {
    "HEART_PULSE": lambda p: p.line((0, .56), (.28, .56), (.38, .28), (.52, .84), (.62, .44), (.7, .56), (1, .56)),
    "SHIELD": lambda p: (shield(p), p.line((.5, .28), (.5, .72), w=11)),
    "CHIP": lambda p: (p.line((.24, .16), (.84, .16), (.84, .76), (.76, .84), (.16, .84), (.16, .24), closed=True),
                       p.poly((.38, .38), (.62, .38), (.62, .62), (.38, .62)),
                       [p.line((x, 0), (x, .14), w=10) or p.line((x, .86), (x, 1), w=10) for x in (.32, .5, .68)],
                       [p.line((0, y), (.14, y), w=10) or p.line((.86, y), (1, y), w=10) for y in (.32, .5, .68)]),
    "SPEED": lambda p: (p.line((.1, .16), (.42, .5), (.1, .84)), p.line((.52, .16), (.84, .5), (.52, .84))),
    "SLOW": lambda p: (p.line((.1, .18), (.9, .18)), p.line((.16, .42), (.5, .78), (.84, .42))),
    "STRENGTH": lambda p: (p.line((.14, .5), (.5, .14), (.86, .5)), p.line((.14, .86), (.5, .5), (.86, .86))),
    "WEAKNESS": lambda p: (p.line((.14, .14), (.5, .5), (.86, .14)), p.line((.14, .5), (.5, .86), (.86, .5))),
    "REGEN": lambda p: (p.line((.5, .12), (.5, .88), w=22), p.line((.12, .5), (.88, .5), w=22)),
    "POISON": lambda p: (p.line((.5, .04), (.8, .52), (.72, .8), (.5, .92), (.28, .8), (.2, .52), closed=True),
                         p.circle(.5, .64, .09, fill=True)),
    "WITHER": lambda p: (p.line((.2, .1), (.8, .1), (.9, .5), (.7, .7), (.7, .9), (.3, .9), (.3, .7), (.1, .5), closed=True),
                         p.poly((.28, .38), (.44, .38), (.44, .52), (.28, .52)), p.poly((.56, .38), (.72, .38), (.72, .52), (.56, .52))),
    "FIRE": lambda p: p.line((.5, .02), (.7, .3), (.66, .44), (.82, .4), (.86, .66), (.66, .94), (.34, .94),
                              (.14, .66), (.22, .36), (.38, .48), closed=True),
    "FIRE_SHIELD": lambda p: (shield(p), p.poly((.5, .28), (.62, .48), (.58, .7), (.42, .7), (.38, .48))),
    "EYE": eye,
    "EYE_OFF": lambda p: (eye(p), p.line((.12, .9), (.88, .1))),
    "GHOST": lambda p: ([p.arc(.5, .3, .2, a, a + 40) for a in range(0, 360, 72)],
                        [p.arc(.5, 1.1, .5, a, a + 18) for a in range(200, 340, 36)]),
    "WATER": lambda p: [p.line(*[(x / 10, y + .07 * math.sin(x / 10 * math.pi * 3)) for x in range(11)], w=13)
                        for y in (.22, .5, .78)],
    "JUMP": lambda p: (p.line((.2, .42), (.5, .1), (.8, .42)), p.line((.5, .12), (.5, .7)), p.line((.08, .9), (.92, .9))),
    "FEATHER": lambda p: (p.arc(.5, .5, .42, 180, 360), p.line((.08, .5), (.5, .9), (.92, .5), w=11), p.line((.5, .08), (.5, .9), w=11)),
    "LEVITATE": lambda p: (p.line((.14, .44), (.5, .1), (.86, .44)), p.line((.14, .76), (.5, .42), (.86, .76)),
                           [p.line((x, .94), (x + .14, .94), w=11) for x in (.06, .34, .62)]),
    "HUNGER": lambda p: (p.line((.18, .08), (.18, .38), (.3, .48), (.42, .38), (.42, .08), w=12),
                         p.line((.3, .08), (.3, .92), w=14), p.circle(.7, .6, .22)),
    "NAUSEA": lambda p: p.line(*[(.5 + (t / 40) * .46 * math.cos(t * .42), .5 + (t / 40) * .46 * math.sin(t * .42)) for t in range(4, 41)], w=14),
    "GLOW": lambda p: (p.circle(.5, .5, .2), [p.line((.5 + .32 * math.cos(a), .5 + .32 * math.sin(a)),
                                                    (.5 + .48 * math.cos(a), .5 + .48 * math.sin(a))) for a in [k * math.pi / 4 for k in range(8)]]),
    "LUCK": lambda p: (p.line((.5, .04), (.94, .5), (.5, .96), (.06, .5), closed=True), p.poly((.5, .3), (.7, .5), (.5, .7), (.3, .5))),
    "HASTE": pick,
    "FATIGUE": lambda p: (pick(p), p.line((.62, .62), (.94, .94), w=12), p.line((.94, .62), (.62, .94), w=12)),
    "ABSORB": lambda p: (p.line(*[(.5 + .46 * math.cos(a), .5 + .46 * math.sin(a)) for a in [k * math.pi / 3 + math.pi / 6 for k in range(6)]], closed=True),
                         p.poly(*[(.5 + .2 * math.cos(a), .5 + .2 * math.sin(a)) for a in [k * math.pi / 3 + math.pi / 6 for k in range(6)]])),
    "FREEZE": lambda p: [(p.line((.5 + .48 * math.cos(a), .5 + .48 * math.sin(a)), (.5 - .48 * math.cos(a), .5 - .48 * math.sin(a)), w=12),
                          p.line((.5 + .3 * math.cos(a) + .12 * math.cos(a + 2), .5 + .3 * math.sin(a) + .12 * math.sin(a + 2)),
                                 (.5 + .3 * math.cos(a), .5 + .3 * math.sin(a)),
                                 (.5 + .3 * math.cos(a) + .12 * math.cos(a - 2), .5 + .3 * math.sin(a) + .12 * math.sin(a - 2)), w=10))
                         for a in (math.pi / 2, math.pi / 2 + math.pi / 3 * 2, math.pi / 2 + math.pi / 3 * 4, -math.pi / 2,
                                   -math.pi / 2 + math.pi / 3 * 2, -math.pi / 2 + math.pi / 3 * 4)],
    "AIR": lambda p: (p.circle(.36, .62, .26, w=13), p.circle(.74, .28, .16, w=12), p.circle(.78, .74, .1, w=11)),
    "GOOD": lambda p: (p.circle(.5, .5, .46), p.line((.5, .26), (.5, .74)), p.line((.26, .5), (.74, .5))),
    "BAD": tri,
    "INFO": lambda p: (p.circle(.5, .5, .46), p.line((.5, .44), (.5, .76)), p.circle(.5, .28, .05, fill=True)),
    "CHECK": lambda p: p.line((.08, .52), (.36, .8), (.92, .2), w=20),
    "CROSS": lambda p: (p.line((.12, .12), (.88, .88), w=20), p.line((.88, .12), (.12, .88), w=20)),
    "POWER": lambda p: (p.arc(.5, .54, .4, 300, 600), p.line((.5, .04), (.5, .46))),
    "SCAN": lambda p: ([p.line(a, b, c) for a, b, c in (((0, .3), (0, 0), (.3, 0)), ((.7, 0), (1, 0), (1, .3)),
                                                         ((1, .7), (1, 1), (.7, 1)), ((.3, 1), (0, 1), (0, .7)))],
                       p.circle(.5, .5, .16), p.circle(.5, .5, .04, fill=True)),
    "CALL": lambda p: p.poly((.1, .18), (.3, .06), (.44, .28), (.34, .4), (.6, .66), (.72, .56), (.94, .7), (.82, .9),
                             (.66, .94), (.38, .78), (.2, .6), (.06, .34)),
    "AVATAR": lambda p: (p.circle(.5, .32, .2, fill=True), p.poly((.1, 1), (.18, .7), (.34, .6), (.66, .6), (.82, .7), (.9, 1))),
    "MARKER": lambda p: (p.line((.5, .96), (.16, .5), (.2, .22), (.5, .04), (.8, .22), (.84, .5), closed=True), p.circle(.5, .38, .1, fill=True)),
    "HEART": lambda p: p.line((.5, .9), (.08, .46), (.1, .2), (.3, .08), (.5, .24), (.7, .08), (.9, .2), (.92, .46), closed=True),
    "FLAG": lambda p: (p.line((.16, .04), (.16, .96)), p.line((.16, .1), (.86, .1), (.7, .3), (.86, .5), (.16, .5))),
    "UPLOAD": lambda p: (p.line((.2, .36), (.5, .06), (.8, .36)), p.line((.5, .08), (.5, .64)), p.line((.08, .62), (.08, .92), (.92, .92), (.92, .62))),
    "HAZARD": lambda p: (tri(p), p.line((.06, .88), (.94, .88), w=W)),
}


def icons():
    names = list(ICONS)
    cols = 8
    rows = math.ceil(len(names) / cols)
    atlas = Image.new("RGBA", (cols * 64, rows * 64), (255, 255, 255, 0))
    for i, name in enumerate(names):
        mask = icon(ICONS[name])
        tile = Image.new("RGBA", (64, 64), (255, 255, 255, 0))
        tile.putalpha(mask)
        atlas.paste(tile, ((i % cols) * 64, (i // cols) * 64))
    (ROOT / "textures/gui").mkdir(parents=True, exist_ok=True)
    atlas.save(ROOT / "textures/gui/hud_icons.png")
    return names, atlas.size


if __name__ == "__main__":
    fonts()
    names, size = icons()
    print(len(names), "icons", size)
