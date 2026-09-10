#!/usr/bin/env python3
"""Cihaz üstü oyun ölçümleri (adb).

Motor birim testleri kuralları doğrular; bu betik oyunun gerçek telefonda
nasıl davrandığını ölçer: kare hızı, kare bütçesinin fazlara dağılımı,
sürükleme hassasiyeti, oyun alanının piksel karşılığı. Kullanımı ve eşikler: docs/oyun-testi.md

    python3 tools/cihaz_testi.py tarama            # tüm oyunlar: A + B
    python3 tools/cihaz_testi.py kare --sure 15
    python3 tools/cihaz_testi.py fazlar
    python3 tools/cihaz_testi.py erisim
    python3 tools/cihaz_testi.py alan
    python3 tools/cihaz_testi.py surukle --y 1500 --mesafeler 20,40,80,160

Gereksinim: adb (ANDROID_HOME/platform-tools ya da PATH), Pillow, numpy.
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
import statistics
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET

import numpy as np
from PIL import Image

PAKET = "com.za.games"

# Hub'daki sırayla tüm oyunlar; `tarama` varsayılan olarak hepsini gezer.
OYUNLAR = [
    "Blok", "2048", "Yılan", "Sudoku", "Mayın Tarlası", "Beş Harf", "Kıskaç",
    "Türetme", "Dizgi", "Kuyu", "Geçit", "Tavla", "Balkon", "Kakuro",
    "Vergici", "Toplam Kapma", "Viraj", "Filo", "Reyon",
]


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
# Arayüz gezinme (uiautomator)
# ---------------------------------------------------------------------------

def tr_kucuk(s: str) -> str:
    """Türkçe duyarlı küçük harf: İ→i, I→ı."""
    return s.replace("\u0130", "i").replace("I", "\u0131").lower()


def arayuz(hepsi: bool = False) -> list[dict]:
    """Ekrandaki öğeler: etiket ve dokunma koordinatı.

    Varsayılan olarak yalnızca etiketli öğeler döner (gezinme bunları kullanır);
    [hepsi] ile etiketsiz düğümler de eklenir (erişilebilirlik taraması için).
    """
    for _ in range(3):
        if "dumped" in kabuk("uiautomator dump /sdcard/za_ui.xml"):
            break
        time.sleep(0.7)
    else:
        return []
    with tempfile.TemporaryDirectory() as gecici:
        yol = os.path.join(gecici, "ui.xml")
        adb("pull", "/sdcard/za_ui.xml", yol)
        try:
            kok = ET.parse(yol).getroot()
        except (ET.ParseError, FileNotFoundError):
            return []
    ogeler = []
    for d in kok.iter("node"):
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", d.get("bounds", ""))
        if not m:
            continue
        x1, y1, x2, y2 = map(int, m.groups())
        etiket = (d.get("text") or "").strip() or (d.get("content-desc") or "").strip()
        tiklanir = d.get("clickable") == "true"
        # Görünmeyen öğeler [0,0][0,0] sınırıyla gelir; dokunulursa ekranın
        # köşesine basılır ve gezinme sessizce yanlış yere gider. Erişilebilirlik
        # taraması için yine de sayılırlar: gizli sistem çubuğunun bölgesine
        # çizilen düğmeler bazı cihazlarda böyle gelir ve ekran okuyucu onlara
        # inemez (docs/oyun-testi.md, Reyon Sipariş bulgu 3).
        if x2 - x1 < 2 or y2 - y1 < 2:
            if hepsi and tiklanir:
                ogeler.append({"t": etiket, "cx": 0, "cy": 0, "x1": 0, "y1": 0, "x2": 0, "y2": 0,
                               "tik": True, "sinirsiz": True})
            continue
        if etiket or (hepsi and tiklanir):
            ogeler.append({"t": etiket, "cx": (x1 + x2) // 2, "cy": (y1 + y2) // 2,
                           "x1": x1, "y1": y1, "x2": x2, "y2": y2, "tik": tiklanir})
    return ogeler


def dokun(oge: dict) -> None:
    kabuk(f"input tap {oge['cx']} {oge['cy']}")
    time.sleep(1.2)


def kaydir() -> None:
    kabuk("input swipe 540 1800 540 900 400")
    time.sleep(1.6)   # savrulma otursun; erken okuma kaymış koordinat verir


def hub_ac(paket: str) -> None:
    # Uzun taramada ekran uyursa okumalar kilit ekranını görür.
    kabuk("input keyevent KEYCODE_WAKEUP")
    kabuk("wm dismiss-keyguard")
    time.sleep(0.5)
    kabuk(f"am force-stop {paket}")
    kabuk(f"am start -n {paket}/.MainActivity")
    time.sleep(2.5)


def oyun_ekraninda(ad: str, ogeler: list[dict] | None = None) -> bool:
    """Üst çubukta [ad] yazan oyun ekranında mıyız?

    Başlığın büyük harfli olmasına güvenilemez ("2048"in büyük hâli yok),
    bu yüzden doğrudan beklenen adla karşılaştırılır.
    """
    hedef = tr_kucuk(ad)
    return any(o["y1"] < 400 and tr_kucuk(o["t"]) == hedef
               for o in (arayuz() if ogeler is None else ogeler))


def oyunu_ac(ad: str) -> bool:
    """Hub'da oyunu açar ve gerçekten o oyunun açıldığını doğrular.

    Hub'ın üstündeki "son oynananlar" şeridi oyun adlarını tekrarladığı için
    ilk eşleşmeye güvenmek başka bir oyunu açabiliyor; bu yüzden her adaydan
    sonra üst çubuk doğrulanır, yanlışsa geri dönülüp sonraki aday denenir.
    """
    hedef = tr_kucuk(ad)
    for _ in range(14):
        for _aday in [o for o in arayuz() if tr_kucuk(o["t"]) == hedef]:
            taze = arayuz()
            eslesen = [o for o in taze if tr_kucuk(o["t"]) == hedef]
            if not eslesen:
                break
            aday = eslesen[0]
            oynalar = [o for o in taze if o["t"] == "Oyna" and abs(o["cy"] - aday["cy"]) < 250]
            dokun(oynalar[0] if oynalar else aday)
            ekran = arayuz()
            if oyun_ekraninda(ad, ekran) and any(o["t"] in ("Geri", "Duraklat") for o in ekran):
                return True
            kabuk("input keyevent KEYCODE_BACK")
            time.sleep(1.2)
        kaydir()
    return False


def turu_baslat() -> str | None:
    """Başlangıç kartını geçer; serbest mod varsa onu seçer (günlük hak yanmasın)."""
    ogeler = arayuz()
    serbest = next((o for o in ogeler if o["t"] == "Serbest"), None)
    if serbest:
        dokun(serbest)
        ogeler = arayuz()
    for etiket in ("Başla", "Yeniden başlat", "Tekrar dene", "Oyna"):
        o = next((x for x in ogeler if etiket.lower() in x["t"].lower()), None)
        if o:
            dokun(o)
            return etiket
    return None


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


def komut_erisim(args) -> None:
    """Erişilebilirlik: etkileşimli öğelerin ekran okuyucu etiketi var mı?

    Her dokunulabilir düğümün sınırları içinde bir etiket (text ya da
    content-desc) bulunmalı; yoksa TalkBack "düğme" der ama ne yaptığını
    söylemez. Etiket çoğu zaman çocuk düğümdedir, bu yüzden düğümün kendisine
    değil **sınırlarını kapsayan** etikete bakılır.

    Dokunma hedefi boyutu bilerek ölçülmez: Compose'da `Surface(onClick)` gibi
    bileşenlerde semantik düğüm, dokunma alanını değil içindeki metnin
    sınırlarını bildirebiliyor. Geçit'in 84 dp'lik yön tuşları bu yüzden 11 dp
    görünüyordu; şeridin dışına dokunmak çalıştığı için ölçüm yanlış alarmdı.
    Buton boyutu kodda tanımlı olduğundan kod incelemesiyle korunur.
    """
    ogeler = arayuz(hepsi=True)
    if not ogeler:
        sys.exit("Arayüz okunamadı; uygulama ön planda mı?")
    sinirsiz = [o for o in ogeler if o.get("sinirsiz")]
    ogeler = [o for o in ogeler if not o.get("sinirsiz")]
    tiklanabilir = [o for o in ogeler if o.get("tik")]
    etiketli = [o for o in ogeler if o["t"]]

    def kapsiyor(dis: dict, ic: dict) -> bool:
        return (ic["x1"] >= dis["x1"] - 2 and ic["y1"] >= dis["y1"] - 2 and
                ic["x2"] <= dis["x2"] + 2 and ic["y2"] <= dis["y2"] + 2)

    etiketsiz = [t for t in tiklanabilir
                 if not t["t"] and not any(kapsiyor(t, e) for e in etiketli)]
    print(f"dokunulabilir öğe: {len(tiklanabilir)}")
    print(f"etiketsiz: {len(etiketsiz)}")
    for o in etiketsiz:
        print(f"  [{o['x1']},{o['y1']}][{o['x2']},{o['y2']}]  "
              f"({(o['x2'] - o['x1']) / 2.625:.0f}×{(o['y2'] - o['y1']) / 2.625:.0f} dp)")
    if not etiketsiz:
        print("Her dokunulabilir öğenin bir etiketi var.")
    print(f"sınırı sıfır dokunulabilir düğüm: {len(sinirsiz)}")
    if sinirsiz:
        print("  Ekran okuyucu bunlara dokunarak inemez; gizli sistem çubuğunun bölgesine çizilen")
        print("  öğeler böyle gelir. TalkBack açıkken uygulama çubukları gizlemez, ama bu her ROM'da")
        print("  yetmiyor: SM-A515F'te alanın son 33 dp'si yine düşüyor (docs/oyun-testi.md).")
        for o in sinirsiz:
            print(f"  {o['t'] or '(etiketsiz)'}")


def komut_tarama(args) -> None:
    """Her oyunu açıp bir tur başlatır ve kare ölçümü alır (A + B aşamaları).

    Çıktıdaki "kare/s" gerçek kare hızıdır (kare sayısı ÷ pencere). Sıra
    tabanlı oyunlar boşta çizim yapmaz; onlarda 0 kare beklenen sonuçtur,
    başarısızlık değil.
    """
    oyunlar = args.oyunlar or OYUNLAR
    adb("logcat", "-c")
    print(f"{'oyun':16}{'kare/s':>8}{'kare':>7}{'jank':>14}{'p50':>7}{'kaçan':>7}  durum")
    for ad in oyunlar:
        hub_ac(args.paket)
        if not oyunu_ac(ad):
            print(f"{ad:16}  — açılamadı")
            continue
        turu_baslat()
        time.sleep(1.5)
        kabuk(f"dumpsys gfxinfo {args.paket} reset")
        time.sleep(args.sure)
        c = kabuk(f"dumpsys gfxinfo {args.paket}")
        al = [l.strip() for l in c.splitlines()]

        def deger(k, vars=""):
            for l in al:
                if l.startswith(k):
                    return l.split(":")[1].strip()
            return vars

        try:
            kare = int(deger("Total frames rendered", "0"))
        except ValueError:
            print(f"{ad:16}  — gfxinfo okunamadı")
            continue
        fps = kare / args.sure
        durum = "sürekli çizim" if fps > 30 else ("olay güdümlü" if kare < 60 else "kısmi")
        if not oyun_ekraninda(ad):
            durum += " (ekran değişti!)"
        print(f"{ad:16}{fps:>8.0f}{kare:>7}{deger('Janky frames', '?'):>14}"
              f"{deger('50th percentile', '?'):>7}{deger('Number Missed Vsync', '0'):>7}  {durum}")
    hata = adb("logcat", "-d", "AndroidRuntime:E", "*:S").strip()
    print("\n=== logcat hataları ===")
    print(hata if hata else "(yok)")


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

    e = alt.add_parser("erisim", help="etkileşimli öğelerin ekran okuyucu etiketi")
    e.set_defaults(func=komut_erisim)

    t = alt.add_parser("tarama", help="tüm oyunları açıp A+B aşamalarını koşar")
    t.add_argument("oyunlar", nargs="*", help="oyun adları; boşsa hepsi")
    t.add_argument("--sure", type=int, default=12, help="oyun başına ölçüm penceresi (s)")
    t.set_defaults(func=komut_tarama)

    args = ayristirici.parse_args()
    cihaz_var()
    args.func(args)


if __name__ == "__main__":
    main()
