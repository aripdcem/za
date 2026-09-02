#!/usr/bin/env python3
"""Dizgi (kelime tahtası) sözlüğünü ve harf setini üretir.

Kaynaklar gen_words.py ile aynıdır (Zemberek kök sözlüğü Apache-2.0,
FrequencyWords tr_50k CC-BY-SA-4.0); indirme bağlantıları orada.

Çıktılar:
  games/dizgi/src/main/resources/dizgi/valid.txt — 2-15 harfli geçerli kökler
  stdout — DizgiLetters.kt'ye elle geçirilen harf dağılımı/puan tablosu
    (torbadaki adetler harflerin derlemdeki ağırlıklı sıklığından, puanlar
    sıklığın tersinden türetilir; tablo depoda dondurulur ki oyun
    kaynak listeler değişse de kararlı kalsın)

Kullanım: python3 tools/gen_dizgi.py <zemberek.dict> <freq50k.txt>
"""
import math
import os
import re
import sys
import unicodedata

TURKISH = "abcçdefgğhıijklmnoöprsştuüvyz"
WORD_RE = re.compile(rf"^[{TURKISH}]{{2,15}}$")
# Zemberek bazı kelimeleri şapkalı yazar (belâ, kâğıt, hâlâ); oyunda şapkalı
# taş yok, oyuncu düz harfle yazar. Düzleştirmeden atlanırlarsa "bela" gibi
# gündelik kelimeler sözlükte bulunamaz.
CIRCUMFLEX = str.maketrans("âîûÂÎÛ", "aiuaiu")
# 10+ harfli kökler yalnızca sıklık listesinde de geçiyorsa alınır
# (nadir uzun kökler listeyi şişirir, oyunda hiç görülmez).
LONG_NEEDS_FREQ = 10
TILE_TOTAL = 98  # + 2 joker = 100

OUT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "games", "dizgi", "src", "main", "resources", "dizgi",
)


def zemberek_roots(path):
    roots = set()
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "Prop" in line:
                continue
            token = line.split(" ")[0].split("[")[0].strip()
            token = unicodedata.normalize("NFC", token).translate(CIRCUMFLEX)
            if WORD_RE.match(token):
                roots.add(token)
    return roots


def freq_counts(path):
    counts = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            parts = line.split()
            if len(parts) == 2 and parts[0] not in counts:
                counts[parts[0]] = int(parts[1])
    return counts


def main():
    if len(sys.argv) != 3:
        sys.exit("kullanım: gen_dizgi.py <zemberek.dict> <freq50k.txt>")

    roots = zemberek_roots(sys.argv[1])
    freqs = freq_counts(sys.argv[2])

    valid = sorted(
        w for w in roots
        if len(w) < LONG_NEEDS_FREQ or w in freqs
    )

    os.makedirs(OUT_DIR, exist_ok=True)
    with open(os.path.join(OUT_DIR, "valid.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(valid) + "\n")

    by_len = {}
    for w in valid:
        by_len[len(w)] = by_len.get(len(w), 0) + 1
    print(f"geçerli kök: {len(valid)}")
    print("uzunluk dağılımı:", " ".join(f"{k}:{v}" for k, v in sorted(by_len.items())))

    # Harf ağırlığı: kelimenin derlem sıklığı (yumuşatılmış) kadar sayılır.
    weight = {c: 0.0 for c in TURKISH}
    for w in valid:
        f = freqs.get(w, 0) + 1
        for c in w:
            weight[c] += f
    total = sum(weight.values())

    # Torba adetleri: paya orantılı, en az 1.
    counts = {c: max(1, round(TILE_TOTAL * weight[c] / total)) for c in TURKISH}
    # Toplamı tam TILE_TOTAL'a getir: en kalabalıktan kırp / en kalabalığa ekle.
    while sum(counts.values()) > TILE_TOTAL:
        c = max(counts, key=lambda x: counts[x])
        counts[c] -= 1
    while sum(counts.values()) < TILE_TOTAL:
        c = max(TURKISH, key=lambda x: weight[x] / counts[x])
        counts[c] += 1

    # Puanlar: sıklığın tersi, 1..10 aralığına logaritmik oturtulur.
    pmax = max(weight.values())
    points = {}
    for c in TURKISH:
        r = pmax / max(weight[c], 1.0)
        points[c] = min(10, max(1, round(1 + 1.9 * math.log(r))))

    print("\n// tools/gen_dizgi.py çıktısı — harf: adet, puan")
    for c in TURKISH:
        print(f"'{c}' to Tile({counts[c]}, {points[c]}),")
    print(f"// toplam {sum(counts.values())} harf + 2 joker (0 puan)")


if __name__ == "__main__":
    main()
