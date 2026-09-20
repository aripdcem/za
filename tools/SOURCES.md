# Kelime listelerinin kaynakları

`tools/gen_wordlists.py` dört kelime oyununun (Beş Harf, Kıskaç, Türetme,
Dizgi) listelerini iki tür kaynaktan üretir. Kaynak dosyalar depoya eklenmez;
aşağıdaki adreslerden indirilip bir klasöre konur ve betik o klasörü alır:

```bash
python3 tools/gen_wordlists.py <kaynak-klasörü> [dil ...]
```

## Neden iki kaynak

Sıklık listesi gerçek kullanımı verir — hangi kelimenin bilindiğini söyler —
ama altyazı derleminden geldiği için içinde yazım hatası, özel ad ve yabancı
kelime vardır. Yazım sözlüğü doğruluğu verir ama bir kelimenin yaygın mı yoksa
kitabi mi olduğunu bilmez. **Cevap havuzu ikisinin kesişimidir**: hem gerçekten
kullanılan hem de sözlükte olan kelimeler.

Özel adlar (`frank`, `peter`, `maria`) altyazıda çok sık geçer ve günün kelimesi
olamaz. Eleme kuralı: yazım sözlüklerinde **özel adlar yalnız büyük harfle**
yazılıdır, sıradan kelimeler küçük harfle de geçer. Fince'de Joukahainen'in
kendi `pnoun_*` sınıfı kullanılır; Arapça'da büyük/küçük harf ayrımı olmadığı
için bu kural işlemez.

## Sıklık verisi — 14 dil

| | |
| --- | --- |
| Proje | [FrequencyWords](https://github.com/hermitdave/FrequencyWords) (hermitdave) |
| Dosya | `content/2018/<dil>/<dil>_50k.txt` → `freq_<dil>.txt` |
| Lisans | CC BY-SA 4.0 |
| Türetildiği veri | OpenSubtitles 2018 |

Norveççe için kaynakta dil kodu `no`, depoda `nb`.

## Yazım sözlükleri

| Dil | Kaynak | Lisans |
| --- | --- | --- |
| en | [wooorm/dictionaries](https://github.com/wooorm/dictionaries) `en` (SCOWL) | MIT AND BSD |
| de | aynı, `de` (igerman98) | GPL-2.0 OR GPL-3.0 |
| fr | aynı, `fr` (Dicollecte) | MPL-2.0 |
| nl | aynı, `nl` (OpenTaal) | BSD-3-Clause OR CC-BY-3.0 |
| es | aynı, `es` (RLA) | GPL-3.0 OR LGPL-3.0 OR MPL-1.1 |
| pt | aynı, `pt` (VERO, Brezilya) | LGPL-3.0 OR MPL-2.0 |
| it | aynı, `it` | GPL-3.0 |
| da | aynı, `da` (Stavekontrolden) | GPL-2.0 OR LGPL-2.1 OR MPL-1.1 |
| nb | aynı, `nb` (spell-norwegian) | GPL-2.0 |
| sv | aynı, `sv` (DSSO) | LGPL-3.0 |
| ru | aynı, `ru` | BSD-3-Clause |
| fi | [voikko/corevoikko](https://github.com/voikko/corevoikko) `voikko-fi/vocabulary/joukahainen.xml` | GPL-2.0-or-later |
| ar | [LibreOffice/dictionaries](https://github.com/LibreOffice/dictionaries) `ar/ar.dic` (Ayaspell) | GPL-2.0 |
| tr | [Zemberek-NLP](https://github.com/ahmetaa/zemberek-nlp) kök sözlüğü | Apache-2.0 |

Fince'nin Hunspell sözlüğü yok (Voikko biçimbilimsel çalışır), bu yüzden
Joukahainen sözvarlığı veritabanı kullanılır. Arapça sözlük kök değil, bütün
çekimli biçimleri sayan bir liste; Portekizce'ninki de öyle (bkz. `wordlang.py`
içindeki `expanded`).

## Lisans notu

ZA'nın kendi kodu GPL-3.0-or-later. Yukarıdaki listeler **veri** olarak
paketlenir, kodla bağlanmaz. MIT, BSD, Apache-2.0, LGPL, MPL-2.0, GPL-3.0 ve
CC BY-SA 4.0 koşulları GPL-3.0 ile birlikte dağıtıma elverir.

İki dosya GPL-2.0 olarak etiketli (`nb`, `ar`) ve "ya da sonraki sürümü" ibaresi
taşımıyor. Ayrı veri dosyaları olarak, kodla birleştirilmeden dağıtıldıkları için
GPL'in "yalnızca bir arada bulundurma" (mere aggregation) maddesi kapsamındadır.
Yine de bir dağıtıcı daha temkinli davranmak isterse `tools/wordlang.py`'den o
iki dili çıkarıp listeleri yeniden üretebilir; kod değişikliği gerekmez,
oyunlar o dillerde İngilizceye düşer.

Üretilen listeler kaynaklarının türevidir ve kaynaklarının koşullarıyla
paylaşılır. Uygulama içi **Hakkında** ekranı bu künyeyi özetler.
