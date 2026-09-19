# sync-exercises

Converts the authored `exercises.yml` files in the
[german-learning](https://github.com/williamokano/german-learning) content repo into the
JSON assets bundled with the app at `app/src/main/assets/exercises/`.

```bash
npm --prefix tools/sync-exercises install
npm --prefix tools/sync-exercises run sync -- --content ../german-learning
```

The generated assets **are committed** — the app ships them and works offline. Re-run the
script and commit the result whenever the content repo gains exercises worth shipping.

## How it validates

The content repo's own Zod schema (`web/src/core/content/schema.ts`) is the contract. The
script bundles and imports it with esbuild rather than reimplementing it, so a set the
website would reject never reaches the app, and the schema stays single-sourced upstream.
esbuild resolves the schema's `zod` import against this tool's `node_modules`, so the
content repo does not need its own dependencies installed.

Parsing uses the same `yaml` package the site uses. That matters: at least one authored
file (`B2/12-argumentieren-und-eroertern`) is valid for this parser but rejected by
stricter ones, so reimplementing the parse would silently drop a set.

## What it filters out

- **Exercise types the app cannot auto-grade yet** — `free-write` and `speaking-prompt`.
- **Exercises that depend on audio**, whether the clip sits on the exercise or on an item.
  Filtering by type alone is not enough: a `single-choice` listening question would
  otherwise ship as an unanswerable question with no sound.
- Author-only fields (`notes`, `transcript`, `audioContext`, `recycledFrom`).

Answers are normalised from the source's `string | string[]` union to plain arrays, which
keeps the Kotlin model free of a custom serializer.

A type that is neither supported nor explicitly excluded is a **hard error**. A tenth
upstream exercise type should stop the sync in a terminal, not vanish from the app or throw
while decoding on someone's phone.

## Output

```
app/src/main/assets/exercises/
  manifest.json  # formatVersion, sourceCommit, counts, sha256 of every emitted file
  index.json     # one summary per set, with per-block counts so the browser can
                 # offer a block as a session without loading the set
  sets/*.json    # one file per set, named after its slugged id
```

Set ids are slugged (`A1/01` -> `a1-01`): a slash in a navigation route argument needs
escaping and reads badly in logs, so the app keys on the slug and keeps `lesson` for
display.

Each exercise is emitted as a shared envelope plus a type-specific `body`, so the Kotlin
model declares the base fields once instead of repeating them across nine variants.

Output is deterministic — sorted keys, pretty-printed, trailing newline — so re-running over
the same content produces a byte-identical tree and a reviewable diff. The indentation costs
nothing in the APK, which compresses to roughly 1.1 MB either way.

`manifest.json` records the german-learning commit the assets came from, so a stale snapshot
is visible, and a sha256 per file so a plain JVM unit test can verify the tree was produced
by this script and not hand-edited.

## Checking for drift

```bash
npm --prefix tools/sync-exercises run sync -- --content ../german-learning --check
```

Re-exports in memory and fails if anything differs from what is committed, without writing.
