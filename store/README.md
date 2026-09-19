# Mağaza dosyaları

Google Play listelemesi için metinler ve form cevapları. Kaynak dosyalar buradadır; Play Console'a elle kopyalanır.

| Dosya | İçerik | Sınır |
| --- | --- | --- |
| `play/<dil>/title.txt` | Uygulama adı | 30 karakter |
| `play/<dil>/short.txt` | Kısa açıklama | 80 karakter |
| `play/<dil>/full.txt` | Tam açıklama | 4000 karakter |
| `play/release-notes/<sürüm>.txt` | Sürüm notları (dil başına `<tr-TR>` / `<en-US>` … blokları) | dil başına 500 karakter |
| `data-safety.md` | Veri güvenliği formu cevapları | |
| `icerik-derecelendirme.md` | IARC anketi cevapları, oyun oyun içerik dökümü, hedef kitle | |
| `checklist.md` | Yayın öncesi kontrol listesi ve görsel gereksinimleri | |

## Diller

Uygulamanın çevirisi olan her dil için bir listeleme klasörü var; listeleme eksik kalırsa
o dildeki kullanıcı uygulamayı kendi dilinde, mağaza sayfasını İngilizce görür. Klasör adları
uygulamadaki dil kodlarıyla (`ZaLocale.TAGS`, `res/values-<dil>`) aynıdır; Play Console bölge
istediği için kopyalarken aşağıdaki karşılığı seçin.

| Klasör | Play yerel ayarı | Dil |
| --- | --- | --- |
| `play/en` | en-US | İngilizce (varsayılan) |
| `play/tr` | tr-TR | Türkçe |
| `play/de` | de-DE | Almanca |
| `play/fr` | fr-FR | Fransızca |
| `play/nl` | nl-NL | Hollandaca |
| `play/es` | es-ES | İspanyolca |
| `play/pt` | pt-BR | Portekizce (Brezilya) |
| `play/it` | it-IT | İtalyanca |
| `play/da` | da-DK | Danca |
| `play/sv` | sv-SE | İsveççe |
| `play/nb` | no-NO | Norveççe (bokmål) |
| `play/fi` | fi-FI | Fince |
| `play/ru` | ru-RU | Rusça |
| `play/ar` | ar | Arapça |

Kelime oyunları (Beş Harf, Kıskaç, Türetme, Dizgi) Türkçe kelime listeleriyle oynandığı için
her listelemede bu dört oyunun metninin Türkçe ya da İngilizce kaldığı yazılıdır.

Sınırları, dil kapsamını ve görünmez karakterleri `python3 tools/check_store.py` ile denetleyebilirsin;
CI'da da koşar.
