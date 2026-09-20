#!/usr/bin/env python3
"""Dört kelime oyununun (Beş Harf, Kıskaç, Türetme, Dizgi) dil listelerini üretir.

Kaynaklar (indirilip bir klasöre konur; depoya eklenmezler):
  freq_<dil>.txt  FrequencyWords <dil>_50k, CC-BY-SA-4.0 (OpenSubtitles türevi)
                  https://github.com/hermitdave/FrequencyWords
  dic_<dil>.txt   Hunspell yazım sözlüğü (.dic), lisanslar tools/SOURCES.md'de
                  https://github.com/wooorm/dictionaries
  dic_fi.xml      Joukahainen sözvarlığı, GPL-2.0-or-later
                  https://github.com/voikko/corevoikko

Neden iki kaynak: sıklık listesi gerçek kullanımı verir ama içinde yazım
hatası, özel ad ve yabancı kelime vardır; yazım sözlüğü doğruluğu verir ama
hangi kelimenin bilindiğini bilmez. Cevap havuzu ikisinin kesişimidir.

Özel adlar: yazım sözlüklerinde özel adlar yalnız büyük harfle yazılıdır, bu
yüzden "sözlükte küçük harfle de geçiyor" kuralı onları eler (Almanca'da
denendi: 1667 adaydan frank/peter/maria gibi 139 tanesi elendi). Fince'de
Joukahainen'in kendi `pnoun_*` sınıfı kullanılır, Arapça'da büyük/küçük harf
olmadığı için bu kural yoktur.

Çıktılar:
  games/besharf/src/main/resources/besharf/<dil>/{answers,allowed}.txt
  games/turetme/src/main/resources/turetme/<dil>/{valid,bases}.txt
  games/dizgi/src/main/resources/dizgi/<dil>/valid.txt
  games/dizgi/src/main/resources/dizgi/<dil>/letters.txt  (harf adet puan)

Kullanım: python3 tools/gen_wordlists.py <kaynak-klasörü> [dil ...]
"""
import math
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from itertools import combinations

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from wordlang import LANGS, fold_word  # noqa: E402
from wordblock import BLOCKLIST        # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

WORD_LENGTH = 5          # Beş Harf
ANSWER_COUNT = 2200
TURETME_LEN = (3, 7)
BASE_LENGTHS = (6, 7)
MIN_SUBWORDS, MAX_SUBWORDS, BASE_COUNT = 15, 60, 1200
DIZGI_LEN = (2, 15)
LONG_NEEDS_FREQ = 10     # 10+ harfli kök yalnız sıklıkta da varsa alınır
TILE_TOTAL = 98          # + 2 joker = 100


def read_freq(path, lang):
    """Sıklık sırasıyla kelimeler; oyunun alfabesine indirgenmiş, tekrarsız."""
    ordered, seen = [], set()
    with open(path, encoding="utf-8") as f:
        for line in f:
            parts = line.split()
            if len(parts) != 2:
                continue
            w = fold_word(parts[0], lang)
            if w and w not in seen:
                seen.add(w)
                ordered.append(w)
    return ordered


def read_hunspell(path, lang, cased=True):
    """(tüm kelimeler, küçük harfle de yazılan kelimeler).

    İkinci küme cevap havuzu içindir: özel adlar yalnız büyük harfle yazılır.
    [cased] False ise (Arapça) ayrım yoktur, iki küme aynıdır.
    """
    every, common = set(), set()
    with open(path, encoding="utf-8", errors="replace") as f:
        for i, line in enumerate(f):
            if i == 0 and line.strip().isdigit():
                continue
            raw = line.strip()
            if not raw or raw.startswith("#") or raw.startswith("/"):
                continue
            surface = raw.split("/")[0].split("\t")[0].strip()
            if not surface:
                continue
            w = fold_word(surface, lang)
            if not w:
                continue
            every.add(w)
            if not cased or surface[:1].islower() or not surface[:1].isalpha():
                common.add(w)
    return every, common


def read_joukahainen(path, lang):
    """Fince: lemma listesi; özel ad ve kısaltma sınıfları cevap havuzu dışı."""
    every, common = set(), set()
    proper = {"pnoun_firstname", "pnoun_lastname", "pnoun_place", "pnoun_misc",
              "abbreviation", "prefix"}
    for _, node in ET.iterparse(path, events=("end",)):
        if node.tag != "word":
            continue
        classes = {c.text for c in node.iter("wclass")}
        for form in node.iter("form"):
            w = fold_word((form.text or "").strip(), lang)
            if not w:
                continue
            every.add(w)
            if not (classes & proper):
                common.add(w)
        node.clear()
    return every, common


def dictionary(src_dir, lang):
    if lang == "fi":
        return read_joukahainen(os.path.join(src_dir, "dic_fi.xml"), lang)
    return read_hunspell(os.path.join(src_dir, f"dic_{lang}.txt"), lang,
                         cased=(lang != "ar"))


def signature_index(words):
    """Harf çoklukları aynı olan kelimeleri say: "ates" ve "sate" aynı imzada.

    Taban adaylarını tek tek bütün sözlükle karşılaştırmak (1200 x 30000)
    dakikalar sürüyordu; taban en çok 7 harfli olduğu için tersi çok daha
    ucuz: tabanın alt kümelerini (en çok 2^7) üretip imzaya bakmak.
    """
    index = Counter()
    for w in words:
        if len(w) >= 3:
            index["".join(sorted(w))] += 1
    return index


def subword_count(base, index):
    """[base] harflerinden kurulabilen, sözlükte olan 3+ harfli kelime sayısı."""
    seen, total = set(), 0
    for size in range(3, len(base) + 1):
        for combo in combinations(base, size):
            sig = "".join(sorted(combo))
            if sig in seen:
                continue
            seen.add(sig)
            total += index.get(sig, 0)
    return total - index.get("".join(sorted(base)), 0)


def letter_table(words, freqrank, letters):
    """Torba adetleri ve puanlar: sık harf çok taş az puan.

    Ağırlık, kelimenin sıklık sırasından gelir (yaygın kelimenin harfleri daha
    çok sayılır), böylece tablo sözlüğün kuyruğundaki nadir kelimelerden değil
    gerçekten oynanan dilden çıkar.
    """
    weight = Counter()
    for w in words:
        rank = freqrank.get(w)
        wt = 1.0 if rank is None else 1.0 + 3.0 / (1.0 + rank / 2000.0)
        for c in w:
            weight[c] += wt
    total = sum(weight.values()) or 1.0
    table = {}
    for c in letters:
        share = weight.get(c, 0.0) / total
        count = max(1, round(share * TILE_TOTAL))
        # Puan: payın tersinden, 1..10 arası. Sık harf 1, çok nadir harf 10.
        points = 1 if share <= 0 else max(1, min(10, int(round(math.log(0.10 / max(share, 1e-6), 1.6)))))
        table[c] = (count, max(1, points))
    # Toplamı TILE_TOTAL'e oturt: fazlaysa en çok taşı olandan kıs, azsa ekle.
    while sum(c for c, _ in table.values()) > TILE_TOTAL:
        c = max(table, key=lambda k: table[k][0])
        table[c] = (table[c][0] - 1, table[c][1])
    while sum(c for c, _ in table.values()) < TILE_TOTAL:
        c = max(table, key=lambda k: (table[k][0] + 1) * 0 - table[k][1])
        table[c] = (table[c][0] + 1, table[c][1])
    return table


MAX_SHARED = 35  # ön-kodlamada paylaşılan en uzun ön ek (tek karakterle yazılır)


def front_code(words):
    """Sıralı listeyi önceki kelimeyle paylaşılan ön ek + kalan hâline getirir.

    "kalem, kalemlik, kalemtıraş" -> "0kalem", "5lik", "5tıraş". Düz metin de
    sıkışır ama APK'nın deflate'i tekrarları ancak 32 KB'lık pencerede görür;
    ön ek payı baştan atılınca 13 dilin listesi 4,6 MB yerine 3,1 MB yer kaplar.
    Uzunluk tek karakterle yazılır ('0' + n), böylece çözücü tek geçişte okur.
    """
    out, prev = [], ""
    for w in words:
        n = 0
        limit = min(len(prev), len(w), MAX_SHARED)
        while n < limit and prev[n] == w[n]:
            n += 1
        out.append(chr(48 + n) + w[n:])
        prev = w
    return out


def write(path, lines, coded=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    body = front_code(lines) if coded else lines
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(body) + "\n")
    return len(lines)


def generate(src_dir, lang):
    cfg = LANGS[lang]
    letters = cfg["letters"]
    rank_of = {c: i for i, c in enumerate(cfg["order"])}

    def key(word):
        # Dilin sözlük sırası. Kıskaç ipucunu bu sıra üzerinde ikili aramayla
        # verdiği için listeler zaten sıralı yazılır; uygulama yeniden sıralamaz.
        return [rank_of.get(c, len(rank_of)) for c in word]
    freq = read_freq(os.path.join(src_dir, cfg["freq"]), lang)
    rank = {w: i for i, w in enumerate(freq)}
    every, common = dictionary(src_dir, lang)
    block = BLOCKLIST.get(lang, set())

    # Sözlük kök listesiyse (çoğu dil) sıklık listesi ona eklenir: çekimli
    # biçimler (Almanca "meine", "alles") kök değildir ama geçerli kelimedir.
    #
    # Portekizce ve Arapça sözlükleri bütün çekimli biçimleri tek tek sayar.
    # Bu, "geçerli mi" sorusu için sorun değil — tersine, daha iyi. Sorun
    # listenin uzunluğunun oyun süresini belirlediği yerde: Türetme'de bulunacak
    # kelimeler ve Dizgi'nin tahtaya sığan sözlüğü. O iki listede biçim sözlüğü
    # sıklıkla kesişir; "geçerli tahmin" listesi (Beş Harf) kısıtlanmaz.
    known = set(freq)
    wide = every | known                      # geçerlilik: olabildiğince geniş
    play = (every & known) if cfg["expanded"] else wide   # oyun süresini belirleyen

    # --- Beş Harf ---
    five_freq = [w for w in freq if len(w) == WORD_LENGTH]
    answers = [w for w in five_freq if w in common and w not in block][:ANSWER_COUNT]
    allowed = sorted({w for w in wide if len(w) == WORD_LENGTH} | set(answers), key=key)

    # --- Türetme ---
    lo, hi = TURETME_LEN
    t_valid = sorted((w for w in play if lo <= len(w) <= hi), key=key)
    t_set = set(t_valid)
    base_pool = [w for w in freq
                 if len(w) in BASE_LENGTHS and w in common and w not in block
                 and len(set(w)) >= 4]
    index = signature_index(t_valid)
    bases = []
    for w in base_pool:
        n = subword_count(w, index)
        if MIN_SUBWORDS <= n <= MAX_SUBWORDS:
            bases.append(w)
            if len(bases) >= BASE_COUNT:
                break

    # --- Dizgi ---
    lo, hi = DIZGI_LEN
    d_valid = sorted((w for w in play
                      if lo <= len(w) <= hi and (len(w) < LONG_NEEDS_FREQ or w in rank)),
                     key=key)
    table = letter_table(d_valid, rank, letters)

    base = os.path.join(ROOT, "games")
    n1 = write(os.path.join(base, "besharf/src/main/resources/besharf", lang, "answers.txt"), sorted(answers, key=key))
    n2 = write(os.path.join(base, "besharf/src/main/resources/besharf", lang, "allowed.txt"), allowed, coded=True)
    n3 = write(os.path.join(base, "turetme/src/main/resources/turetme", lang, "valid.txt"), t_valid, coded=True)
    n4 = write(os.path.join(base, "turetme/src/main/resources/turetme", lang, "bases.txt"), sorted(bases, key=key))
    n5 = write(os.path.join(base, "dizgi/src/main/resources/dizgi", lang, "valid.txt"), d_valid, coded=True)
    n6 = write(os.path.join(base, "dizgi/src/main/resources/dizgi", lang, "letters.txt"),
               [f"{c} {table[c][0]} {table[c][1]}" for c in letters])
    print(f"{lang}: cevap {n1}  tahmin {n2} | türetme {n3} taban {n4} | dizgi {n5} harf {n6}")
    if n1 < 500 or n4 < 300:
        print(f"  UYARI {lang}: havuz küçük (cevap {n1}, taban {n4})")
    return answers, bases


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__.strip().splitlines()[-1])
    src = sys.argv[1]
    langs = sys.argv[2:] or [t for t in LANGS if t != "tr"]
    for lang in langs:
        generate(src, lang)


if __name__ == "__main__":
    main()
