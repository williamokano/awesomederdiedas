#!/usr/bin/env python3
"""
Synthesises the promo video's soundtrack: an upbeat 128 BPM music bed plus sound effects
cued to the animation in promo-video.html. Everything is generated from scratch, so there
is nothing to license.

    python3 marketing/soundtrack.py            -> marketing/export/soundtrack.wav
    node marketing/render.cjs mux              -> puts it on the existing MP4

The cue times below mirror the timeline constants in promo-video.html (SCENES, WIPES,
IRISES, ROUNDS, BURSTS, ...). If you retime the video, move the matching cues here.
Needs numpy and scipy.
"""
import os
import wave

import numpy as np
from scipy.signal import butter, fftconvolve, lfilter

SR = 48000
DUR = 31.5
N = int(SR * DUR)
rng = np.random.default_rng(42)

# ------------------------------------------------------------------ mixing

music = np.zeros((2, N))
sfx = np.zeros((2, N))
send = np.zeros((2, N))  # reverb send


def place(bus, t, sig, gain=1.0, pan=0.0, verb=0.0):
    """Mix mono `sig` into `bus` at time `t` (s), equal-power panned (-1 left … 1 right)."""
    i = int(round(t * SR))
    if i >= N or len(sig) == 0:
        return
    if i < 0:
        sig, i = sig[-i:], 0
    sig = sig[: N - i]
    a = (pan + 1) * np.pi / 4
    for ch, g in enumerate((np.cos(a), np.sin(a))):
        seg = sig * gain * g * np.sqrt(2)
        bus[ch, i:i + len(sig)] += seg
        if verb:
            send[ch, i:i + len(sig)] += seg * verb


def ts(dur):
    return np.arange(int(dur * SR)) / SR


def lp(x, hz, order=2):
    b, a = butter(order, hz / (SR / 2), 'low')
    return lfilter(b, a, x)


def hp(x, hz, order=2):
    b, a = butter(order, hz / (SR / 2), 'high')
    return lfilter(b, a, x)


def bp(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], 'band')
    return lfilter(b, a, x)


def noise(dur):
    return rng.uniform(-1, 1, int(dur * SR))


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def fade(x, a=0.002, r=0.01):
    """Short attack/release ramps so nothing clicks."""
    n = len(x)
    ai, ri = min(n, int(a * SR)), min(n, int(r * SR))
    env = np.ones(n)
    if ai:
        env[:ai] = np.linspace(0, 1, ai)
    if ri:
        env[n - ri:] *= np.linspace(1, 0, ri)
    return x * env

# ------------------------------------------------------------------ instruments


def kick(dur=0.4):
    t = ts(dur)
    f = 48 + 110 * np.exp(-t / 0.035)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.16)
    click = hp(noise(dur), 2000) * np.exp(-t / 0.003) * 0.4
    return fade(np.tanh(1.6 * (body + click)))


def clap(dur=0.3):
    t = ts(dur)
    n = bp(noise(dur), 900, 5000)
    env = np.exp(-t / 0.09)
    for d in (0.0, 0.011, 0.022):  # three quick hand hits
        env += (t >= d) * np.exp(-np.clip(t - d, 0, None) / 0.006) * 1.5
    tone = np.sin(2 * np.pi * 185 * t) * np.exp(-t / 0.04) * 0.4
    return fade(n * env * 0.5 + tone)


def hat(dur=0.08, open_=False):
    t = ts(dur if not open_ else 0.3)
    return fade(hp(noise(len(t) / SR), 7000) * np.exp(-t / (0.018 if not open_ else 0.12)))


def crash(dur=1.8):
    t = ts(dur)
    return fade(hp(noise(dur), 4000) * np.exp(-t / 0.55), r=0.2)


def bass(note, dur):
    t = ts(dur)
    f = midi(note)
    saw = 2 * ((t * f) % 1) - 1
    sub = np.sin(2 * np.pi * f * t)
    env = np.exp(-t / 0.22)
    return fade(lp(saw * 0.6 + sub, 380 + 1400 * 0.5) * env, r=0.02)


def pluck(note, dur=0.5):
    """Marimba-ish chord stab."""
    t = ts(dur)
    f = midi(note)
    x = (np.sin(2 * np.pi * f * t) * np.exp(-t / 0.22)
         + 0.35 * np.sin(2 * np.pi * 4 * f * t) * np.exp(-t / 0.03)
         + 0.2 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / 0.08))
    return fade(x, r=0.05)


def bell(note, dur=0.9, decay=0.35):
    t = ts(dur)
    f = midi(note) if note < 200 else note
    vib = 1 + 0.003 * np.sin(2 * np.pi * 5.5 * t)
    x = (np.sin(2 * np.pi * f * t * vib)
         + 0.5 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / 0.15)
         + 0.25 * np.sin(2 * np.pi * 3.01 * f * t) * np.exp(-t / 0.08))
    return fade(x * np.exp(-t / decay), r=0.1)

# ------------------------------------------------------------------ sound effects


def pop(f=600, dur=0.14):
    """Bubbly 'bloop' for things popping in."""
    t = ts(dur)
    freq = f * (1 + 1.2 * (1 - np.exp(-t / 0.025)))
    return fade(np.sin(2 * np.pi * np.cumsum(freq) / SR) * np.exp(-t / 0.045))


def slam(dur=0.5):
    t = ts(dur)
    low = np.sin(2 * np.pi * np.cumsum(70 + 160 * np.exp(-t / 0.03)) / SR) * np.exp(-t / 0.14)
    grit = lp(noise(dur), 2500) * np.exp(-t / 0.05) * 0.8
    return fade(np.tanh(2 * (low + grit)))


def whoosh(dur=0.7, peak=0.6):
    """Filtered noise swell; `peak` is where in the sweep it is loudest (0..1)."""
    t = ts(dur)
    x = noise(dur)
    lo, hi = lp(x, 700), bp(x, 1500, 6000)
    w = t / dur
    env = np.where(w < peak, (w / peak) ** 2, np.exp(-(w - peak) / 0.12))
    return fade((lo * (1 - w) + hi * w) * env * 1.4, r=0.05)


def click(dur=0.05):
    t = ts(dur)
    return fade(np.sin(2 * np.pi * 1900 * t) * np.exp(-t / 0.008) + hp(noise(dur), 3000) * np.exp(-t / 0.002) * 0.5)


def key(dur=0.04):
    t = ts(dur)
    return fade(bp(noise(dur), 1500, 6000) * np.exp(-t / 0.006) + np.sin(2 * np.pi * 320 * t) * np.exp(-t / 0.01) * 0.4)


def tick(f=2600):
    t = ts(0.03)
    return fade(np.sin(2 * np.pi * f * t) * np.exp(-t / 0.006))


def ding():
    """Correct answer: bright rising arpeggio."""
    out = np.zeros(int(0.9 * SR))
    for i, n in enumerate((79, 84, 88)):  # G5 C6 E6
        s = bell(n, 0.7, 0.25)
        o = int(i * 0.07 * SR)
        out[o:o + len(s)] += s[: len(out) - o]
    return out


def buzz(dur=0.42):
    """Wrong answer: a playful descending 'bwomp'."""
    t = ts(dur)
    f = 190 * np.exp(-t * 1.2)
    sq = np.sign(np.sin(2 * np.pi * np.cumsum(f) / SR)) + np.sign(np.sin(2 * np.pi * np.cumsum(f * 1.02) / SR))
    return fade(lp(sq, 1400) * np.exp(-t / 0.18) * 0.5, r=0.05)


def sparkle(dur=0.8, n=14, seed=0):
    r = np.random.default_rng(seed)
    out = np.zeros(int(dur * SR))
    for _ in range(n):
        s = bell(r.uniform(2200, 5200), 0.25, 0.06) * r.uniform(0.3, 1)
        o = int(r.uniform(0, dur - 0.25) * SR)
        out[o:o + len(s)] += s
    return out


def boing(f=260, dur=0.6):
    t = ts(dur)
    freq = f * (1 + 0.8 * t / dur) * (1 + 0.25 * np.sin(2 * np.pi * 14 * t) * np.exp(-t / 0.2))
    return fade(np.sin(2 * np.pi * np.cumsum(freq) / SR) * np.exp(-t / 0.2), r=0.05)


def riser(dur):
    t = ts(dur)
    f = 180 * 2 ** (3 * t / dur)
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.35
    nz = hp(noise(dur), 2500) * (t / dur) ** 2
    return fade((tone + nz) * (t / dur) ** 1.5, r=0.01)


def sweep(f0, f1, dur):
    t = ts(dur)
    f = f0 * (f1 / f0) ** (t / dur)
    trem = 0.75 + 0.25 * np.sin(2 * np.pi * 16 * t)
    return fade(np.sin(2 * np.pi * np.cumsum(f) / SR) * trem * np.minimum(1, t / 0.1), r=0.05)

# ------------------------------------------------------------------ music bed

BPM = 128
BEAT = 60 / BPM
DROP = 4.3          # first wipe: the groove kicks in here
END_K = 56          # last downbeat (≈30.55 s), under the CTA button
CHORDS = [          # C  G  Am  F: (bass note, chord tones)
    (36, (60, 64, 67)), (43, (59, 62, 67)), (45, (60, 64, 69)), (41, (60, 65, 69)),
]
MELODY = [          # one bar per chord, eighth notes, None = rest
    [76, None, 79, None, 81, 79, 76, None],
    [74, None, 76, 79, None, 76, 74, None],
    [72, None, 76, None, 81, None, 79, 76],
    [77, None, 76, None, 74, None, 72, None],
]
LEAD_ON = [(DROP, 7.6), (16.6, 21.2), (28.0, 30.4)]  # sections where the tune plays


def beat_t(k):
    return DROP + k * BEAT


# Intro (hook): muffled pulse, then a snare build and riser into the drop.
for k in range(-9, 0):
    t = beat_t(k)
    if t < 0:
        continue
    place(music, t, lp(kick(), 900), 0.55)
    place(music, t + BEAT / 2, bass(36, BEAT / 2), 0.25)
roll = [beat_t(-4) + i * BEAT / 4 for i in range(8)] + [beat_t(-2) + i * BEAT / 8 for i in range(16)]
for i, t in enumerate(roll):
    place(music, t, clap(), 0.08 + 0.3 * i / len(roll), pan=0.1)
place(music, DROP - 1.6, riser(1.6), 0.35)

# Groove
for k in range(0, END_K):
    t = beat_t(k)
    bar, pos = divmod(k, 4)
    root, chord = CHORDS[bar % 4]
    place(music, t, kick(), 0.9)
    if pos in (1, 3):
        place(music, t, clap(), 0.45, pan=0.05, verb=0.25)
    place(music, t + BEAT / 2, hat(), 0.18, pan=0.35)
    if k % 2 == 1:
        place(music, t + BEAT * 0.75, hat(), 0.08, pan=-0.3)
    # bass: off-beat eighths, octave jump on the "and" of 4
    place(music, t + BEAT / 2, bass(root + (12 if pos == 3 else 0), BEAT / 2), 0.4)
    # chord stabs on the off-beat
    for j, n in enumerate(chord):
        place(music, t + BEAT / 2, pluck(n, 0.35), 0.1, pan=(j - 1) * 0.4, verb=0.3)
    # lead melody
    if any(a <= t < b for a, b in LEAD_ON):
        for e in (0, 1):
            n = MELODY[bar % 4][pos * 2 + e]
            if n is not None:
                place(music, t + e * BEAT / 2, bell(n, 0.6, 0.22), 0.2, pan=0.15, verb=0.4)

# Downbeat of each new scene gets a crash
for t in (DROP, 16.6, 21.2, 25.4, 28.0):
    k = round((t - DROP) / BEAT)
    place(music, beat_t(k), crash(), 0.25, verb=0.2)

# Ending: big final chord that rings out
fin = beat_t(END_K)
place(music, fin, kick(), 1.0)
place(music, fin, crash(2.5), 0.35, verb=0.3)
for j, n in enumerate((48, 60, 64, 67, 72, 76)):
    place(music, fin, bell(n, 1.0, 0.45), 0.16, pan=(j - 2.5) * 0.2, verb=0.5)

# ------------------------------------------------------------------ SFX cues

# Hook: DER / DIE / DAS slam in, wobble, fall, "Mädchen", punchline
for i, (start, pan) in enumerate(((0.0, -0.5), (0.25, 0.0), (0.5, 0.5))):
    place(sfx, start + 0.22, slam(), 0.55, pan=pan)
    place(sfx, start + 0.2, pop(300 + 120 * i), 0.3, pan=pan)
place(sfx, 1.3, boing(220, 0.8), 0.18)
place(sfx, 2.15, whoosh(0.5, 0.4), 0.35)
place(sfx, 2.35, pop(420, 0.2), 0.5, pan=0.2)
place(sfx, 2.55, whoosh(0.4, 0.7), 0.3, pan=-0.3)
place(sfx, 2.95, slam(0.3), 0.35, pan=-0.3)
place(sfx, 3.1, boing(300, 0.7), 0.4)

# Scene wipes and iris transitions
for at in (4.3, 16.6, 21.2):
    place(sfx, at - 0.55, whoosh(0.85, 0.62), 0.55)
for at in (25.4, 28.0):
    place(sfx, at - 0.4, whoosh(0.6, 0.7), 0.45)

# Logo + title
place(sfx, 4.35, sparkle(0.9, 16, seed=1), 0.25, verb=0.4)
place(sfx, 4.4, pop(500, 0.18), 0.45)
for i in range(3):
    place(sfx, 4.8 + i * 0.16 + 0.1, pop(520 + i * 140), 0.35, pan=(i - 1) * 0.4)

# Gameplay: phone flies in, taps, feedback, card swipes
place(sfx, 7.15, whoosh(0.6, 0.55), 0.4, pan=-0.4)
ROUNDS = [(9.2, True), (11.8, False), (14.4, True)]
for tap, ok in ROUNDS:
    place(sfx, tap, click(), 0.5, pan=-0.35)
    if ok:
        place(sfx, tap + 0.08, ding(), 0.4, pan=-0.3, verb=0.3)
        place(sfx, tap + 0.1, sparkle(0.9, 18, seed=int(tap * 10)), 0.25, pan=-0.3, verb=0.3)
    else:
        place(sfx, tap + 0.08, buzz(), 0.5, pan=-0.35)
for t in (10.4, 13.1):
    place(sfx, t - 0.05, whoosh(0.45, 0.5), 0.3, pan=-0.35)
for t in (7.9, 10.6, 13.3):  # captions
    place(sfx, t + 0.1, pop(700, 0.12), 0.18, pan=0.5)

# Features: counter, CEFR chips, tiles
prev = 0
for i in range(0, 1101):
    t = 16.85 + i / 1100 * 1.1
    p = min(1, (t - 16.85) / 1.1)
    val = round(1086 * (1 - (1 - p) ** 3))
    if val // 60 != prev // 60:
        place(sfx, t, tick(2400 + (val / 1086) * 1200), 0.2)
    prev = val
place(sfx, 17.95, pop(900, 0.2), 0.35)
PENTA = [72, 74, 76, 79, 81, 84]
for i in range(6):
    place(sfx, 18.85 + i * 0.09 + 0.08, pop(midi(PENTA[i]) / 2, 0.12), 0.35, pan=(i - 2.5) * 0.25)
for i in range(3):
    place(sfx, 19.75 + i * 0.14 + 0.1, pop(400 + i * 90, 0.15), 0.35, pan=(i - 1) * 0.5)

# Exercises: phone in, chips, typing, check
place(sfx, 21.3, whoosh(0.6, 0.55), 0.4, pan=0.45)
for i in range(6):
    place(sfx, 22.0 + i * 0.13 + 0.08, pop(560 + i * 60, 0.12), 0.25, pan=-0.5 + i * 0.1)
for i, ans in enumerate(['bin', 'bist', 'ist', 'sind', 'seid', 'sind']):
    start = 22.1 + i * 0.3
    for c in range(len(ans)):
        place(sfx, start + (c + 0.5) * 0.24 / len(ans), key(), 0.3, pan=0.45)
place(sfx, 24.2, click(), 0.5, pan=0.45)
place(sfx, 24.25, ding(), 0.4, pan=0.4, verb=0.3)
place(sfx, 24.35, sparkle(0.9, 20, seed=7), 0.25, pan=0.4, verb=0.3)

# Results: title, ring fill, stats, celebration
place(sfx, 25.65, pop(480, 0.2), 0.4)
place(sfx, 25.95, sweep(300, 1400, 0.95), 0.12, verb=0.2)
for i in range(4):
    place(sfx, 26.2 + i * 0.12 + 0.08, pop(600 + i * 80, 0.12), 0.3, pan=-0.6 if i < 2 else 0.6)
place(sfx, 26.9, ding(), 0.45, verb=0.4)
place(sfx, 26.92, sparkle(1.4, 30, seed=9), 0.35, verb=0.4)
place(sfx, 27.05, pop(800, 0.15), 0.2)

# CTA: logo boing, title pops, button, confetti cannons
place(sfx, 28.2, boing(330, 0.7), 0.4)
for i, (pan, at) in enumerate(((-0.7, 28.5), (0.7, 28.65), (0.6, 28.8))):
    place(sfx, at + 0.15, pop(450 + i * 100, 0.14), 0.25, pan=pan)
for i in range(3):
    place(sfx, 28.6 + i * 0.14 + 0.1, pop(520 + i * 140), 0.35, pan=(i - 1) * 0.4)
place(sfx, 29.65, pop(380, 0.22), 0.5)
for t, pan in ((29.7, -0.8), (29.75, 0.8)):
    place(sfx, t, slam(0.35), 0.3, pan=pan)
    place(sfx, t + 0.05, sparkle(1.2, 22, seed=int(t * 100)), 0.3, pan=pan * 0.8, verb=0.3)

# ------------------------------------------------------------------ master

ir_t = ts(1.4)
ir = rng.standard_normal(len(ir_t)) * np.exp(-ir_t / 0.35)
ir = lp(ir, 5000) / np.sqrt(np.sum(ir ** 2))
wet = np.stack([fftconvolve(send[ch], ir)[:N] for ch in range(2)]) * 0.5

mix = music * 0.55 + sfx * 0.85 + wet
mix = hp(mix, 30)                       # clean up sub rumble
mix /= np.max(np.abs(mix)) + 1e-9
mix = np.tanh(1.6 * mix) / np.tanh(1.6)  # gentle limiter
mix *= 0.89                              # ≈ -1 dBFS peak
tail = int(0.8 * SR)                     # fade out over the final hold
mix[:, -tail:] *= np.linspace(1, 0, tail) ** 2

out = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'export', 'soundtrack.wav')
os.makedirs(os.path.dirname(out), exist_ok=True)
pcm = (np.clip(mix.T, -1, 1) * 32767).astype('<i2')
with wave.open(out, 'wb') as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print('->', os.path.relpath(out))
