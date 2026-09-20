#!/usr/bin/env python3
"""Play Console'un istediği iki görseli üretir: simge ve öne çıkan görsel.

Kaynak, uygulamanın kendi simgesidir: `res/drawable/ic_launcher_foreground.xml`
içindeki tetromino bloklarından kurulu "Z" harfi, `res/values/colors.xml`
içindeki arka plan rengiyle. Değerler oradan okunur, elle kopyalanmaz — simge
değişirse görseller de değişir.

Çıktılar (store/graphics/):
  icon-512.png      512×512, 32 bit. Play kendi maskesini uygular
  feature-1024.png  1024×500. Metin yok: 14 dilin hepsinde aynı görsel geçerli

SVG'ler de yanına yazılır; PNG'yi Chromium'un başsız kipi çizer (depoda
raster araç yok). Chromium yolu ZA_CHROME ile verilebilir.

Kullanım: python3 tools/gen_store_graphics.py
"""
import os
import re
import shutil
import subprocess
import sys
import zlib

OUT = 'store/graphics'
FOREGROUND = 'app/src/main/res/drawable/ic_launcher_foreground.xml'
COLORS = 'app/src/main/res/values/colors.xml'

# Uyarlanır simgenin 108 birimlik tuvalinde görünen alan ortadaki 72 birim;
# mağaza simgesi o alanın tamamını kaplar.
CANVAS = 108.0
VISIBLE = 72.0

# headless_shell öncelikli: tam pencere kipindeki Chromium tuvali istenen
# yüksekliğe getirmeyip altını kırpıyor. Kırpma sessizdir, o yüzden çizim
# ayrıca işaret rengiyle denetlenir (aşağıda).
CHROME_CANDIDATES = [
    os.environ.get('ZA_CHROME'),
    '/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell',
    shutil.which('headless_shell'),
    '/opt/pw-browsers/chromium-1194/chrome-linux/chrome',
    shutil.which('chromium'),
    shutil.which('chromium-browser'),
    shutil.which('google-chrome'),
]

# Sayfanın zemini: çizimde hiç kullanılmayan bir renk. Çıktıda görünürse
# çizim tuvali kaplamamış demektir.
MARKER = (255, 0, 255)


def icon_source():
    """Simge çiziminden (renk, blok boyu, blok köşeleri) üçlüsü."""
    src = open(FOREGROUND, encoding='utf-8').read()
    color = re.search(r'android:fillColor="(#[0-9A-Fa-f]+)"', src).group(1)
    data = re.search(r'android:pathData="([^"]+)"', src).group(1)
    cells = [(float(x), float(y)) for x, y in re.findall(r'M([\d.]+),([\d.]+)h', data)]
    size = float(re.search(r'h([\d.]+)v', data).group(1))
    background = re.search(r'name="ic_launcher_background">(#[0-9A-Fa-f]+)<', 
                           open(COLORS, encoding='utf-8').read()).group(1)
    return background, color, cells, size


def blocks(cells, size, scale, dx, dy):
    """Hücreleri hedef ölçeğe taşınmış <rect> dizisine çevirir."""
    return '\n'.join(
        f'  <rect x="{x * scale + dx:.2f}" y="{y * scale + dy:.2f}" '
        f'width="{size * scale:.2f}" height="{size * scale:.2f}"/>'
        for x, y in cells)


def icon_svg(background, color, cells, size):
    side = 512
    scale = side / VISIBLE
    xs = [x for x, _ in cells]
    ys = [y for _, y in cells]
    span = (max(xs) - min(xs) + size) * scale
    dx = (side - span) / 2 - min(xs) * scale
    dy = (side - (max(ys) - min(ys) + size) * scale) / 2 - min(ys) * scale
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{side}" height="{side}">\n'
            f'  <rect width="{side}" height="{side}" fill="{background}"/>\n'
            f'  <g fill="{color}">\n{blocks(cells, size, scale, dx, dy)}\n  </g>\n'
            f'</svg>\n')


def feature_svg(background, color, cells, size):
    """1024×500 öne çıkan görsel. Marka adından başka metin yok: tek görsel
    14 dilin hepsinde geçerli olsun diye.

    Yazının genişliği `textLength` ile çivilenir; böylece DejaVu kurulu
    olmayan bir makinede de yerleşim aynı çıkar, yazı tipi değişse bile
    taşma olmaz.
    """
    w, h = 1024, 500
    mark = 186.0
    xs = [x for x, _ in cells]
    ys = [y for _, y in cells]
    scale = mark / ((max(xs) - min(xs)) + size)
    dx = 150 - min(xs) * scale
    dy = (h - (max(ys) - min(ys) + size) * scale) / 2 - min(ys) * scale
    text_x, text_w = 396, 470
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}">
  <defs>
    <radialGradient id="glow" cx="24%" cy="50%" r="58%">
      <stop offset="0" stop-color="{color}" stop-opacity="0.17"/>
      <stop offset="1" stop-color="{color}" stop-opacity="0"/>
    </radialGradient>
    <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
      <path d="M40,0 V40 H0" fill="none" stroke="#ffffff" stroke-opacity="0.04" stroke-width="1"/>
    </pattern>
  </defs>
  <rect width="{w}" height="{h}" fill="{background}"/>
  <rect width="{w}" height="{h}" fill="url(#grid)"/>
  <rect width="{w}" height="{h}" fill="url(#glow)"/>
  <g fill="{color}">
{blocks(cells, size, scale, dx, dy)}
  </g>
  <text x="{text_x}" y="258" textLength="{text_w}" lengthAdjust="spacingAndGlyphs"
        font-family="DejaVu Sans, Liberation Sans, Arial, sans-serif"
        font-size="76" font-weight="bold" fill="#F4F7FB">ZA GAMES</text>
  <rect x="{text_x}" y="292" width="{text_w}" height="6" fill="{color}"/>
</svg>
'''


PAGE = """<!doctype html><meta charset="utf-8">
<style>html,body{{margin:0;padding:0;overflow:hidden;background:rgb{marker}}}
svg{{display:block}}</style>
{svg}"""


def render(chrome, svg, png_path, width, height, workdir):
    page = os.path.join(workdir, 'page.html')
    open(page, 'w', encoding='utf-8').write(PAGE.format(svg=svg, marker=MARKER))
    subprocess.run(
        [chrome, '--headless', '--no-sandbox', '--disable-gpu', '--hide-scrollbars',
         '--force-device-scale-factor=1',
         f'--screenshot={png_path}', f'--window-size={width},{height}', page],
        check=True, capture_output=True)
    os.remove(page)


def read_png(path):
    """(genişlik, yükseklik, piksel başına bayt, satırlar).

    Depoda raster kütüphanesi yok; çıktıyı doğrulamak için bu kadarı yeter.
    """
    data = open(path, 'rb').read()
    pos, idat, width, height, ctype = 8, b'', 0, 0, 0
    while pos < len(data):
        length = int.from_bytes(data[pos:pos + 4], 'big')
        kind = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        if kind == b'IHDR':
            width = int.from_bytes(body[0:4], 'big')
            height = int.from_bytes(body[4:8], 'big')
            ctype = body[9]
        elif kind == b'IDAT':
            idat += body
        pos += 12 + length
    raw = zlib.decompress(idat)
    bpp = {0: 1, 2: 3, 4: 2, 6: 4}[ctype]
    stride = width * bpp
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(height):
        filt = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if filt == 1:
                line[x] = (line[x] + a) & 255
            elif filt == 2:
                line[x] = (line[x] + b) & 255
            elif filt == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(bytes(line))
        prev = line
    return width, height, bpp, rows


def verify(path, width, height):
    """Boyut doğru mu ve çizim tuvalin tamamını kaplamış mı."""
    got_w, got_h, bpp, rows = read_png(path)
    if (got_w, got_h) != (width, height):
        sys.exit(f'{path}: {got_w}×{got_h} çıktı, {width}×{height} olmalıydı')
    for y, row in enumerate(rows):
        for x in range(0, got_w):
            if tuple(row[x * bpp:x * bpp + 3]) == MARKER:
                sys.exit(f'{path}: ({x},{y}) çizilmemiş — tuval kırpılmış')


def to_rgba(path):
    """PNG'yi 32 bit RGBA olarak yeniden yazar.

    Play uygulama simgesini 32 bit PNG olarak ister; Chromium çizim tamamen
    donuk olduğu için 24 bit RGB yazıyor. Öne çıkan görsel için tersi geçerli
    (alfa istenmiyor), o yüzden dönüşüm yalnızca simgeye uygulanır.
    """
    width, height, bpp, rows = read_png(path)
    if bpp == 4:
        return
    rgba = [bytes(b for x in range(width)
                  for b in (*row[x * bpp:x * bpp + 3], 255)) for row in rows]
    stride = width * 4
    # Satırlar birbirine çok benzediği için "Up" süzgeci düz baytlardan çok
    # daha iyi sıkışır; ilk satırın üstü yok, süzgeçsiz yazılır.
    raw = bytearray(b'\x00' + rgba[0])
    for y in range(1, height):
        raw += b'\x02' + bytes((rgba[y][i] - rgba[y - 1][i]) & 255 for i in range(stride))

    def chunk(kind, body):
        return (len(body).to_bytes(4, 'big') + kind + body
                + zlib.crc32(kind + body).to_bytes(4, 'big'))

    header = (width.to_bytes(4, 'big') + height.to_bytes(4, 'big')
              + bytes((8, 6, 0, 0, 0)))
    open(path, 'wb').write(
        b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', header)
        + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))


def main():
    chrome = next((c for c in CHROME_CANDIDATES if c and os.path.exists(c)), None)
    if not chrome:
        sys.exit('Chromium bulunamadı; yolu ZA_CHROME ile ver')
    os.makedirs(OUT, exist_ok=True)
    background, color, cells, size = icon_source()
    print(f'simge kaynağı: {len(cells)} blok, {size} birim, {color} / {background}')
    for name, svg, (w, h), rgba in (
        ('icon-512', icon_svg(background, color, cells, size), (512, 512), True),
        ('feature-1024', feature_svg(background, color, cells, size), (1024, 500), False),
    ):
        svg_path = os.path.join(OUT, f'{name}.svg')
        png_path = os.path.join(OUT, f'{name}.png')
        open(svg_path, 'w', encoding='utf-8').write(svg)
        render(chrome, svg, os.path.abspath(png_path), w, h, OUT)
        verify(png_path, w, h)
        if rgba:
            to_rgba(png_path)
        depth = read_png(png_path)[2] * 8
        print(f'{png_path}: {w}×{h}, {depth} bit, {os.path.getsize(png_path)} bayt')
    return 0


if __name__ == '__main__':
    sys.exit(main())
