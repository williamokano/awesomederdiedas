#!/usr/bin/env python3
"""
Soundtrack for the keynote cut (promo-keynote.html): a calm launch-film score with a warm
pad, a soft eighth-note pulse, and a whoosh, impact and triple chime on every chapter change.
UI sounds follow what happens on screen: taps, correct answers, counters, the split flap.

    python3 marketing/soundtrack_keynote.py    -> marketing/export/soundtrack-keynote.wav
    node marketing/render.cjs mux keynote      -> puts it on the existing keynote MP4

Cue times mirror promo-keynote.html (CHAPTERS, TAPS and the reveal times in render()).
"""
import os

from audio import *  # noqa: F403  (instruments, SR, Mix)

DUR = 28.5
mix = Mix(DUR)
music, sfx = mix.music, mix.sfx
place = mix.place

CHAPTERS = [3.3, 6.9, 9.0, 12.3, 15.6, 18.9, 21.8, 24.9]
LOCKUP = 26.6


def pad(notes, dur, attack=0.8, release=1.0):
    """Warm detuned-saw pad, low-passed, with slow attack and release."""
    t = ts(dur)
    x = np.zeros(len(t))
    for n in notes:
        f = midi(n)
        for d in (-0.08, 0.0, 0.08):  # detune in semitones
            fd = f * 2 ** (d / 12)
            x += 2 * ((t * fd + rng.random()) % 1) - 1
    x = lp(x / (3 * len(notes)), 900, order=2)
    env = np.minimum(1, t / attack) * np.minimum(1, (dur - t) / release).clip(0)
    return x * env


def blip(f, dur=0.12):
    t = ts(dur)
    return fade(np.sin(2 * np.pi * f * t) * np.exp(-t / 0.035))


def chime3(t0, pan=0.0, gain=0.22):
    """The reference's signature: three quick, identical high blips."""
    for i in range(3):
        place(sfx, t0 + i * 0.075, blip(1568), gain, pan=pan, verb=0.35)


def impact(dur=0.9):
    t = ts(dur)
    body = np.sin(2 * np.pi * np.cumsum(42 + 60 * np.exp(-t / 0.05)) / SR) * np.exp(-t / 0.35)
    air = lp(noise(dur), 1800) * np.exp(-t / 0.12) * 0.25
    return fade(body + air, r=0.1)


# ------------------------------------------------------------------ music

# Pad chords per section (MIDI notes): Cmaj9 → Am9 → Fmaj7 → G6 → Cadd9
SECTIONS = [
    (0.3, 6.9, (48, 55, 64, 71, 74)),
    (6.9, 12.3, (45, 52, 60, 67, 71)),
    (12.3, 18.9, (41, 48, 57, 64, 67)),
    (18.9, 24.9, (43, 50, 59, 64, 69)),
    (24.9, DUR, (48, 55, 62, 64, 67, 72)),
]
for i, (a, b, notes) in enumerate(SECTIONS):
    place(music, a - (0.3 if i else 0), pad(notes, b - a + 0.9, attack=0.9 if i == 0 else 0.35), 0.5, verb=0.3)

# Eighth-note pulse on the chord root, 84 BPM; soft kick once the product appears
BEAT = 60 / 84
k = 0
while (t := 0.6 + k * BEAT / 2) < DUR - 1.5:
    root = next(n for a, b, n in SECTIONS if a - 0.3 <= t < b)[0] + 24
    breath = 24.9 <= t < LOCKUP  # drop the pulse for "Your German. Sorted."
    if not breath:
        place(music, t, lp(pluck(root, 0.3), 1600), 0.16 if k % 2 == 0 else 0.1, pan=0.2 if k % 2 else -0.2)
        if 9.0 <= t < 24.9 and k % 2 == 0:
            place(music, t, lp(kick(), 400), 0.35)
    k += 1

# Final rising chord stack under the lockup
for i, n in enumerate((60, 64, 67, 72, 76, 79)):
    place(music, LOCKUP + i * 0.09, bell(n, 2.2, 0.9), 0.14, pan=(i - 2.5) * 0.25, verb=0.6)

# ------------------------------------------------------------------ chapter hits

for t in CHAPTERS + [LOCKUP]:
    big = t in (18.9, LOCKUP)
    place(sfx, t - 0.55, whoosh(0.75, 0.72), 0.32 if big else 0.2)
    place(sfx, t, impact(), 0.55 if big else 0.38)
    chime3(t + 0.1)

# ------------------------------------------------------------------ UI sounds

place(sfx, 0.05, blip(880, 0.3), 0.25, verb=0.4)                          # dot
for i, n in enumerate((72, 76, 79)):                                       # Der. Die. Das.
    place(sfx, 0.55 + i * 0.32, bell(n, 0.8, 0.3), 0.2, pan=(i - 1) * 0.5, verb=0.4)
place(sfx, 1.9, sparkle(0.6, 10, seed=3), 0.12, verb=0.4)                   # words take their colours

place(sfx, 4.2, tick(1800), 0.12, pan=0.5)                                 # hover DIE
place(sfx, 4.75, click(), 0.35, pan=0.55)                                  # pick DAS
place(sfx, 4.85, bell(88, 0.6, 0.2), 0.14, pan=0.55, verb=0.3)
place(sfx, 4.93, bell(91, 0.6, 0.2), 0.14, pan=0.55, verb=0.3)

for tap in (10.2, 11.55):                                                  # Tap.
    place(sfx, tap - 0.1, click(), 0.35, pan=0.5)
    place(sfx, tap + 0.05, bell(88, 0.6, 0.2), 0.14, pan=0.5, verb=0.3)
    place(sfx, tap + 0.13, bell(91, 0.6, 0.2), 0.14, pan=0.5, verb=0.3)
place(sfx, 11.25, whoosh(0.35, 0.5), 0.1, pan=0.5)

prev = 0                                                                   # Learn. counter
for i in range(1600):
    t = 13.0 + i / 1000
    p = min(1.0, (t - 13.0) / 1.6)
    val = round(1086 * (1 - 2 ** (-10 * p)))
    if val // 45 != prev // 45:
        place(sfx, t, tick(2200 + val), 0.07, pan=0.45)
    prev = val
for i, n in enumerate((72, 74, 76, 79, 81, 84)):                           # bars
    place(sfx, 13.1 + i * 0.12 + 0.2, blip(midi(n), 0.15), 0.1, pan=0.2 + i * 0.08)

place(sfx, 16.3, sweep(500, 1300, 1.7), 0.05, pan=0.45, verb=0.2)          # Master. chart
place(sfx, 18.0, bell(84, 0.6, 0.25), 0.12, pan=0.5, verb=0.3)

for i in range(9):                                                         # exercise types
    place(sfx, 19.9 + i * 0.07 + 0.05, pop(700 + i * 40, 0.1), 0.12, pan=-0.6 + i * 0.15)

step = 0.055                                                               # split flap
for i in range(5):
    settle = 22.4 + i * 0.16
    t = 22.0
    while t < settle:
        place(sfx, t, key(), 0.07, pan=(i - 2) * 0.3)
        t += step
    place(sfx, settle, click(), 0.22, pan=(i - 2) * 0.3)
for i in range(4):                                                         # stat tiles
    place(sfx, 23.5 + i * 0.1 + 0.1, blip(1046 + i * 130, 0.12), 0.12, pan=(i - 1.5) * 0.4)

place(sfx, 25.45, bell(79, 1.2, 0.5), 0.14, verb=0.5)                      # "Sorted."

mix.master(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'export', 'soundtrack-keynote.wav'),
           music=0.7, sfx=0.8, reverb=0.6, fade_out=1.2, drive=1.2, peak=0.8)
