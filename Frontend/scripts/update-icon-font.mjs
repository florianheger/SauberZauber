// Downloads a subset of the Material Symbols Rounded font that contains only the icons
// listed in src/app/tasks/icon-catalog.ts, and stores it in public/fonts for self-hosting.
import { readFile, writeFile } from 'node:fs/promises';

const catalog = await readFile(new URL('../src/app/tasks/icon-catalog.ts', import.meta.url), 'utf8');
const names = [...new Set([...catalog.matchAll(/\['([a-z0-9_]+)',/g), ...catalog.matchAll(/^\s+'([a-z0-9_]+)',$/gm)].map((m) => m[1]))].sort();

const cssUrl =
  'https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@20..48,300..600,0..1,0' +
  `&icon_names=${names.join(',')}&display=block`;
const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130 Safari/537.36';
const css = await (await fetch(cssUrl, { headers: { 'User-Agent': userAgent } })).text();
const fontUrl = css.match(/url\((https:[^)]+)\)/)?.[1];
if (!fontUrl) throw new Error(`No font URL in response:\n${css}`);

const font = Buffer.from(await (await fetch(fontUrl)).arrayBuffer());
await writeFile(new URL('../public/fonts/material-symbols-rounded.woff2', import.meta.url), font);
console.log(`${names.length} icons, ${(font.length / 1024).toFixed(1)} kB`);
