#!/usr/bin/env python3
"""Beş Harf kelime listelerini üretir.

Kaynaklar (yeniden üretim için indirilip yanına konur; depoya eklenmezler):
  zemberek.dict — Zemberek NLP kök sözlüğü (Apache-2.0)
    https://raw.githubusercontent.com/ahmetaa/zemberek-nlp/master/morphology/src/main/resources/tr/master-dictionary.dict
  freq50k.txt — FrequencyWords tr_50k (CC-BY-SA-4.0, OpenSubtitles türevi)
    https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/tr/tr_50k.txt

Çıktılar (depoya eklenir):
  games/besharf/src/main/resources/besharf/answers.txt — cevap havuzu
  games/besharf/src/main/resources/besharf/allowed.txt — geçerli tahminler

Kullanım: python3 tools/gen_words.py <zemberek.dict> <freq50k.txt>
"""
import os
import re
import sys
import unicodedata

TURKISH_5 = re.compile(r"^[abcçdefgğhıijklmnoöprsştuüvyz]{5}$")
ANSWER_COUNT = 2200
# Zemberek bazı kelimeleri şapkalı yazar (kâğıt, hâlâ). Tahmin listesi için
# düzleştirilir; cevap havuzu günlük kelime dizisini belirlediğinden şapkasız
# kaynak girdilerden üretilmeye devam eder (dizi kaymaz).
CIRCUMFLEX = str.maketrans("âîûÂÎÛ", "aiuaiu")

# Cevap havuzundan dışlanan kaba sözcükler (tahmin olarak yine kabul edilir).
ANSWER_BLOCKLIST = {
    "yarak", "amcık", "orosp", "pipiş", "sikik", "sikiş", "yavşa",
}

OUT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "games", "besharf", "src", "main", "resources", "besharf",
)


def zemberek_roots(path, flatten=False):
    roots = set()
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "Prop" in line:  # özel adlar dışarıda
                continue
            token = line.split(" ")[0].split("[")[0].strip()
            if flatten:
                token = unicodedata.normalize("NFC", token).translate(CIRCUMFLEX)
            if TURKISH_5.match(token):
                roots.add(token)
    return roots


def freq_words(path):
    """rank -> kelime sırasıyla, 5 harfli Türkçe olanlar."""
    ordered = []
    seen = set()
    with open(path, encoding="utf-8") as f:
        for line in f:
            word = line.split(" ")[0].strip()
            if TURKISH_5.match(word) and word not in seen:
                seen.add(word)
                ordered.append(word)
    return ordered


def main():
    if len(sys.argv) != 3:
        sys.exit("kullanım: gen_words.py <zemberek.dict> <freq50k.txt>")

    roots = zemberek_roots(sys.argv[1])  # cevaplar: şapkasız, sabit
    roots_all = zemberek_roots(sys.argv[1], flatten=True)  # tahminler
    freq = freq_words(sys.argv[2])
    rank = {w: i for i, w in enumerate(freq)}

    # Cevaplar: sıklık listesinde de geçen kökler, en yaygından başlayarak.
    candidates = sorted((w for w in roots if w in rank), key=lambda w: rank[w])
    answers = [w for w in candidates if w not in ANSWER_BLOCKLIST][:ANSWER_COUNT]

    # Tahminler: tüm 5 harfli kökler + sıklık listesindeki tüm 5 harfli
    # biçimler (çekimli hâller dahil) + cevaplar.
    allowed = sorted(roots_all | set(freq) | set(answers))

    os.makedirs(OUT_DIR, exist_ok=True)
    with open(os.path.join(OUT_DIR, "answers.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(sorted(answers)) + "\n")
    with open(os.path.join(OUT_DIR, "allowed.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(allowed) + "\n")

    print(f"kök (5 harf): {len(roots)} (+{len(roots_all - roots)} şapkalı düzleştirmeyle)")
    print(f"sıklık (5 harf): {len(freq)}")
    print(f"cevap: {len(answers)}  örnekler: {answers[:8]} ... {answers[200:204]} ... {answers[-4:]}")
    print(f"tahmin: {len(allowed)}")


if __name__ == "__main__":
    main()
