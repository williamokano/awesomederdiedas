# Marketing assets

Store and repo artwork for Der Die Das. Everything here is generated from HTML, so text,
colours and timing are easy to change and re-render.

## Files in `export/`

| File | Size | Use it for |
| --- | --- | --- |
| `der-die-das-promo.mp4` | 1920×1080, 31.5 s, 30 fps | Google Play promo video (upload to YouTube, then paste the link into Play Console) |
| `play-feature-graphic.png` | 1024×500 | Play Console → Main store listing → Feature graphic |
| `play-screenshot-1..4.png` | 1080×1920 | Play Console → Phone screenshots |
| `github-social-preview.png` | 1280×640 | GitHub → Settings → General → Social preview |
| `readme-banner.png` | 1600×560 | Header image at the top of the main README |

The video has a silent audio track. For a more upbeat cut, add a royalty-free track in
YouTube Studio's editor (Audio library) after you upload it.

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
