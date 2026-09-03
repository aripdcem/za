#!/usr/bin/env python3
"""ZA ses efektlerini üretir (app/src/main/res/raw/*.wav).

Ses varlıkları elle kaydedilmez; bu betik küçük chiptune tarzı efektleri
prosedürel olarak sentezler. Yeniden üretmek için: python3 tools/gen_sfx.py
"""
import math
import os
import struct
import wave

SR = 22050
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "raw")


def tone(dur, freq_fn, shape="square", amp=0.30, decay=4.0):
    """dur saniyelik tek nota; freq_fn(t) [0..1] -> Hz."""
    n = int(SR * dur)
    out = []
    phase = 0.0
    attack = max(1, int(0.004 * SR))
    for i in range(n):
        t = i / n
        phase += 2 * math.pi * freq_fn(t) / SR
        s = math.sin(phase)
        if shape == "square":
            s = 1.0 if s >= 0 else -1.0
        elif shape == "tri":
            s = (2 / math.pi) * math.asin(math.sin(phase))
        env = min(1.0, i / attack) * math.exp(-decay * t)
        out.append(amp * s * env)
    return out


def silence(dur):
    return [0.0] * int(SR * dur)


def write(name, samples):
    path = os.path.join(OUT, name)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        frames = b"".join(
            struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32767)) for s in samples
        )
        w.writeframes(frames)
    print(f"{name}: {os.path.getsize(path)} bayt")


def main():
    os.makedirs(OUT, exist_ok=True)

    # Kısa "pop": yem yeme / taş birleştirme.
    write("sfx_pop.wav", tone(0.07, lambda t: 520 + 520 * t, "square", 0.26, 5.0))

    # Satır temizleme: yükselen üçlü arpej.
    clear = []
    for f in (523.25, 659.25, 783.99):
        clear += tone(0.06, lambda t, f=f: f, "square", 0.24, 3.5)
    write("sfx_clear.wav", clear)

    # Büyük an (Tetris, 2048): dörtlü arpej + uzun tepe notası.
    big = []
    for f in (523.25, 659.25, 783.99):
        big += tone(0.07, lambda t, f=f: f, "square", 0.24, 3.0)
    big += tone(0.22, lambda t: 1046.5, "square", 0.24, 4.5)
    write("sfx_big.wav", big)

    # Kilitlenme / sert düşüş: tok, kısa vuruş.
    write("sfx_drop.wav", tone(0.05, lambda t: 170 - 70 * t, "tri", 0.50, 7.0))

    # Oyun sonu: inen kayma.
    write("sfx_over.wav", tone(0.42, lambda t: 392 * (0.5 ** t), "tri", 0.30, 2.2))

    # Kuyu, bot atışı: çok kısa, hızla inen tiz tıkırtı (saniyede 10 kez çalabilir).
    write("sfx_shot.wav", tone(0.035, lambda t: 1200 - 700 * t, "square", 0.16, 9.0))

    # Kuyu, düşmana basış: tok, yaylanan vuruş.
    write("sfx_stomp.wav", tone(0.09, lambda t: 260 - 140 * t, "tri", 0.45, 6.0))

    # Geçit, zıplama: kısa yükselen "hop".
    write("sfx_hop.wav", tone(0.05, lambda t: 520 + 500 * t, "tri", 0.32, 5.0))

    # Geçit, tren kornası: iki notalı, boğuk.
    write("sfx_horn.wav", tone(0.12, lambda t: 466.16, "square", 0.26, 2.0) + tone(0.2, lambda t: 392.0, "square", 0.26, 3.0))

    # Geçit, kartal çığlığı: titreşimli, inen tiz ses.
    write("sfx_screech.wav", tone(0.22, lambda t: 1500 - 500 * t + 120 * math.sin(90 * t), "square", 0.22, 3.0))

    # Geçit, suya düşüş: tok, hızla inen "plop".
    write("sfx_splash.wav", tone(0.12, lambda t: 420 - 300 * t, "tri", 0.40, 6.0))


if __name__ == "__main__":
    main()
