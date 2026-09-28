#!/usr/bin/env node
/*
 * Renders the marketing pages to files with headless Chromium (Playwright) + ffmpeg.
 *
 *   node marketing/render.cjs video            -> marketing/export/der-die-das-promo.mp4
 *   node marketing/render.cjs covers           -> marketing/export/*.png
 *   node marketing/render.cjs stills 2.8 9.5   -> marketing/export/frame-<t>.png (preview frames, gitignored)
 *   node marketing/render.cjs mux              -> swaps export/soundtrack.wav onto the existing MP4
 *
 * The soundtrack comes from soundtrack.py (python3 marketing/soundtrack.py); without it the
 * video gets a silent track.
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

async function video(browser, fps) {
  const page = await openPage(browser, 1920, 1080, url('promo-video.html', '?render'));
  const duration = await page.evaluate(() => window.DURATION);
  const frames = Math.round(duration * fps);
  const file = path.join(OUT, 'der-die-das-promo.mp4');
  const ff = spawn(process.env.FFMPEG || 'ffmpeg', [
    '-y', '-loglevel', 'error',
    '-f', 'image2pipe', '-framerate', String(fps), '-i', '-',
    ...audioInput(),
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

const SOUNDTRACK = path.join(OUT, 'soundtrack.wav');

/** ffmpeg input args for the audio: the generated soundtrack, or silence if it's missing. */
function audioInput() {
  if (fs.existsSync(SOUNDTRACK)) return ['-i', SOUNDTRACK];
  console.warn('No export/soundtrack.wav (run python3 marketing/soundtrack.py); using silence.');
  return ['-f', 'lavfi', '-i', 'anullsrc=channel_layout=stereo:sample_rate=48000'];
}

/** Replace the audio of the rendered MP4 without re-encoding the picture. */
function mux() {
  const file = path.join(OUT, 'der-die-das-promo.mp4');
  const tmp = file.replace(/\.mp4$/, '.tmp.mp4');
  return new Promise((res, rej) => {
    const ff = spawn(process.env.FFMPEG || 'ffmpeg', [
      '-y', '-loglevel', 'error', '-i', file, ...audioInput(),
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

async function stills(browser, times) {
  const page = await openPage(browser, 1920, 1080, url('promo-video.html', '?render'));
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
  if (mode === 'mux') return mux();
  const browser = await chromium.launch();
  try {
    if (mode === 'covers' || mode === 'all') await covers(browser, rest);
    if (mode === 'stills') await stills(browser, rest);
    if (mode === 'video' || mode === 'all') await video(browser, fps);
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
