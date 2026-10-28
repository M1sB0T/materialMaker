/**
 * 生成演示用 PNG 图片（模板封面 + 模拟生成结果）。
 * 纯 Node 实现的最小 PNG 编码器（zlib + CRC32），无需任何依赖。
 * 用法：node scripts/generate-demo-images.mjs
 */
import { deflateSync } from "node:zlib";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const coversDir = join(root, "src/main/resources/static/demo/covers");
const resultsDir = join(root, "src/main/resources/demo/results");

// ---------- PNG 编码 ----------
const crcTable = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (const b of buf) c = crcTable[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, "ascii"), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
}

function encodePng(width, height, rgb) {
  const sig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 2; // color type: truecolor RGB
  const raw = Buffer.alloc(height * (1 + width * 3));
  for (let y = 0; y < height; y++) {
    const rowStart = y * (1 + width * 3);
    raw[rowStart] = 0; // filter: none
    rgb.copy(raw, rowStart + 1, y * width * 3, (y + 1) * width * 3);
  }
  const idat = deflateSync(raw, { level: 9 });
  return Buffer.concat([sig, chunk("IHDR", ihdr), chunk("IDAT", idat), chunk("IEND", Buffer.alloc(0))]);
}

// ---------- 简易画布 ----------
function createCanvas(width, height) {
  const rgb = Buffer.alloc(width * height * 3);
  const set = (x, y, r, g, b) => {
    if (x < 0 || y < 0 || x >= width || y >= height) return;
    const i = (y * width + x) * 3;
    rgb[i] = Math.max(0, Math.min(255, r | 0));
    rgb[i + 1] = Math.max(0, Math.min(255, g | 0));
    rgb[i + 2] = Math.max(0, Math.min(255, b | 0));
  };
  return { width, height, rgb, set };
}

function gradient(c, top, bottom) {
  for (let y = 0; y < c.height; y++) {
    const t = y / (c.height - 1);
    const r = top[0] + (bottom[0] - top[0]) * t;
    const g = top[1] + (bottom[1] - top[1]) * t;
    const b = top[2] + (bottom[2] - top[2]) * t;
    for (let x = 0; x < c.width; x++) c.set(x, y, r, g, b);
  }
}

function circle(c, cx, cy, radius, color) {
  for (let y = Math.floor(cy - radius); y <= cy + radius; y++) {
    for (let x = Math.floor(cx - radius); x <= cx + radius; x++) {
      const d = Math.hypot(x - cx, y - cy);
      if (d <= radius) c.set(x, y, color[0], color[1], color[2]);
    }
  }
}

function rect(c, x0, y0, w, h, color) {
  for (let y = y0; y < y0 + h; y++)
    for (let x = x0; x < x0 + w; x++) c.set(x, y, color[0], color[1], color[2]);
}

function hash(s) {
  let h = 2166136261;
  for (const ch of s) {
    h ^= ch.codePointAt(0);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

const palettes = [
  [[255, 138, 101], [183, 28, 28]],
  [[129, 199, 132], [27, 94, 32]],
  [[100, 181, 246], [13, 71, 161]],
  [[255, 213, 79], [230, 81, 0]],
  [[186, 128, 232], [74, 20, 140]],
  [[77, 208, 225], [0, 96, 100]],
  [[244, 143, 177], [136, 14, 79]],
  [[161, 136, 127], [62, 39, 35]],
];

function coverFor(id, width = 640, height = 400) {
  const p = palettes[hash(id) % palettes.length];
  const c = createCanvas(width, height);
  gradient(c, p[0], p[1]);
  // 装饰圆
  const n = 5 + (hash(id) % 4);
  for (let i = 0; i < n; i++) {
    const h1 = hash(id + ":c" + i);
    circle(
      c,
      (h1 % 100) / 100 * width,
      ((h1 >>> 7) % 100) / 100 * height,
      20 + ((h1 >>> 14) % 60),
      [255, 255, 255].map((v, k) => v * (0.25 + (h1 >>> 21) % 40 / 100))
    );
  }
  // 中央横带 + 标题条
  rect(c, 0, height / 2 - 26, width, 52, [255, 255, 255]);
  rect(c, width / 2 - 90, height / 2 - 10, 180, 20, p[1]);
  // 边框
  rect(c, 0, 0, width, 8, [255, 255, 255]);
  rect(c, 0, height - 8, width, 8, [255, 255, 255]);
  return c;
}

// 模拟生成结果：竖版卡片，带白色相纸边框和四宫格/单图取景框
function resultFor(id, width = 640, height = 860) {
  const p = palettes[hash(id + ":r") % palettes.length];
  const c = createCanvas(width, height);
  gradient(c, p[0], p[1]);
  rect(c, 24, 24, width - 48, height - 48, [255, 255, 255]); // 相纸
  const inner = { x: 56, y: 56, w: width - 112, h: height - 200 };
  const grid = hash(id + ":g") % 2 === 0 ? 2 : 1;
  const cellW = Math.floor((inner.w - (grid - 1) * 12) / grid);
  const cellH = Math.floor((inner.h - (grid - 1) * 12) / grid);
  for (let gy = 0; gy < grid; gy++) {
    for (let gx = 0; gx < grid; gx++) {
      const sub = id + `:${gx},${gy}`;
      const sp = palettes[hash(sub) % palettes.length];
      const x0 = inner.x + gx * (cellW + 12);
      const y0 = inner.y + gy * (cellH + 12);
      const cell = createCanvas(cellW, cellH);
      gradient(cell, sp[0], sp[1]);
      circle(cell, cellW / 2, cellH / 2, Math.min(cellW, cellH) / 4,
        [255, 255, 255].map((v) => v * 0.45));
      for (let y = 0; y < cellH; y++)
        for (let x = 0; x < cellW; x++)
          c.set(x0 + x, y0 + y, ...[cell.rgb[(y * cellW + x) * 3], cell.rgb[(y * cellW + x) * 3 + 1], cell.rgb[(y * cellW + x) * 3 + 2]]);
    }
  }
  // 底部色条（模拟签名区）
  rect(c, 56, height - 120, width - 112, 24, p[1]);
  rect(c, 56, height - 84, (width - 112) * 0.6, 12, [204, 204, 204]);
  return c;
}

const templateIds = [
  "demo-postcard", "demo-avatar", "demo-wallpaper", "demo-meme",
  "demo-photo-frame", "demo-collage", "demo-banner", "demo-badge",
];

mkdirSync(coversDir, { recursive: true });
mkdirSync(resultsDir, { recursive: true });

for (const id of templateIds) {
  const cover = coverFor(id);
  writeFileSync(join(coversDir, `${id}.png`), encodePng(cover.width, cover.height, cover.rgb));
  const result = resultFor(id);
  writeFileSync(join(resultsDir, `${id}.png`), encodePng(result.width, result.height, result.rgb));
  console.log(`generated: ${id} (cover + result)`);
}
console.log("done.");
