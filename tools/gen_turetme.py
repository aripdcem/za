#!/usr/bin/env python3
"""Kelime Türetme listelerini üretir.

Kaynaklar gen_words.py ile aynıdır (Zemberek kök sözlüğü Apache-2.0,
FrequencyWords tr_50k CC-BY-SA-4.0); indirme bağlantıları orada.

Çıktılar (depoya eklenir):
  games/turetme/src/main/resources/turetme/valid.txt — 3-7 harfli geçerli kökler
  games/turetme/src/main/resources/turetme/bases.txt — 6-7 harfli taban kelimeler
    (her biri en az MIN_SUBWORDS alt kelime garantili, sıklığa göre elenmiş)

Kullanım: python3 tools/gen_turetme.py <zemberek.dict> <freq50k.txt>
"""
import os
import re
import sys
from collections import Counter

TURKISH = "abcçdefgğhıijklmnoöprsştuüvyz"
WORD_RE = re.compile(rf"^[{TURKISH}]{{3,7}}$")
BASE_LENGTHS = (6, 7)
MIN_SUBWORDS = 15
MAX_SUBWORDS = 60
BASE_COUNT = 1200

OUT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "games", "turetme", "src", "main", "resources", "turetme",
)

LETTER_BIT = {c: 1 << i for i, c in enumerate(TURKISH)}


def mask(word):
    m = 0
    for c in word:
        m |= LETTER_BIT[c]
    return m


def zemberek_roots(path):
    roots = set()
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "Prop" in line:
                continue
            token = line.split(" ")[0].split("[")[0].strip()
            if WORD_RE.match(token):
                roots.add(token)
    return roots


def freq_rank(path):
    rank = {}
    with open(path, encoding="utf-8") as f:
        for i, line in enumerate(f):
            word = line.split(" ")[0].strip()
            if word not in rank:
                rank[word] = i
    return rank


def main():
    if len(sys.argv) != 3:
        sys.exit("kullanım: gen_turetme.py <zemberek.dict> <freq50k.txt>")

    valid = sorted(zemberek_roots(sys.argv[1]))
    rank = freq_rank(sys.argv[2])

    prepared = [(w, mask(w), Counter(w)) for w in valid]

    def subword_count(base):
        bm = mask(base)
        bc = Counter(base)
        n = 0
        for w, wm, wc in prepared:
            if wm & ~bm:
                continue
            if wc <= bc:
                n += 1
        return n

    candidates = [
        w for w in valid
        if len(w) in BASE_LENGTHS and w in rank
    ]
    candidates.sort(key=lambda w: rank[w])

    bases = []
    for w in candidates:
        n = subword_count(w)
        if MIN_SUBWORDS <= n <= MAX_SUBWORDS:
            bases.append(w)
        if len(bases) >= BASE_COUNT:
            break

    os.makedirs(OUT_DIR, exist_ok=True)
    with open(os.path.join(OUT_DIR, "valid.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(valid) + "\n")
    with open(os.path.join(OUT_DIR, "bases.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(sorted(bases)) + "\n")

    print(f"geçerli (3-7 harf): {len(valid)}")
    print(f"taban adayı: {len(candidates)}  seçilen: {len(bases)}")
    sample = bases[:6]
    for w in sample:
        print(f"  {w}: {subword_count(w)} alt kelime")


if __name__ == "__main__":
    main()
