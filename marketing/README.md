# Marketing assets

Store and repo artwork for Der Die Das. Everything here is generated from HTML, so text,
colours and timing are easy to change and re-render.

## Files in `export/`

| File | Size | Use it for |
| --- | --- | --- |
| `der-die-das-promo.mp4` | 1920×1080, 31.5 s, 30 fps, stereo sound | Google Play promo video (upload to YouTube, then paste the link into Play Console) |
| `play-feature-graphic.png` | 1024×500 | Play Console → Main store listing → Feature graphic |
| `play-screenshot-1..4.png` | 1080×1920 | Play Console → Phone screenshots |
| `github-social-preview.png` | 1280×640 | GitHub → Settings → General → Social preview |
| `readme-banner.png` | 1600×560 | Header image at the top of the main README |

## Sound

`soundtrack.py` generates the whole soundtrack from code, so there is nothing to license.
It has two parts:

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

```bash
pip install numpy scipy
python3 marketing/soundtrack.py        # -> export/soundtrack.wav (gitignored)
node marketing/render.cjs mux          # swap it onto the MP4 without re-rendering frames
```

The cue times copy the timeline constants in `promo-video.html`. If you retime a scene,
move its cues in `soundtrack.py` too.

## Video storyboard

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

- `promo-video.html`: open it in a browser for a live preview. Space pauses, ←/→ seek,
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
node marketing/render.cjs video               # MP4 only (about 5 minutes)
node marketing/render.cjs stills 3.6 11.3     # single preview frames
FFMPEG=/path/to/ffmpeg node marketing/render.cjs all
```

The phone screens are HTML recreations of `FlashcardScreen`, `GameResultsScreen` and the
exercise session. They use the app's own strings and Material 3 colours. If the UI
changes, update the markup here so the screens still match what users will see.

Fonts: Fredoka and Roboto (both SIL Open Font License 1.1) are included in `fonts/`.
