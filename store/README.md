# Mağaza dosyaları

Google Play listelemesi için metinler ve form cevapları. Kaynak dosyalar buradadır; Play Console'a elle kopyalanır.

| Dosya | İçerik | Sınır |
| --- | --- | --- |
| `play/tr/title.txt`, `play/en/title.txt` | Uygulama adı | 30 karakter |
| `play/tr/short.txt`, `play/en/short.txt` | Kısa açıklama | 80 karakter |
| `play/tr/full.txt`, `play/en/full.txt` | Tam açıklama | 4000 karakter |
| `play/release-notes/<sürüm>.txt` | Sürüm notları (dil başına `<tr-TR>` / `<en-US>` blokları) | dil başına 500 karakter |
| `data-safety.md` | Veri güvenliği formu cevapları | |
| `checklist.md` | Yayın öncesi kontrol listesi ve görsel gereksinimleri | |

Sınırları `python3 tools/check_store.py` ile denetleyebilirsin.
