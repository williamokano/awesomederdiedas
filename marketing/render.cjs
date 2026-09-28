#!/usr/bin/env node
/*
 * Renders the marketing pages to files with headless Chromium (Playwright) + ffmpeg.
 *
 *   node marketing/render.cjs video                  -> export/der-die-das-promo.mp4   (fun cut)
 *   node marketing/render.cjs keynote                -> export/der-die-das-keynote.mp4 (keynote cut)
 *   node marketing/render.cjs covers                 -> export/*.png
 *   node marketing/render.cjs stills [keynote] 2.8 9 -> export/frame-<t>.png (preview frames, gitignored)
 *   node marketing/render.cjs mux [keynote]          -> swap a fresh soundtrack onto an existing MP4
 *
 * Soundtracks come from soundtrack.py / soundtrack_keynote.py; without one the video gets a
 * silent track.
 *
 * Options: --fps 30   FFMPEG=/path/to/ffmpeg (defaults to `ffmpeg` on PATH)
 * Needs the `playwright` package (npm i -g playwright, then NODE_PATH="$(npm root -g)").
 */
const { chromium } = require('playwright');
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const ROOT = __dirname;
const OUT = path.join(ROOT, 'export');
const url = (file, q = '') => 'file://' + path.join(ROOT, file) + q;

const CUTS = {
  video: { page: 'promo-video.html', out: 'der-die-das-promo.mp4', audio: 'soundtrack.wav', script: 'soundtrack.py' },
  keynote: { page: 'promo-keynote.html', out: 'der-die-das-keynote.mp4', audio: 'soundtrack-keynote.wav', script: 'soundtrack_keynote.py' },
};

const COVERS = [
  // id,                 width, height, output file
  ['feature', 1024, 500, 'play-feature-graphic.png'],
  ['social', 1280, 640, 'github-social-preview.png'],
  ['banner', 1600, 560, 'readme-banner.png'],
  ['shot1', 1080, 1920, 'play-screenshot-1.png'],
  ['shot2', 1080, 1920, 'play-screenshot-2.png'],
  ['shot3', 1080, 1920, 'play-screenshot-3.png'],
  ['shot4', 1080, 1920, 'play-screenshot-4.png'],
];

async function openPage(browser, width, height, target) {
  const page = await browser.newPage({ viewport: { width, height }, deviceScaleFactor: 1 });
  await page.goto(target, { waitUntil: 'networkidle' });
  await page.evaluate(async () => {
    await document.fonts.ready;
    await Promise.all([...document.images].map(i => i.decode().catch(() => {})));
  });
  return page;
}

async function video(browser, fps, cut) {
  const page = await openPage(browser, 1920, 1080, url(cut.page, '?render'));
  const duration = await page.evaluate(() => window.DURATION);
  const frames = Math.round(duration * fps);
  const file = path.join(OUT, cut.out);
  const ff = spawn(process.env.FFMPEG || 'ffmpeg', [
    '-y', '-loglevel', 'error',
    '-f', 'image2pipe', '-framerate', String(fps), '-i', '-',
    ...audioInput(cut),
    '-shortest', '-c:v', 'libx264', '-preset', 'slow', '-crf', '17', '-pix_fmt', 'yuv420p',
    '-c:a', 'aac', '-b:a', '192k', '-movflags', '+faststart', file,
  ], { stdio: ['pipe', 'inherit', 'inherit'] });
  const done = new Promise((res, rej) => ff.on('close', c => (c ? rej(new Error('ffmpeg exited ' + c)) : res())));
  for (let i = 0; i < frames; i++) {
    await page.evaluate(t => window.renderAt(t), i / fps);
    const buf = await page.screenshot({ type: 'png' });
    if (!ff.stdin.write(buf)) await new Promise(r => ff.stdin.once('drain', r));
    if (i % fps === 0) process.stdout.write(`\rframe ${i}/${frames}`);
  }
  ff.stdin.end();
  await done;
  console.log(`\n-> ${path.relative(process.cwd(), file)}`);
}

/** ffmpeg input args for the audio: the generated soundtrack, or silence if it's missing. */
function audioInput(cut) {
  const wav = path.join(OUT, cut.audio);
  if (fs.existsSync(wav)) return ['-i', wav];
  console.warn(`No export/${cut.audio} (run python3 marketing/${cut.script}); using silence.`);
  return ['-f', 'lavfi', '-i', 'anullsrc=channel_layout=stereo:sample_rate=48000'];
}

/** Replace the audio of the rendered MP4 without re-encoding the picture. */
function mux(cut) {
  const file = path.join(OUT, cut.out);
  const tmp = file.replace(/\.mp4$/, '.tmp.mp4');
  return new Promise((res, rej) => {
    const ff = spawn(process.env.FFMPEG || 'ffmpeg', [
      '-y', '-loglevel', 'error', '-i', file, ...audioInput(cut),
      '-map', '0:v', '-map', '1:a', '-c:v', 'copy', '-c:a', 'aac', '-b:a', '192k',
      '-shortest', '-movflags', '+faststart', tmp,
    ], { stdio: 'inherit' });
    ff.on('close', c => {
      if (c) return rej(new Error('ffmpeg exited ' + c));
      fs.renameSync(tmp, file);
      console.log('->', path.relative(process.cwd(), file));
      res();
    });
  });
}

async function stills(browser, times, cut) {
  const page = await openPage(browser, 1920, 1080, url(cut.page, '?render'));
  for (const t of times) {
    await page.evaluate(x => window.renderAt(x), +t);
    const file = path.join(OUT, `frame-${t}.png`);
    await page.screenshot({ path: file });
    console.log('->', path.relative(process.cwd(), file));
  }
}

async function covers(browser, only) {
  for (const [id, w, h, name] of COVERS) {
    if (only.length && !only.includes(id)) continue;
    const page = await openPage(browser, w, h, url('covers.html', '?c=' + id));
    const file = path.join(OUT, name);
    await page.screenshot({ path: file });
    await page.close();
    console.log('->', path.relative(process.cwd(), file));
  }
}

(async () => {
  const args = process.argv.slice(2);
  const fpsIdx = args.indexOf('--fps');
  const fps = fpsIdx >= 0 ? +args.splice(fpsIdx, 2)[1] : 30;
  const [mode = 'all', ...rest] = args;
  fs.mkdirSync(OUT, { recursive: true });
  const cutName = CUTS[rest[0]] ? rest.shift() : 'video';
  if (mode === 'mux') return mux(CUTS[cutName]);
  const browser = await chromium.launch();
  try {
    if (mode === 'covers' || mode === 'all') await covers(browser, rest);
    if (mode === 'stills') await stills(browser, rest, CUTS[cutName]);
    if (mode === 'video' || mode === 'all') await video(browser, fps, CUTS.video);
    if (mode === 'keynote' || mode === 'all') await video(browser, fps, CUTS.keynote);
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
