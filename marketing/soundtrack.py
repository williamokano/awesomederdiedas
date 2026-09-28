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

from audio import *  # noqa: F403  (instruments, SR, Mix)

DUR = 31.5
mix = Mix(DUR)
music, sfx = mix.music, mix.sfx
place = mix.place

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

mix.master(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'export', 'soundtrack.wav'))
