#!/usr/bin/env python3
"""Sanal çoklu dokunma: iki parmağı aynı anda enjekte eder.

`adb shell input` tek parmak gönderir, `sendevent` ise SELinux yüzünden
çalışmaz (shell /dev/input'a yazamaz: "Permission denied"). Bu betik CTS'in
yolunu izler: `uinput` ile sanal bir dokunmatik kaydeder (shell uhid
grubundadır) ve olayları oradan basar. İki kişilik Raket'in iki raketi aynı
anda sürülebiliyor mu, Tuşe'de iki parmakla art arda dokunuş sayılıyor mu
diye ölçmek için kullanıldı (bkz. docs/oyun-testi.md).

Kullanım:
    python3 tools/coklu_dokunus.py surukle 300,900 760,1800 560,900 500,1800
    python3 tools/coklu_dokunus.py dokun   410,2200 664,2200 --arada 15
    python3 tools/coklu_dokunus.py dokun   410,2200 664,2200 --ayni-anda
"""
import json, os, re, subprocess, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cihaz_testi import ADB, adb                      # noqa: E402  (adb yolu tek yerde)


def _ekran():
    """Ekran çözünürlüğü: sanal aygıtın ekseni gerçek ekranla aynı olmalı."""
    ham = adb("shell", "wm size")
    m = re.findall(r"(\d+)x(\d+)", ham)
    en, boy = (int(m[-1][0]), int(m[-1][1])) if m else (1080, 2400)
    return en, boy


EN, BOY = _ekran()
SLOT, MAJOR, POSX, POSY, TRACK = 47, 48, 53, 54, 57
BTN_TOUCH, EV_KEY, EV_ABS, EV_SYN = 330, 1, 3, 0

def abs_info(code, mx):
    return {"code": code, "info": {"value": 0, "minimum": 0, "maximum": mx, "fuzz": 0, "flat": 0, "resolution": 0}}

KAYIT = {
    "id": 1, "command": "register", "name": "za-sanal-dokunmatik",
    "vid": 0x18d1, "pid": 0x0abc, "bus": "usb",
    "configuration": [
        {"type": 100, "data": [EV_KEY, EV_ABS]},
        {"type": 101, "data": [BTN_TOUCH]},
        {"type": 103, "data": [SLOT, MAJOR, POSX, POSY, TRACK]},
        {"type": 110, "data": [1]},              # INPUT_PROP_DIRECT
    ],
    "abs_info": [abs_info(SLOT, 9), abs_info(MAJOR, 255), abs_info(POSX, EN - 1),
                 abs_info(POSY, BOY - 1), abs_info(TRACK, 65535)],
}

def olaylar(*ucler):
    d = []
    for t, c, v in ucler:
        d += [t, c, v]
    return {"id": 1, "command": "inject", "events": d}

def gecikme(ms):
    return {"id": 1, "command": "delay", "duration": ms}

def indir(noktalar):
    e = []
    for slot, (x, y) in enumerate(noktalar):
        e += [(EV_ABS, SLOT, slot), (EV_ABS, TRACK, 100 + slot), (EV_ABS, MAJOR, 30),
              (EV_ABS, POSX, int(x)), (EV_ABS, POSY, int(y))]
    e += [(EV_KEY, BTN_TOUCH, 1), (EV_SYN, 0, 0)]
    return olaylar(*e)

def tasi(noktalar):
    e = []
    for slot, (x, y) in enumerate(noktalar):
        e += [(EV_ABS, SLOT, slot), (EV_ABS, POSX, int(x)), (EV_ABS, POSY, int(y))]
    e += [(EV_SYN, 0, 0)]
    return olaylar(*e)

def kaldir(n):
    e = []
    for slot in range(n):
        e += [(EV_ABS, SLOT, slot), (EV_ABS, TRACK, -1)]
    e += [(EV_KEY, BTN_TOUCH, 0), (EV_SYN, 0, 0)]
    return olaylar(*e)

def surukle(bas, son, adim=10, adim_ms=16, basili_kal=False):
    akis = [KAYIT, gecikme(400), indir(bas)]
    for i in range(1, adim + 1):
        ara = [(b[0] + (s[0] - b[0]) * i / adim, b[1] + (s[1] - b[1]) * i / adim) for b, s in zip(bas, son)]
        akis += [gecikme(adim_ms), tasi(ara)]
    if basili_kal:
        akis += [gecikme(1500)]
    akis += [gecikme(60), kaldir(len(bas)), gecikme(200)]
    metin = "\n".join(json.dumps(x) for x in akis)
    p = subprocess.run([ADB, "shell", "uinput -"], input=metin, capture_output=True, text=True)
    return p.stdout + p.stderr


def iki_dokunus(p1, p2, arada_ms=30, ayni_anda=False):
    """İki parmakla dokunma: [ayni_anda] ise ikisi de aynı SYN çerçevesinde iner,
    değilse [arada_ms] ms arayla; her parmak 40 ms basılı kalır."""
    akis = [KAYIT, gecikme(400)]
    if ayni_anda:
        akis += [indir([p1, p2]), gecikme(40), kaldir(2)]
    else:
        akis += [
            olaylar((EV_ABS, SLOT, 0), (EV_ABS, TRACK, 100), (EV_ABS, MAJOR, 30),
                    (EV_ABS, POSX, int(p1[0])), (EV_ABS, POSY, int(p1[1])),
                    (EV_KEY, BTN_TOUCH, 1), (EV_SYN, 0, 0)),
            gecikme(arada_ms),
            olaylar((EV_ABS, SLOT, 1), (EV_ABS, TRACK, 101), (EV_ABS, MAJOR, 30),
                    (EV_ABS, POSX, int(p2[0])), (EV_ABS, POSY, int(p2[1])), (EV_SYN, 0, 0)),
            gecikme(40),
            kaldir(2),
        ]
    akis += [gecikme(150)]
    metin = "\n".join(json.dumps(x) for x in akis)
    p = subprocess.run([ADB, "shell", "uinput -"], input=metin, capture_output=True, text=True)
    return p.stdout + p.stderr


def _nokta(s):
    x, y = s.split(",")
    return (int(x), int(y))


if __name__ == "__main__":
    import argparse
    ap = argparse.ArgumentParser(description="iki parmakla sürükleme ya da dokunma")
    ap.add_argument("komut", choices=["surukle", "dokun"])
    ap.add_argument("noktalar", nargs="+", help="x,y ... (sürüklemede: bas1 bas2 son1 son2)")
    ap.add_argument("--arada", type=int, default=30, help="dokunuşlar arası ms")
    ap.add_argument("--ayni-anda", dest="ayni", action="store_true", help="ikisi de tek çerçevede insin")
    a = ap.parse_args()
    p = [_nokta(n) for n in a.noktalar]
    if a.komut == "surukle":
        print(surukle([p[0], p[1]], [p[2], p[3]]) or "sürüklendi")
    else:
        print(iki_dokunus(p[0], p[1], arada_ms=a.arada, ayni_anda=a.ayni) or "dokunuldu")
