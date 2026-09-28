"""Tiny synth + mixer shared by the promo soundtracks (numpy + scipy only)."""
import os
import wave

import numpy as np
from scipy.signal import butter, fftconvolve, lfilter

SR = 48000
rng = np.random.default_rng(42)

# ------------------------------------------------------------------ mixing


class Mix:
    """Stereo buses for one soundtrack: music, sfx and a shared reverb send."""

    def __init__(self, dur):
        self.n = int(SR * dur)
        self.music = np.zeros((2, self.n))
        self.sfx = np.zeros((2, self.n))
        self.send = np.zeros((2, self.n))

    def place(self, bus, t, sig, gain=1.0, pan=0.0, verb=0.0):
        """Mix mono `sig` into `bus` at time `t` (s), equal-power panned (-1 left … 1 right)."""
        n = self.n
        i = int(round(t * SR))
        if i >= n or len(sig) == 0:
            return
        if i < 0:
            sig, i = sig[-i:], 0
        sig = sig[: n - i]
        a = (pan + 1) * np.pi / 4
        for ch, g in enumerate((np.cos(a), np.sin(a))):
            seg = sig * gain * g * np.sqrt(2)
            bus[ch, i:i + len(sig)] += seg
            if verb:
                self.send[ch, i:i + len(sig)] += seg * verb

    def master(self, path, music=0.55, sfx=0.85, reverb=0.5, fade_out=0.8, peak=0.89, drive=1.6):
        """Reverb, limit, fade and write a 16-bit stereo WAV."""
        ir_t = ts(1.4)
        ir = rng.standard_normal(len(ir_t)) * np.exp(-ir_t / 0.35)
        ir = lp(ir, 5000) / np.sqrt(np.sum(ir ** 2))
        wet = np.stack([fftconvolve(self.send[ch], ir)[: self.n] for ch in range(2)]) * reverb
        mix = self.music * music + self.sfx * sfx + wet
        mix = hp(mix, 30)                           # clean up sub rumble
        mix /= np.max(np.abs(mix)) + 1e-9
        mix = np.tanh(drive * mix) / np.tanh(drive)  # gentle limiter
        mix *= peak
        tail = int(fade_out * SR)
        mix[:, -tail:] *= np.linspace(1, 0, tail) ** 2
        os.makedirs(os.path.dirname(path), exist_ok=True)
        pcm = (np.clip(mix.T, -1, 1) * 32767).astype('<i2')
        with wave.open(path, 'wb') as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        print('->', os.path.relpath(path))


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
