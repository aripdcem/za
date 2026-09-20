#!/usr/bin/env python3
"""Kelime listelerini ve dil tablosunu denetler.

Dil tablosu iki yerde duruyor: `tools/wordlang.py` (listeleri üretir) ve
`games/sozluk/.../WordLang.kt` (oyun onu okur). İkisi ayrışırsa hata sessiz
olur: üretilmiş liste oyunun beklediği alfabede olmaz, ya da klavyede olmayan
bir harf sözlükte geçer ve oyuncu o kelimeyi hiç yazamaz.

Denetimler:
  * iki tablo aynı dilleri, aynı alfabeyi, aynı sıralamayı ve aynı klavyeyi
    tanımlıyor mu
  * her dilin beş dosyası var mı ve boş değil mi
  * ön-kodlu dosyalar çözülüyor mu, çözülünce dilin sözlük sırasında mı
  * her kelime yalnız o dilin alfabesindeki harflerden mi kuruluyor
  * uzunluk kuralları: cevaplar 5 harf, Türetme tabanı 6-7, Dizgi 2-15
  * Dizgi harf tablosu alfabeyi tam kapsıyor mu ve 98 taş mı
  * Türetme tabanı kendi geçerli listesinde mi

Kullanım: python3 tools/check_wordlists.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from wordlang import LANGS, check as check_table  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KOTLIN = os.path.join(ROOT, "games", "sozluk", "src", "main", "kotlin",
                      "com", "aripd", "zagames", "sozluk", "WordLang.kt")
TILE_TOTAL = 98
errors = []


def err(message):
    errors.append(message)


def kotlin_table():
    """WordLang.kt'deki dil tablosu: etiket -> (alfabe, sıralama, klavye)."""
    src = open(KOTLIN, encoding="utf-8").read()
    table = {}
    pattern = re.compile(
        r'tag = "([a-z]+)",\s*'
        r'letters = "([^"]+)",\s*'
        r'order = "([^"]+)",\s*'
        r'keyRows = listOf\(([^)]*)\),', re.S)
    for match in pattern.finditer(src):
        tag, letters, order, keys = match.groups()
        table[tag] = (letters, order, "".join(re.findall(r'"([^"]*)"', keys)))
    return table


def decode(path):
    """Ön-kodlu dosyayı çözer."""
    words, previous = [], ""
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            shared = ord(line[0]) - 48
            if not 0 <= shared <= len(previous):
                err(f"{path}: bozuk ön ek uzunluğu {shared} ('{line[:20]}')")
                return words
            previous = previous[:shared] + line[1:]
            words.append(previous)
    return words


def plain(path):
    return [w for w in open(path, encoding="utf-8").read().split("\n") if w]


def main():
    for problem in check_table():
        err(f"tools/wordlang.py: {problem}")

    kotlin = kotlin_table()
    if set(kotlin) != set(LANGS):
        only_py = sorted(set(LANGS) - set(kotlin))
        only_kt = sorted(set(kotlin) - set(LANGS))
        if only_py:
            err(f"wordlang.py'de olup WordLang.kt'de olmayan: {only_py}")
        if only_kt:
            err(f"WordLang.kt'de olup wordlang.py'de olmayan: {only_kt}")

    print(f"{len(LANGS)} dil")
    for tag in [t for t in kotlin if t in LANGS]:
        cfg = LANGS[tag]
        kt_letters, kt_order, kt_keys = kotlin[tag]
        if kt_letters != cfg["letters"]:
            err(f"{tag}: alfabe ayrışmış (kt '{kt_letters}' / py '{cfg['letters']}')")
        if kt_order != cfg["order"]:
            err(f"{tag}: sözlük sırası ayrışmış")
        if kt_keys != "".join(cfg["keys"]):
            err(f"{tag}: klavye ayrışmış")

        letters = set(cfg["letters"])
        rank = {c: i for i, c in enumerate(cfg["order"])}
        base = os.path.join(ROOT, "games")
        files = {
            "answers": (f"{base}/besharf/src/main/resources/besharf/{tag}/answers.txt", False),
            "allowed": (f"{base}/besharf/src/main/resources/besharf/{tag}/allowed.txt", True),
            "t_valid": (f"{base}/turetme/src/main/resources/turetme/{tag}/valid.txt", True),
            "bases": (f"{base}/turetme/src/main/resources/turetme/{tag}/bases.txt", False),
            "d_valid": (f"{base}/dizgi/src/main/resources/dizgi/{tag}/valid.txt", True),
            "letters": (f"{base}/dizgi/src/main/resources/dizgi/{tag}/letters.txt", False),
        }
        missing = [name for name, (path, _) in files.items() if not os.path.exists(path)]
        if missing:
            err(f"{tag}: eksik dosya {missing}")
            continue

        lists = {}
        for name, (path, coded) in files.items():
            if name == "letters":
                continue
            words = decode(path) if coded else plain(path)
            lists[name] = words
            if not words:
                err(f"{tag}/{name}: boş")
                continue
            bad = next((w for w in words if not set(w) <= letters), None)
            if bad:
                outside = "".join(sorted(set(bad) - letters))
                err(f"{tag}/{name}: '{bad}' alfabe dışı harf içeriyor ('{outside}')")
            if coded:
                key = [[rank.get(c, 99) for c in w] for w in words]
                if key != sorted(key):
                    err(f"{tag}/{name}: dilin sözlük sırasında değil")
            if len(set(words)) != len(words):
                err(f"{tag}/{name}: yinelenen kelime var")

        for name, lo, hi in [("answers", 5, 5), ("allowed", 5, 5),
                             ("t_valid", 3, 7), ("bases", 6, 7), ("d_valid", 2, 15)]:
            wrong = next((w for w in lists.get(name, []) if not lo <= len(w) <= hi), None)
            if wrong:
                err(f"{tag}/{name}: '{wrong}' {len(wrong)} harf, beklenen {lo}-{hi}")

        # Türetme tabanı kendi listesinde olmalı; yoksa oyun taban kelimeyi
        # geçerli saymaz ve bonus hiç verilmez.
        valid = set(lists.get("t_valid", ()))
        orphan = next((b for b in lists.get("bases", ()) if b not in valid), None)
        if orphan:
            err(f"{tag}/bases: '{orphan}' geçerli kelime listesinde yok")

        # Cevaplar tahmin listesinde olmalı, yoksa günün kelimesi reddedilir.
        allowed = set(lists.get("allowed", ()))
        rejected = next((a for a in lists.get("answers", ()) if a not in allowed), None)
        if rejected:
            err(f"{tag}/answers: '{rejected}' tahmin listesinde yok")

        rows = [r.split(" ") for r in plain(files["letters"][0])]
        table_letters = {r[0] for r in rows}
        if table_letters != letters:
            err(f"{tag}/letters: harf tablosu alfabeyle örtüşmüyor")
        total = sum(int(r[1]) for r in rows)
        if total != TILE_TOTAL:
            err(f"{tag}/letters: {total} taş, {TILE_TOTAL} olmalı")
        if any(int(r[2]) < 1 for r in rows):
            err(f"{tag}/letters: 1'den küçük puan var")

        print(f"  {tag}: cevap {len(lists['answers'])} tahmin {len(lists['allowed'])} | "
              f"türetme {len(lists['t_valid'])} taban {len(lists['bases'])} | "
              f"dizgi {len(lists['d_valid'])} harf {len(rows)}")

    print()
    for e in errors:
        print(f"HATA   {e}")
    print("✓ hata yok" if not errors else f"✗ {len(errors)} hata")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
