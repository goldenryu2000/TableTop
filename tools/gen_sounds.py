#!/usr/bin/env python3
"""Synthesizes Tabletop's calm UI sounds into core/feedback res/raw. Stdlib only."""
import math, random, struct, wave
from pathlib import Path

RATE = 44100
OUT = Path(__file__).resolve().parent.parent / "core/feedback/src/main/res/raw"


def env(i, n, attack=0.004, power=3.0):
    t = i / RATE
    a = min(1.0, t / attack) if attack > 0 else 1.0
    return a * (1 - i / n) ** power


def noise(dur, cutoff=0.15, attack=0.004, power=3.0, seed=1):
    rnd = random.Random(seed)
    n = int(dur * RATE)
    out, y = [], 0.0
    for i in range(n):
        y += cutoff * (rnd.uniform(-1, 1) - y)  # one-pole low-pass
        out.append(y * env(i, n, attack, power))
    return out


def tone(freq, dur, attack=0.006, power=2.5, glide=0.0, harmonics=((1, 1.0),)):
    n = int(dur * RATE)
    out, phase = [], 0.0
    for i in range(n):
        f = freq * (1 + glide * i / n)
        phase += 2 * math.pi * f / RATE
        s = sum(a * math.sin(phase * h) for h, a in harmonics)
        out.append(s * env(i, n, attack, power))
    return out


def mix(*parts):
    """parts: (offset_seconds, samples, gain)."""
    length = max(int(o * RATE) + len(s) for o, s, _ in parts)
    out = [0.0] * length
    for o, s, g in parts:
        start = int(o * RATE)
        for i, v in enumerate(s):
            out[start + i] += v * g
    return out


def write(name, samples, peak=0.25):
    m = max(abs(v) for v in samples) or 1.0
    frames = b"".join(struct.pack("<h", int(v / m * peak * 32767)) for v in samples)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(frames)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    write("pickup", noise(0.06, cutoff=0.25, power=4), peak=0.12)
    write("place", mix((0, noise(0.05, cutoff=0.12, power=5, seed=2), 1.0),
                       (0, tone(170, 0.09, power=4), 0.6)), peak=0.22)
    write("flip", noise(0.09, cutoff=0.35, attack=0.03, power=2, seed=3), peak=0.12)
    write("draw", noise(0.07, cutoff=0.3, attack=0.02, power=2.5, seed=4), peak=0.11)
    write("foundation", mix((0, tone(880, 0.45, harmonics=((1, 1.0), (2, 0.15))), 1.0),
                            (0.03, tone(1318.5, 0.4), 0.45)), peak=0.16)
    write("invalid", tone(230, 0.14, glide=-0.2, power=3, harmonics=((1, 1.0), (2, 0.2))), peak=0.14)
    write("deal", mix(*[(k * 0.055, noise(0.05, cutoff=0.3, power=3, seed=10 + k), 1.0) for k in range(7)]), peak=0.1)
    # A pebble dropped into a wooden pit: a bright click over a short hollow knock.
    write("stone", mix((0, noise(0.025, cutoff=0.6, attack=0.001, power=6, seed=20), 0.8),
                       (0, tone(1900, 0.05, attack=0.001, power=6), 0.35),
                       (0, tone(420, 0.08, attack=0.002, power=5), 0.5)), peak=0.13)
    notes = [523.25, 659.25, 783.99, 1046.5]
    write("win", mix(*[(k * 0.14, tone(f, 0.9, harmonics=((1, 1.0), (2, 0.12))), 1.0) for k, f in enumerate(notes)]), peak=0.18)


if __name__ == "__main__":
    main()
