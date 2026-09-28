# Marketing assets

Store and repo artwork for Der Die Das. Everything here is generated from HTML, so text,
colours and timing are easy to change and re-render.

## Files in `export/`

| File | Size | Use it for |
| --- | --- | --- |
| `der-die-das-promo.mp4` | 1920×1080, 31.5 s, 30 fps, stereo sound | **Fun cut**: bright, bouncy and full of confetti. Google Play promo video (upload to YouTube, then paste the link into Play Console) |
| `der-die-das-keynote.mp4` | 1920×1080, 28.5 s, 30 fps, stereo sound | **Keynote cut**: dark, calm, product-launch style. Good for YouTube, LinkedIn or the Play listing if you want a more premium feel |
| `play-feature-graphic.png` | 1024×500 | Play Console → Main store listing → Feature graphic |
| `play-screenshot-1..4.png` | 1080×1920 | Play Console → Phone screenshots |
| `github-social-preview.png` | 1280×640 | GitHub → Settings → General → Social preview |
| `readme-banner.png` | 1600×560 | Header image at the top of the main README |

## Sound

Both soundtracks are generated from code, so there is nothing to license. The synth
instruments and the mixer live in `audio.py`.

`soundtrack.py` (fun cut) has two parts:

- **Music:** a 128 BPM groove in C major (C–G–Am–F). A muffled pulse and a riser build up
  during the hook, and the full beat starts at the first wipe. A bell melody plays over the
  logo, features and ending, and the music ends on a final chord under the Google Play button.
- **Sound effects** cued to the animation:
  - slams when the articles crash in
  - whooshes on every scene transition
  - pops as chips, captions and title words appear
  - taps on the buttons, a rising "ding" for correct answers and a "bwomp" for the wrong one
  - ticks while the noun counter runs, keyboard clicks while the exercise is typed
  - confetti sparkles and a sweep while the accuracy ring fills

The mix is about -13 LUFS with peaks at -1 dBFS.

`soundtrack_keynote.py` (keynote cut) is quieter, at about -15 LUFS. A warm pad changes
chord each section and a soft eighth-note pulse runs underneath. Every chapter change gets a
whoosh, a low impact and a triple chime. Gentle UI sounds follow the screen: taps, correct
answers, counters, the split-flap clatter, and a rising chord for the final logo.

```bash
pip install numpy scipy
python3 marketing/soundtrack.py            # -> export/soundtrack.wav (gitignored)
python3 marketing/soundtrack_keynote.py    # -> export/soundtrack-keynote.wav (gitignored)
node marketing/render.cjs mux              # swap audio onto the fun cut without re-rendering frames
node marketing/render.cjs mux keynote      # same for the keynote cut
```

Each script's cue times copy the timeline in its HTML page. If you retime a scene, move
its cues too.

## Keynote cut storyboard

This cut is modelled on a product-launch film. It has one idea per beat, two-line statements
with a grey "ghost" second line, and live UI cards beside the text. A HUD shows a timecode,
the chapter name and a segmented progress bar, and there is one colour flip. Every number
is the app's real data.

| Time | Chapter |
| --- | --- |
| 0–3.3 s | 01 The problem: "Der. Die. Das." / "Every German noun takes one of them." |
| 3.3–6.9 s | 02 The catch: "Mädchen?" / "Neuter. Obviously." with a card picking DAS |
| 6.9–9 s | 03 Meet: logo lockup |
| 9–12.3 s | 04 Tap: a flashcard answering der Hund and die Brücke |
| 12.3–15.6 s | 05 Learn: 1,086 nouns as a stacked der/die/das chart per CEFR level |
| 15.6–18.9 s | 06 Master: an accuracy history chart climbing to 92% |
| 18.9–21.8 s | 07 Beyond: yellow flip, 3,243 exercises in 226 sets, 9 exercise types |
| 21.8–24.9 s | 08 By the numbers: a split-flap 1,086 and stat tiles |
| 24.9–28.5 s | 09 Yours: "Your German. Sorted.", then the logo and "Now on Google Play" |

## Fun cut storyboard

| Time | Scene |
| --- | --- |
| 0–4.3 s | Hook: DER? DIE? DAS? slam in, then "das Mädchen … is neuter?!" |
| 4.3–7.6 s | Logo and title reveal |
| 7.6–16.6 s | Flashcard gameplay: a correct answer (der Hund), a wrong one (das Mädchen), another correct one (die Brücke) |
| 16.6–21.2 s | 1,086 nouns, CEFR levels A1–C2, session sizes, languages, history |
| 21.2–25.4 s | Grammar exercises (a gap-text set being filled in and checked) |
| 25.4–28 s | Results: 92% accuracy ring |
| 28–31.5 s | Call to action: "Now on Google Play" |

## Editing and re-rendering

- `promo-video.html` (fun cut) and `promo-keynote.html` (keynote cut): open either in a browser for a live preview. Space pauses, ←/→ seek,
  and clicking the bar scrubs. Each frame comes from `render(t)`, and the timings are in
  `SCENES`, `ROUNDS`, `WIPES` and `IRISES`.
- `covers.html`: open it without a query string to see all covers in a gallery.
  `?c=feature` (or `social`, `banner`, `shot1`–`shot4`) shows one at its real size.
- `brand.css`: shared colours (DER blue, DIE red, DAS yellow), the phone mockup and fonts.

To render, you need Node, Playwright with Chromium, and an ffmpeg build that includes libx264:

```bash
npm i -g playwright            # and `npx playwright install chromium` if needed
export NODE_PATH="$(npm root -g)"
node marketing/render.cjs covers              # PNGs only
node marketing/render.cjs video               # fun cut MP4 (about 5 minutes)
node marketing/render.cjs keynote             # keynote cut MP4
node marketing/render.cjs stills 3.6 11.3     # single preview frames (add `keynote` for that cut)
FFMPEG=/path/to/ffmpeg node marketing/render.cjs all
```

The phone screens are HTML recreations of `FlashcardScreen`, `GameResultsScreen` and the
exercise session. They use the app's own strings and Material 3 colours. If the UI
changes, update the markup here so the screens still match what users will see.

Fonts: Fredoka and Roboto (both SIL Open Font License 1.1) are included in `fonts/`.
