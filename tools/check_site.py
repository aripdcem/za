#!/usr/bin/env python3
"""Site denetimi. Depo kökünden koşar, CI her itmede çağırır.

Sitenin dil kapsamı bugüne kadar hiçbir denetime bağlı değildi; uygulama ve
mağaza 14 dile çıkarken gizlilik sayfası iki dilde kaldı. Denetimler:

  1. site/gizlilik.html, tools/gen_privacy.py çıktısıyla birebir aynı
     (üretilen dosya elle düzenlenince sessizce ayrışırdı)
  2. ZaLocale.TAGS içindeki her dilin kendi bölümü ve iletişim adresi var
  3. Uygulamadaki bağlantılar (ZaLinks.SITE, ZaLinks.PRIVACY) sitenin alan
     adıyla aynı: yarım kalmış alan adı taşıması ölü bağlantı bırakır
  4. Sitede eski marka tek başına geçmiyor

site/index.html şimdilik yalnızca Türkçe; bu bilinen eksik, uyarı verir.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_privacy  # noqa: E402

ROOT = gen_privacy.ROOT
LINKS = os.path.join(ROOT, "app", "src", "main", "kotlin", "com", "aripd",
                     "zagames", "platform", "Links.kt")
INDEX = os.path.join(ROOT, "site", "index.html")

errors = []
warnings = []


def host(url):
    return re.sub(r"^https?://", "", url).split("/")[0]


def main():
    tags = gen_privacy.za_tags()

    # 1. Üretilen sayfa ile depodaki dosya
    fresh = gen_privacy.page(tags)
    stored = open(gen_privacy.OUT, encoding="utf-8").read()
    if fresh != stored:
        errors.append("site/gizlilik.html üreticiyle ayrışmış; "
                      "python3 tools/gen_privacy.py ile yeniden üret")

    # 2. Dil kapsamı ve iletişim
    for tag in tags:
        block = re.search(rf'<section id="{tag}" lang="{tag}".*?</section>', stored, re.S)
        if not block:
            errors.append(f"gizlilik.html: {tag} bölümü yok")
        elif gen_privacy.MAIL not in block.group(0):
            errors.append(f"gizlilik.html: {tag} bölümünde iletişim adresi yok")
    print(f"gizlilik.html: {len(tags)} dil")

    # 3. Uygulamadaki bağlantılar sitenin alan adıyla aynı mı
    src = open(LINKS, encoding="utf-8").read()
    site = re.search(r'const val SITE = "([^"]+)"', src).group(1)
    privacy = re.search(r'const val PRIVACY = "([^"]+)"', src).group(1)
    if host(site) != host(privacy):
        errors.append(f"ZaLinks: SITE ({host(site)}) ile PRIVACY ({host(privacy)}) "
                      "aynı alan adında değil")
    for path, label in ((gen_privacy.OUT, "gizlilik.html"), (INDEX, "index.html")):
        page = open(path, encoding="utf-8").read()
        for url in re.findall(r'https?://[a-z0-9.-]*aripd\.com', page):
            if host(url) != host(site):
                errors.append(f"{label}: {url} ZaLinks.SITE ({site}) ile uyuşmuyor")
    print(f"alan adı: {host(site)}")

    # 4. Tek başına eski marka
    for path, label in ((gen_privacy.OUT, "gizlilik.html"), (INDEX, "index.html")):
        page = open(path, encoding="utf-8").read()
        lone = re.findall(r"\bZA\b(?! Games)(?![\w./-])", page)
        if lone:
            errors.append(f"{label}: tek başına \"ZA\" {len(lone)} kez geçiyor")

    # index.html'in dil kapsamı
    index = open(INDEX, encoding="utf-8").read()
    index_langs = set(re.findall(r'lang="([a-z-]+)"', index))
    if len(index_langs) < len(tags):
        warnings.append(f"index.html {len(index_langs)} dilde, uygulama {len(tags)} dilde "
                        f"({', '.join(sorted(set(tags) - index_langs))} eksik)")

    print()
    for w in warnings:
        print(f"UYARI  {w}")
    for e in errors:
        print(f"HATA   {e}")
    print()
    if errors:
        print(f"✗ {len(errors)} hata, {len(warnings)} uyarı")
        return 1
    print(f"✓ hata yok, {len(warnings)} uyarı")
    return 0


if __name__ == "__main__":
    sys.exit(main())
