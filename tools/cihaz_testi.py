#!/usr/bin/env python3
"""Cihaz üstü oyun ölçümleri (adb).

Motor birim testleri kuralları doğrular; bu betik oyunun gerçek telefonda
nasıl davrandığını ölçer: kare hızı, kare bütçesinin fazlara dağılımı,
sürükleme hassasiyeti, oyun alanının piksel karşılığı. Kullanımı ve eşikler: docs/oyun-testi.md

    python3 tools/cihaz_testi.py kare --sure 15
    python3 tools/cihaz_testi.py fazlar
    python3 tools/cihaz_testi.py alan
    python3 tools/cihaz_testi.py surukle --y 1500 --mesafeler 20,40,80,160

Gereksinim: adb (ANDROID_HOME/platform-tools ya da PATH), Pillow, numpy.
"""

from __future__ import annotations

import argparse
import os
import shutil
import statistics
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image

PAKET = "com.za.games"


# ---------------------------------------------------------------------------
# adb
# ---------------------------------------------------------------------------

def adb_yolu() -> str:
    for kok in (os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT"),
                os.path.expanduser("~/Android/Sdk")):
        if kok:
            aday = os.path.join(kok, "platform-tools", "adb")
            if os.path.exists(aday):
                return aday
    bulunan = shutil.which("adb")
    if bulunan:
        return bulunan
    sys.exit("adb bulunamadı: ANDROID_HOME tanımlayın ya da adb'yi PATH'e ekleyin.")


ADB = adb_yolu()


def adb(*args: str) -> str:
    return subprocess.run([ADB, *args], capture_output=True, text=True).stdout


def kabuk(komut: str) -> str:
    return adb("shell", komut)


def cihaz_var() -> None:
    satirlar = [s for s in adb("devices").splitlines()[1:] if s.strip()]
    bagli = [s for s in satirlar if s.endswith("device")]
    if not bagli:
        sys.exit("Bağlı cihaz yok (adb devices boş). USB hata ayıklamayı açın.")
    if len(bagli) > 1:
        sys.exit(f"Birden çok cihaz bağlı; birini bırakın:\n" + "\n".join(bagli))


def ekran_al(hedef: str) -> str:
    kabuk("screencap -p /sdcard/za_test.png")
    adb("pull", "/sdcard/za_test.png", hedef)
    return hedef


# ---------------------------------------------------------------------------
# Görüntü ölçümü
# ---------------------------------------------------------------------------

def sprite_x(yol: str, y0: int, y1: int, esik: int = 170, en_az_genislik: int = 22):
    """[y0,y1) bandındaki en geniş bitişik parlak sütun kümesinin merkezi (px).

    Oyuncu gemisi/aracı geniş ve bitişiktir; yıldız ve kıvılcım gibi küçük
    parlak noktalar ``en_az_genislik`` ile elenir. Bulunamazsa None.
    """
    im = np.asarray(Image.open(yol).convert("RGB")).astype(float)
    parlaklik = im[y0:y1].mean(axis=2)
    sutun = (parlaklik > esik).sum(axis=0).astype(float)
    acik = sutun > 0
    kumeler, i, n = [], 0, len(acik)
    while i < n:
        if acik[i]:
            j = i
            while j + 1 < n and acik[j + 1]:
                j += 1
            kumeler.append((i, j))
            i = j + 1
        else:
            i += 1
    kumeler = [k for k in kumeler if k[1] - k[0] + 1 >= en_az_genislik]
    if not kumeler:
        return None
    lo, hi = max(kumeler, key=lambda k: sutun[k[0]:k[1] + 1].sum())
    dilim, indeks = sutun[lo:hi + 1], np.arange(lo, hi + 1)
    return float((dilim * indeks).sum() / dilim.sum())


def alan_sinirlari(yol: str, satir: int | None = None):
    """Tuvalin yatay sınırlarını arka plan parlaklık sıçramasından bulur."""
    im = np.asarray(Image.open(yol).convert("RGB")).astype(float)
    satir = satir if satir is not None else im.shape[0] // 2
    parlaklik = im[satir].mean(axis=1)
    fark = np.abs(np.diff(parlaklik))
    kenar = [i for i in range(len(fark)) if fark[i] > 3]
    if len(kenar) < 2:
        return None
    return kenar[0] + 1, kenar[-1]


# ---------------------------------------------------------------------------
# Komutlar
# ---------------------------------------------------------------------------

def komut_kare(args) -> None:
    """Kare hızı ve takılma (jank) oranı; oyun OYNANIRKEN çağrılmalı."""
    kabuk(f"dumpsys gfxinfo {args.paket} reset")
    kabuk(f"sleep {args.sure}")
    cikti = kabuk(f"dumpsys gfxinfo {args.paket}")
    ilgi = ("Total frames rendered", "Janky frames:", "50th percentile",
            "90th percentile", "99th percentile", "Number Missed Vsync",
            "50th gpu percentile", "90th gpu percentile")
    satirlar = [s.strip() for s in cikti.splitlines() if s.strip().startswith(ilgi)]
    if not satirlar:
        sys.exit(f"gfxinfo boş: {args.paket} ön planda ve çiziyor mu?")
    for s in satirlar:
        print(s)
    kare = next((s for s in satirlar if s.startswith("Total frames")), "")
    sayi = int(kare.split(":")[1]) if ":" in kare else 0
    if sayi == 0:
        print("\nUYARI: 0 kare çizildi — oyun duraklamış ya da bitmiş olabilir.")
    else:
        print(f"\nortalama ≈ {sayi / args.sure:.1f} kare/s ({args.sure} s pencere)")


def komut_fazlar(args) -> None:
    """Kare bütçesinin fazlara dağılımı: darboğaz CPU'da mı GPU'da mı?

    ``gfxinfo`` yalnızca toplamı verir; hangi aşamanın pahalı olduğunu
    ``framestats`` söyler. Sütun düzeni ROM'a göre değişir, bu yüzden
    başlık satırından ad-indeks eşlemesi çıkarılır (sabit sütun numarası
    varsaymak yanlış sonuç verir).
    """
    ham = kabuk(f"dumpsys gfxinfo {args.paket} framestats")
    satirlar = [l.strip() for l in ham.splitlines() if l.strip()]
    baslik = next((l for l in satirlar if l.startswith("Flags,")), None)
    if baslik is None:
        sys.exit("framestats boş: oyun ön planda ve çiziyor mu?")
    adlar = [c for c in baslik.split(",") if c]
    yer = {ad: i for i, ad in enumerate(adlar)}
    kareler = []
    for l in satirlar:
        if not l[0].isdigit():
            continue
        p = [x for x in l.split(",") if x != ""]
        if len(p) != len(adlar):
            continue
        v = [int(x) for x in p]
        if v[0] == 0:            # yalnızca normal kareler
            kareler.append(v)
    if not kareler:
        sys.exit("framestats'ta geçerli kare yok.")

    def faz(a: str, b: str):
        if a not in yer or b not in yer:
            return []
        return sorted((r[yer[b]] - r[yer[a]]) / 1e6 for r in kareler
                      if r[yer[a]] > 0 and r[yer[b]] > 0)

    tanim = [
        ("girdi→traversal", "HandleInputStart", "PerformTraversalsStart"),
        ("ölçüm/yerleşim", "PerformTraversalsStart", "DrawStart"),
        ("çizim kaydı (CPU)", "DrawStart", "SyncQueued"),
        ("sync", "SyncStart", "IssueDrawCommandsStart"),
        ("komut→swap", "IssueDrawCommandsStart", "SwapBuffers"),
        ("GPU", "IssueDrawCommandsStart", "GpuCompleted"),
        ("TOPLAM", "IntendedVsync", "FrameCompleted"),
    ]
    print(f"kare: {len(kareler)}")
    print(f'{"faz":20}{"ortanca":>10}{"90p":>10}{"azami":>10}')
    for ad, a, b in tanim:
        v = faz(a, b)
        if not v:
            continue
        print(f"{ad:20}{statistics.median(v):9.1f}ms{v[int(len(v) * 0.9)]:9.1f}ms{v[-1]:9.1f}ms")
    print("\nÇizim kaydı yüksekse maliyet çizim kodunda; GPU yüksekse dolgu/")
    print("aşırı çizimde. `adb shell setprop debug.hwui.overdraw show` ile")
    print("aşırı çizim renklerle görülür (mavi 1×, yeşil 2×, pembe 3×, kırmızı 4×+).")


def komut_alan(args) -> None:
    with tempfile.TemporaryDirectory() as gecici:
        yol = ekran_al(os.path.join(gecici, "a.png"))
        sinir = alan_sinirlari(yol, args.satir)
        if not sinir:
            sys.exit("Tuval sınırı bulunamadı; --satir ile oyun alanından bir satır seçin.")
        sol, sag = sinir
        print(f"tuval x: {sol} → {sag}  (genişlik {sag - sol} px)")
        print("Oyun alanı birimini piksele çevirmek için oyunun ölçeğiyle karşılaştırın;")
        print("gemi/araç en sola ve en sağa dayandığında iki konumun farkı ölçeği verir.")


def komut_surukle(args) -> None:
    """Sürükleme kalibrasyonu: parmak yolu → nesne hareketi ve ölü bölge.

    Her mesafe ``--tekrar`` kez denenir, medyan alınır: ekran sarsıntısı ve
    ölüm anındaki sıçramalar tek tek ölçümleri bozabilir.
    """
    mesafeler = [int(m) for m in args.mesafeler.split(",")]
    sonuc: dict[int, list[float]] = {m: [] for m in mesafeler}
    with tempfile.TemporaryDirectory() as gecici:
        for _ in range(args.tekrar):
            for dx in mesafeler:
                once = sprite_x(ekran_al(os.path.join(gecici, "o.png")), args.y0, args.y1)
                kabuk(f"input swipe {args.x} {args.y} {args.x + dx} {args.y} {args.sure_ms}")
                kabuk("sleep 0.6")
                sonra = sprite_x(ekran_al(os.path.join(gecici, "s.png")), args.y0, args.y1)
                if once is None or sonra is None:
                    print(f"dx={dx:>4}: nesne bulunamadı, atlandı")
                    continue
                sonuc[dx].append(sonra - once)
                print(f"dx={dx:>4}  önce={once:7.1f}  sonra={sonra:7.1f}  fark={sonra - once:7.1f}")

    print("\nparmak yolu | ölçülen hareket (medyan) | kayıp")
    for dx in mesafeler:
        if not sonuc[dx]:
            print(f"{dx:>10} | (ölçüm yok)")
            continue
        orta = statistics.median(sonuc[dx])
        print(f"{dx:>10} | {orta:>10.1f} px | {dx - orta:>6.1f} px")
    print("\nKayıp her parmak basışında bir kez ödenir (dokunma toleransı).")
    print("Hareketi sıfır çıkan en büyük mesafe = ölü bölge.")


def main() -> None:
    ayristirici = argparse.ArgumentParser(description=__doc__,
                                          formatter_class=argparse.RawDescriptionHelpFormatter)
    ayristirici.add_argument("--paket", default=PAKET)
    alt = ayristirici.add_subparsers(dest="komut", required=True)

    k = alt.add_parser("kare", help="kare hızı ve takılma oranı")
    k.add_argument("--sure", type=int, default=15, help="ölçüm penceresi (s)")
    k.set_defaults(func=komut_kare)

    f = alt.add_parser("fazlar", help="kare bütçesinin fazlara dağılımı")
    f.set_defaults(func=komut_fazlar)

    a = alt.add_parser("alan", help="tuvalin piksel sınırları")
    a.add_argument("--satir", type=int, default=None)
    a.set_defaults(func=komut_alan)

    s = alt.add_parser("surukle", help="sürükleme hassasiyeti ve ölü bölge")
    s.add_argument("--x", type=int, default=400, help="sürüklemenin başlangıç x'i")
    s.add_argument("--y", type=int, default=1500, help="sürüklemenin y'si (oyun alanı içi)")
    s.add_argument("--y0", type=int, default=1935, help="nesne bandı üst y")
    s.add_argument("--y1", type=int, default=2035, help="nesne bandı alt y")
    s.add_argument("--mesafeler", default="20,40,80,160")
    s.add_argument("--tekrar", type=int, default=3)
    s.add_argument("--sure-ms", dest="sure_ms", type=int, default=300)
    s.set_defaults(func=komut_surukle)

    args = ayristirici.parse_args()
    cihaz_var()
    args.func(args)


if __name__ == "__main__":
    main()
