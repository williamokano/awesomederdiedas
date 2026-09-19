// Converts the authored exercises.yml files in the german-learning content repo
// into the JSON assets bundled with the app.
//
//   npm --prefix tools/sync-exercises install
//   npm --prefix tools/sync-exercises run sync -- --content ../german-learning
//
// The content repo's own Zod schema is the contract: we bundle and import it
// rather than reimplementing it, so a set the website would reject never reaches
// the app. esbuild resolves the schema's `zod` import against this tool's
// node_modules, so the content repo needs no dependencies installed.

import { execFileSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import * as esbuild from 'esbuild';
import { parse as parseYaml } from 'yaml';

// Bumped whenever the emitted shape changes. The app asserts it matches, so a
// stale asset tree fails a test rather than misparsing at runtime.
const FORMAT_VERSION = 1;

// Types the app can render and auto-grade.
const SUPPORTED_TYPES = new Set([
  'gap-text', 'gap-bank', 'single-choice', 'true-false',
  'matching', 'categorize', 'odd-one-out', 'order', 'table-fill',
]);

// Types deliberately left out: both are self-assessed, with no correct answer to
// grade against. Listing them explicitly means a NEW upstream type is an error
// here, in a human's terminal, rather than a silently missing exercise.
const EXCLUDED_TYPES = new Set(['free-write', 'speaking-prompt']);

// Base fields that live on the envelope. Everything else on an exercise is
// type-specific and goes under `body`.
const ENVELOPE_FIELDS = ['id', 'block', 'title', 'instructions', 'instructionsEn', 'skill', 'track'];
const FLAG_FIELDS = ['caseSensitive', 'strictUmlaut', 'keepPunctuation'];
// Author-only, or implying a capability the app does not have.
const DROPPED_FIELDS = ['notes', 'transcript', 'audioContext', 'recycledFrom', 'audio', 'type'];

function fail(message) {
  console.error(`sync-exercises: ${message}`);
  process.exit(1);
}

function parseArgs(argv) {
  const args = { content: null, out: null, check: false };
  for (let i = 0; i < argv.length; i++) {
    const next = () => {
      const v = argv[i + 1];
      if (v === undefined) fail(`${argv[i]} needs a value`);
      i++;
      return v;
    };
    if (argv[i] === '--content') args.content = next();
    else if (argv[i] === '--out') args.out = next();
    else if (argv[i] === '--check') args.check = true;
    else fail(`unknown argument: ${argv[i]}`);
  }
  if (!args.content) fail('--content <path-to-german-learning> is required');
  return args;
}

/** Bundles the content repo's schema.ts so we validate with the exact same rules the site does. */
async function loadSchema(contentRoot) {
  const schemaPath = path.join(contentRoot, 'web/src/core/content/schema.ts');
  if (!fs.existsSync(schemaPath)) {
    fail(`no schema at ${schemaPath} — is --content pointing at the german-learning repo?`);
  }
  const outfile = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'sync-exercises-')), 'schema.mjs');
  await esbuild.build({
    entryPoints: [schemaPath],
    outfile,
    bundle: true,
    format: 'esm',
    platform: 'node',
    nodePaths: [path.join(import.meta.dirname, 'node_modules')],
    logLevel: 'error',
  });
  return import(pathToFileURL(outfile).href);
}

/** Sets live at <group>/<slug>/exercises.yml, matching the site's own glob. */
function findSetFiles(contentRoot) {
  const found = [];
  for (const group of fs.readdirSync(contentRoot, { withFileTypes: true })) {
    if (!group.isDirectory() || group.name.startsWith('.')) continue;
    const groupDir = path.join(contentRoot, group.name);
    for (const slug of fs.readdirSync(groupDir, { withFileTypes: true })) {
      if (!slug.isDirectory()) continue;
      const file = path.join(groupDir, slug.name, 'exercises.yml');
      if (fs.existsSync(file)) found.push(file);
    }
  }
  return found.sort();
}

/** An exercise is unusable without its clip, whether the audio sits on the exercise or an item. */
function needsAudio(exercise) {
  if (exercise.audio) return true;
  return (exercise.items ?? []).some((item) => item && item.audio);
}

/** The source allows `string | string[]` for any answer; collapsing to arrays removes a union from the Kotlin model. */
function asList(answer) {
  return Array.isArray(answer) ? answer : [answer];
}

/**
 * Splits an authored exercise into the envelope every type shares and the
 * type-specific `body`. Keeping base fields in one place means the Kotlin model
 * declares them once instead of repeating them across nine variants.
 */
function toEnvelope(exercise) {
  const envelope = {};
  for (const field of ENVELOPE_FIELDS) {
    if (exercise[field] !== undefined) envelope[field] = exercise[field];
  }

  const flags = {};
  for (const field of FLAG_FIELDS) {
    if (exercise[field]) flags[field] = true;
  }
  if (Object.keys(flags).length > 0) envelope.flags = flags;

  const body = { type: exercise.type };
  const skip = new Set([...ENVELOPE_FIELDS, ...FLAG_FIELDS, ...DROPPED_FIELDS]);
  for (const [key, value] of Object.entries(exercise)) {
    if (!skip.has(key)) body[key] = value;
  }

  // Only the cloze types have `answers` holding typed answers. `matching.answers` is a
  // left-key -> right-key map, and wrapping those in arrays would corrupt it.
  if (body.type === 'gap-text' || body.type === 'gap-bank') {
    body.answers = Object.fromEntries(Object.entries(body.answers).map(([gap, a]) => [gap, asList(a)]));
  }
  if (body.rows) {
    body.rows = body.rows.map((row) => ({
      ...row,
      cells: row.cells.map((cell) => (cell ? { ...cell, answer: asList(cell.answer) } : null)),
    }));
  }
  if (body.items) {
    body.items = body.items.map((item) => {
      const copy = { ...item };
      delete copy.audio;
      return copy;
    });
  }

  envelope.body = body;
  return envelope;
}

/**
 * "A1/01" -> "a1-01", "SIT/78-auto-kaufen-b1" -> "sit-78-auto-kaufen-b1".
 * A slash in a navigation route argument needs escaping and reads badly in logs,
 * so the app keys on this slug and keeps `lesson` only for display.
 */
function slugOf(lesson) {
  return lesson.toLowerCase().replace(/\//g, '-');
}

// Standalone sets carry an explicit `level`; curriculum sets do not, because
// theirs is implied by the id ("A1/04"). The app browses by level, so derive it.
const CURRICULUM_ID = /^([A-C][12])\//;

function levelOf(set) {
  if (set.level) return set.level;
  const match = CURRICULUM_ID.exec(set.lesson);
  return match ? match[1] : null;
}

/** One entry per block, so the browser can offer a block as a session without loading the set. */
function blockSummaries(exercises) {
  const byBlock = new Map();
  for (const exercise of exercises) {
    const entry = byBlock.get(exercise.block) ?? { block: exercise.block, exerciseCount: 0, types: new Set() };
    entry.exerciseCount++;
    entry.types.add(exercise.body.type);
    byBlock.set(exercise.block, entry);
  }
  return [...byBlock.values()]
    .map((e) => ({ block: e.block, exerciseCount: e.exerciseCount, types: [...e.types].sort() }))
    .sort((a, b) => a.block.localeCompare(b.block));
}

/**
 * Sorted keys and a trailing newline: these files are committed and re-synced, so
 * a re-sync must produce a byte-identical tree, and its diff must be readable.
 */
function stableStringify(value) {
  const sortKeys = (_key, val) =>
    val && typeof val === 'object' && !Array.isArray(val)
      ? Object.fromEntries(Object.keys(val).sort().map((k) => [k, val[k]]))
      : val;
  return `${JSON.stringify(value, sortKeys, 2)}\n`;
}

function sha256(text) {
  return crypto.createHash('sha256').update(text, 'utf8').digest('hex');
}

function contentCommit(contentRoot) {
  try {
    return execFileSync('git', ['-C', contentRoot, 'rev-parse', 'HEAD'], { encoding: 'utf8' }).trim();
  } catch {
    return 'unknown';
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const contentRoot = path.resolve(args.content);
  const outRoot = path.resolve(args.out ?? path.join(import.meta.dirname, '../../app/src/main/assets/exercises'));

  if (!fs.existsSync(contentRoot)) fail(`content path does not exist: ${contentRoot}`);

  const { ExerciseSet } = await loadSchema(contentRoot);
  const files = findSetFiles(contentRoot);
  if (files.length === 0) fail(`no exercises.yml found under ${contentRoot}`);

  const summaries = [];
  const payloads = new Map();
  const bySlug = new Map();
  const stats = { sets: 0, skippedSets: 0, exercises: 0, droppedType: 0, droppedAudio: 0 };
  const problems = [];

  for (const file of files) {
    const relative = path.relative(contentRoot, file);
    let raw;
    try {
      raw = parseYaml(fs.readFileSync(file, 'utf8'));
    } catch (error) {
      problems.push(`${relative}: YAML parse error — ${error.message}`);
      continue;
    }

    const parsed = ExerciseSet.safeParse(raw);
    if (!parsed.success) {
      const issues = parsed.error.issues.slice(0, 3).map((i) => `${i.path.join('.')}: ${i.message}`);
      problems.push(`${relative}: ${issues.join('; ')}`);
      continue;
    }
    const set = parsed.data;

    const kept = [];
    for (const exercise of set.exercises) {
      if (EXCLUDED_TYPES.has(exercise.type)) { stats.droppedType++; continue; }
      if (!SUPPORTED_TYPES.has(exercise.type)) {
        // A type that is neither supported nor deliberately excluded is new
        // upstream. Failing here is the point: shipping it silently would drop
        // exercises, and decoding it on a phone would throw.
        problems.push(`${relative}: ${exercise.id} has unknown type "${exercise.type}" — add it to SUPPORTED_TYPES or EXCLUDED_TYPES`);
        continue;
      }
      if (needsAudio(exercise)) { stats.droppedAudio++; continue; }
      kept.push(toEnvelope(exercise));
    }
    if (kept.length === 0) { stats.skippedSets++; continue; }

    const slug = slugOf(set.lesson);
    if (bySlug.has(slug)) fail(`slug collision: "${set.lesson}" and "${bySlug.get(slug)}" both slug to "${slug}"`);
    bySlug.set(slug, set.lesson);

    const level = levelOf(set);
    payloads.set(`sets/${slug}.json`, stableStringify({
      formatVersion: FORMAT_VERSION,
      id: slug,
      lesson: set.lesson,
      title: set.title,
      level,
      topic: set.topic ?? null,
      category: set.category ?? null,
      summary: set.summary ?? null,
      intro: set.intro ?? null,
      exercises: kept,
    }));

    summaries.push({
      id: slug,
      lesson: set.lesson,
      title: set.title,
      level,
      topic: set.topic ?? null,
      category: set.category ?? null,
      summary: set.summary ?? null,
      blocks: blockSummaries(kept),
    });
    stats.sets++;
    stats.exercises += kept.length;
  }

  if (problems.length > 0) {
    console.error('sync-exercises: refusing to write, these sets have problems:\n');
    for (const line of problems) console.error(`  ${line}`);
    fail(`${problems.length} problem(s) — fix them in the content repo rather than shipping broken data`);
  }

  summaries.sort((a, b) => a.id.localeCompare(b.id));
  payloads.set('index.json', stableStringify({ formatVersion: FORMAT_VERSION, sets: summaries }));

  // Hashing every emitted file lets a plain JVM unit test verify the assets were
  // produced by this script and not hand-edited — a drift check that needs no
  // Node in CI. The commit is recorded so a stale snapshot is visible.
  const fileHashes = {};
  for (const [name, text] of [...payloads].sort((a, b) => a[0].localeCompare(b[0]))) {
    fileHashes[name] = sha256(text);
  }
  payloads.set('manifest.json', stableStringify({
    formatVersion: FORMAT_VERSION,
    sourceRepo: 'williamokano/german-learning',
    sourceCommit: contentCommit(contentRoot),
    setCount: stats.sets,
    exerciseCount: stats.exercises,
    files: fileHashes,
  }));

  if (args.check) {
    const differences = [];
    for (const [name, text] of payloads) {
      const existing = path.join(outRoot, name);
      if (!fs.existsSync(existing) || fs.readFileSync(existing, 'utf8') !== text) differences.push(name);
    }
    if (differences.length > 0) {
      console.error(`sync-exercises: ${differences.length} file(s) differ from a fresh export, e.g.`);
      for (const name of differences.slice(0, 5)) console.error(`  ${name}`);
      fail('assets are out of date — re-run without --check and commit the result');
    }
    console.log('sync-exercises: assets are up to date');
    return;
  }

  fs.rmSync(outRoot, { recursive: true, force: true });
  for (const [name, text] of payloads) {
    const target = path.join(outRoot, name);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, text);
  }

  console.log(`sync-exercises: wrote ${stats.sets} sets, ${stats.exercises} exercises to ${outRoot}`);
  console.log(`  skipped ${stats.droppedType} self-assessed and ${stats.droppedAudio} audio-dependent exercises`);
  console.log(`  skipped ${stats.skippedSets} sets left with nothing usable`);
}

await main();
