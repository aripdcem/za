#!/usr/bin/env python3
"""Play listeleme metinlerinin karakter sınırlarını denetler."""
import glob
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "store", "play")
LIMITS = {"title.txt": 30, "short.txt": 80, "full.txt": 4000}
ok = True
for lang in sorted(os.listdir(ROOT)):
    d = os.path.join(ROOT, lang)
    if not os.path.isdir(d) or lang == "release-notes":
        continue
    for name, limit in LIMITS.items():
        text = open(os.path.join(d, name), encoding="utf-8").read().strip()
        flag = "ok" if len(text) <= limit else "SINIR AŞILDI"
        ok = ok and len(text) <= limit
        print(f"{lang}/{name}: {len(text)}/{limit} {flag}")
for path in sorted(glob.glob(os.path.join(ROOT, "release-notes", "*.txt"))):
    text = open(path, encoding="utf-8").read()
    for tag in ("tr-TR", "en-US"):
        start, end = text.find(f"<{tag}>"), text.find(f"</{tag}>")
        if start < 0 or end < 0:
            continue
        block = text[start + len(tag) + 2:end].strip()
        flag = "ok" if len(block) <= 500 else "SINIR AŞILDI"
        ok = ok and len(block) <= 500
        print(f"{os.path.basename(path)} {tag}: {len(block)}/500 {flag}")
sys.exit(0 if ok else 1)
