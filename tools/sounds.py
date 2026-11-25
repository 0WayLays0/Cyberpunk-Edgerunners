"""Chrome Neon's sounds, synthesised from scratch (no samples): run from the repo root, python3 tools/sounds.py.
Writes OGG Vorbis files and sounds.json. Needs numpy and soundfile."""
import json
from pathlib import Path
import numpy as np
import soundfile as sf

SR = 32000
OUT = Path("src/main/resources/assets/chromeneon/sounds")
rng = np.random.default_rng(2077)


def t(d):
    return np.arange(int(SR * d)) / SR


def env(n, a=0.005, r=0.2, shape=3.0):
    x = np.arange(n) / SR
    att = np.clip(x / max(a, 1e-4), 0, 1)
    dec = np.exp(-x / max(r, 1e-4) * shape / 3)
    return att * dec


def sweep(f0, f1, d, wave="sine"):
    x = t(d)
    f = np.geomspace(f0, f1, len(x))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) if wave == "sine" else np.sign(np.sin(ph)) * 0.6


def tone(f, d):
    return np.sin(2 * np.pi * f * t(d))


def noise(d):
    return rng.uniform(-1, 1, int(SR * d))


def lowpass(x, cut):
    a = np.exp(-2 * np.pi * cut / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def highpass(x, cut):
    return x - lowpass(x, cut)


def ring(f, d, decay):
    x = t(d)
    return (np.sin(2 * np.pi * f * x) + 0.5 * np.sin(2 * np.pi * f * 2.76 * x)) * np.exp(-x / decay)


def pad(x, d):
    n = int(SR * d)
    return np.pad(x, (0, max(0, n - len(x))))[:n] if len(x) < n else x


def mix(*parts, d=None):
    n = max(len(p) for p in parts) if d is None else int(SR * d)
    out = np.zeros(n)
    for p in parts:
        out[:min(n, len(p))] += p[:n]
    return out


def at(x, start):
    return np.concatenate([np.zeros(int(SR * start)), x])


def norm(x, peak=0.85):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def crush(x, steps=12, hold=6):
    y = np.round(x * steps) / steps
    return np.repeat(y[::hold], hold)[:len(y)]


SOUNDS = {}

SOUNDS["implant.install"] = lambda: norm(mix(
    sweep(180, 1400, 1.0) * env(int(SR * 1.0), 0.05, 0.9) * 0.5,
    at(sweep(400, 2600, 0.6) * env(int(SR * 0.6), 0.02, 0.5) * 0.3, 0.3),
    at(ring(1800, 0.4, 0.08) * 0.6, 0.05), at(ring(2200, 0.4, 0.06) * 0.5, 0.45), at(ring(2900, 0.5, 0.1) * 0.6, 0.9),
    at(lowpass(noise(0.25), 1200) * env(int(SR * 0.25), 0.01, 0.2) * 0.6, 0.0)))
SOUNDS["implant.remove"] = lambda: norm(mix(
    sweep(1600, 160, 0.9) * env(int(SR * 0.9), 0.02, 0.8) * 0.5,
    at(lowpass(noise(0.2), 600) * env(int(SR * 0.2), 0.002, 0.1), 0.75), at(ring(240, 0.5, 0.12) * 0.7, 0.75)))
SOUNDS["accelerator.on"] = lambda: norm(mix(
    sweep(900, 55, 0.9) * env(int(SR * 0.9), 0.01, 0.9) * 0.7,
    lowpass(noise(0.9), 900) * env(int(SR * 0.9), 0.3, 0.6) * 0.5,
    at(sweep(3000, 800, 0.5) * env(int(SR * 0.5), 0.001, 0.3) * 0.2, 0.0)))
SOUNDS["accelerator.off"] = lambda: norm(mix(
    sweep(55, 700, 0.6) * env(int(SR * 0.6), 0.05, 0.5) * 0.7, lowpass(noise(0.6), 1500) * env(int(SR * 0.6), 0.05, 0.3) * 0.4))
SOUNDS["accelerator.crit"] = lambda: norm(mix(
    highpass(noise(0.05), 3000) * env(int(SR * 0.05), 0.001, 0.04) * 0.8,
    sweep(2400, 600, 0.22, "square") * env(int(SR * 0.22), 0.002, 0.2) * 0.45,
    at(ring(1760, 0.35, 0.08) * 0.4, 0.02)))
SOUNDS["blades.deploy"] = lambda: norm(mix(
    highpass(noise(0.35), 3000) * env(int(SR * 0.35), 0.01, 0.25) * 0.6,
    at(ring(2400, 0.6, 0.18) * 0.7, 0.12), at(ring(3100, 0.5, 0.12) * 0.4, 0.14),
    sweep(300, 900, 0.2, "square") * env(int(SR * 0.2), 0.01, 0.15) * 0.15))
SOUNDS["blades.retract"] = lambda: norm(mix(
    highpass(noise(0.25), 2500) * env(int(SR * 0.25), 0.05, 0.15) * 0.5,
    sweep(900, 250, 0.2, "square") * env(int(SR * 0.2), 0.01, 0.15) * 0.15, at(ring(600, 0.3, 0.05) * 0.6, 0.18)))
SOUNDS["blades.hit"] = lambda: norm(mix(
    highpass(noise(0.18), 1800) * env(int(SR * 0.18), 0.001, 0.06) * 0.9, ring(1900, 0.35, 0.07) * 0.5,
    lowpass(noise(0.12), 400) * env(int(SR * 0.12), 0.001, 0.05) * 0.8))
SOUNDS["blades.lunge"] = lambda: norm(mix(
    lowpass(noise(0.45), 2500) * np.sin(np.linspace(0, np.pi, int(SR * 0.45))) ** 2 * 0.8,
    sweep(200, 1200, 0.4) * env(int(SR * 0.4), 0.1, 0.3) * 0.2))
SOUNDS["tendons.jump"] = lambda: norm(mix(
    sweep(180, 60, 0.18) * env(int(SR * 0.18), 0.002, 0.12) * 0.9,
    highpass(noise(0.3), 2000) * env(int(SR * 0.3), 0.005, 0.15) * 0.4))
SOUNDS["dermis.absorb"] = lambda: norm(mix(
    ring(3400, 0.3, 0.05) * 0.6, ring(2100, 0.3, 0.07) * 0.4,
    highpass(noise(0.2), 4000) * (rng.uniform(0, 1, int(SR * 0.2)) > 0.97) * 0.9))
SOUNDS["dermis.breach"] = lambda: norm(mix(
    highpass(noise(0.5), 2500) * env(int(SR * 0.5), 0.001, 0.2) * 0.7, ring(1300, 0.6, 0.15) * 0.4,
    at(tone(880, 0.12) * 0.4, 0.25), at(tone(660, 0.12) * 0.4, 0.42)))
SOUNDS["pump.inject"] = lambda: norm(mix(
    highpass(noise(0.5), 3500) * env(int(SR * 0.5), 0.02, 0.4) * 0.5, sweep(80, 50, 0.2) * env(int(SR * 0.2), 0.002, 0.1) * 0.9,
    at(sweep(500, 1300, 0.35) * env(int(SR * 0.35), 0.02, 0.3) * 0.35, 0.2)))
SOUNDS["optics.scan"] = lambda: norm(mix(*[at(tone(1200 + 300 * i, 0.05) * env(int(SR * 0.05), 0.002, 0.03) * 0.5, 0.06 * i) for i in range(7)],
                                         at(sweep(800, 3000, 0.3) * env(int(SR * 0.3), 0.01, 0.25) * 0.25, 0.42)))
SOUNDS["optics.zoom"] = lambda: norm(sweep(400, 1100, 0.18, "square") * env(int(SR * 0.18), 0.01, 0.15) * 0.4)
SOUNDS["system.on"] = lambda: norm(mix(tone(880, 0.08) * env(int(SR * 0.08), 0.003, 0.06), at(tone(1320, 0.12) * env(int(SR * 0.12), 0.003, 0.09), 0.09)))
SOUNDS["system.off"] = lambda: norm(mix(tone(1320, 0.08) * env(int(SR * 0.08), 0.003, 0.06), at(tone(660, 0.14) * env(int(SR * 0.14), 0.003, 0.1), 0.09)))
SOUNDS["malfunction"] = lambda: norm(crush(mix(*[at(sweep(rng.uniform(200, 2000), rng.uniform(100, 3000), 0.06, "square")
                                                    * env(int(SR * 0.06), 0.001, 0.05), 0.07 * i) for i in range(8)],
                                               lowpass(noise(0.6), 3000) * 0.25)))
SOUNDS["chair.sit"] = lambda: norm(mix(highpass(noise(0.6), 1500) * env(int(SR * 0.6), 0.05, 0.5) * 0.5,
                                       lowpass(noise(0.2), 300) * env(int(SR * 0.2), 0.002, 0.1) * 0.6))
SOUNDS["ui.hover"] = lambda: norm(tone(2400, 0.03) * env(int(SR * 0.03), 0.001, 0.015), 0.4)
SOUNDS["ui.click"] = lambda: norm(mix(tone(1800, 0.05) * env(int(SR * 0.05), 0.001, 0.02), highpass(noise(0.01), 4000) * 0.5), 0.6)
SOUNDS["ui.error"] = lambda: norm(mix(sweep(220, 200, 0.12, "square") * env(int(SR * 0.12), 0.002, 0.1),
                                      at(sweep(220, 180, 0.15, "square") * env(int(SR * 0.15), 0.002, 0.12), 0.15)), 0.6)

if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    index = {}
    for name, fn in SOUNDS.items():
        file = name.replace(".", "_")
        sf.write(OUT / f"{file}.ogg", fn().astype(np.float32), SR, format="OGG", subtype="VORBIS")
        index[name] = {"sounds": [{"name": f"chromeneon:{file}"}], "subtitle": f"subtitles.chromeneon.{name}"}
    Path("src/main/resources/assets/chromeneon/sounds.json").write_text(json.dumps(index, indent=1))
    print(len(SOUNDS), "sounds")
