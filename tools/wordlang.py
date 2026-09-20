#!/usr/bin/env python3
"""Kelime oyunlarının dil tablosu: alfabe, yazım sadeleştirme, kaynak dosyalar.

Dört oyun (Beş Harf, Kıskaç, Türetme, Dizgi) aynı harf kümesini ve aynı
sadeleştirme kurallarını kullanır; tablo burada tek yerde durur ve hem
üretim betiği hem denetim betiği buradan okur.

`fold`: oyuncunun klavyede yazamayacağı işaretleri harfe indirger. Kural dile
göre değişir çünkü bir dilde ayrı harf olan işaret ötekinde vurgudur:
  * Almanca ä ö ü ayrı tuş, ß yazımda "ss" ile değişebilir -> ss
  * İskandinav dillerinde æ ø å / å ä ö alfabenin kendi harfleri, kalır
  * Fransızca, İtalyanca, Portekizce, Hollandaca'da vurgu işareti tuş değil:
    é->e, ç->c, œ->oe. Bu, o dillerin kelime oyunlarının yaygın kuralı.
  * İspanyolca ñ ayrı harf, vurgular düşer
  * Rusça'da ё yaygın olarak е yazılır -> е
"""

LANGS = {}


def _lang(tag, letters, fold=None, freq=None, dic=None, note="", expanded=False,
          order=None, keys=None):
    LANGS[tag] = {
        "tag": tag,
        "letters": letters,          # oyunun alfabesi (Dizgi harf tablosu sırası)
        # Sözlük sırası: Kıskaç'ın "önce mi sonra mı" ipucu buna göre çalışır ve
        # listeler bu sırayla yazılır. Unicode sırası birçok dilde yanlıştır:
        # Almanca'da ä a ile aynı yere, İsveççe'de ä z'den sonra gider.
        "order": order or letters,
        # Ekran klavyesi: üç sıra, ülkenin kendi düzeni (QWERTZ, AZERTY, ЙЦУКЕН…).
        "keys": keys or (),
        "fold": fold or {},          # kaynak metni alfabeye indirgeme
        "freq": freq or f"freq_{tag}.txt",
        "dic": dic or f"dic_{tag}.txt",
        # Çoğu yazım sözlüğü kök listesidir (İngilizce 22 bin, Fransızca 23 bin
        # 3-7 harfli kök). Portekizce ve Arapça sözlükleri ise bütün çekimli
        # biçimleri açık açık sayar (57 bin ve 253 bin), yani "kelime" değil
        # "biçim" listesi. Oyuna olduğu gibi girerse Türetme'de bir tabanın
        # yüzlerce alt kelimesi çıkar ve oyun bitmez; bu diller için sözlük
        # katkısı sıklık listesinde de geçen biçimlerle sınırlanır.
        "expanded": expanded,
        "note": note,
    }


_ACCENTS = {
    "á": "a", "à": "a", "â": "a", "ã": "a", "ä": "a", "å": "a",
    "é": "e", "è": "e", "ê": "e", "ë": "e",
    "í": "i", "ì": "i", "î": "i", "ï": "i",
    "ó": "o", "ò": "o", "ô": "o", "õ": "o", "ö": "o",
    "ú": "u", "ù": "u", "û": "u", "ü": "u",
    "ý": "y", "ÿ": "y", "ñ": "n", "ç": "c",
    "æ": "ae", "ø": "o", "œ": "oe", "ß": "ss",
}


def _accents(keep=""):
    """Vurgu tablosu; [keep] içindeki harfler dokunulmadan bırakılır."""
    return {k: v for k, v in _ACCENTS.items() if k not in keep}


LATIN = "abcdefghijklmnopqrstuvwxyz"

_lang("en", LATIN, _accents(), note="26 harf",
      order="abcdefghijklmnopqrstuvwxyz", keys=('qwertyuiop', 'asdfghjkl', 'zxcvbnm'))
_lang("de", "abcdefghijklmnopqrstuvwxyzäöü", _accents(keep="äöü"),
      note="ä ö ü ayrı tuş; ß -> ss",
      order="aäbcdefghijklmnoöpqrstuüvwxyz", keys=('qwertzuiopü', 'asdfghjklöä', 'yxcvbnm'))
_lang("fr", LATIN, _accents(), note="vurgular düşer (SUTOM kuralı)",
      order="abcdefghijklmnopqrstuvwxyz", keys=('azertyuiop', 'qsdfghjklm', 'wxcvbn'))
_lang("nl", LATIN, _accents(), note="vurgular düşer; ij iki harf sayılır",
      order="abcdefghijklmnopqrstuvwxyz", keys=('qwertyuiop', 'asdfghjkl', 'zxcvbnm'))
_lang("es", "abcdefghijklmnopqrstuvwxyzñ", _accents(keep="ñ"),
      note="ñ ayrı harf, vurgular düşer",
      order="abcdefghijklmnñopqrstuvwxyz", keys=('qwertyuiop', 'asdfghjklñ', 'zxcvbnm'))
_lang("pt", LATIN, _accents(), note="vurgular ve ç düşer", expanded=True,
      order="abcdefghijklmnopqrstuvwxyz", keys=('qwertyuiop', 'asdfghjkl', 'zxcvbnm'))
_lang("it", LATIN, _accents(), note="vurgular düşer",
      order="abcdefghijklmnopqrstuvwxyz", keys=('qwertyuiop', 'asdfghjkl', 'zxcvbnm'))
_lang("da", "abcdefghijklmnopqrstuvwxyzæøå", _accents(keep="æøå"),
      note="æ ø å alfabenin harfleri",
      order="abcdefghijklmnopqrstuvwxyzæøå", keys=('qwertyuiopå', 'asdfghjkløæ', 'zxcvbnm'))
_lang("nb", "abcdefghijklmnopqrstuvwxyzæøå", _accents(keep="æøå"),
      note="æ ø å alfabenin harfleri",
      order="abcdefghijklmnopqrstuvwxyzæøå", keys=('qwertyuiopå', 'asdfghjkløæ', 'zxcvbnm'))
_lang("sv", "abcdefghijklmnopqrstuvwxyzåäö", _accents(keep="åäö"),
      note="å ä ö alfabenin sonunda",
      order="abcdefghijklmnopqrstuvwxyzåäö", keys=('qwertyuiopå', 'asdfghjklöä', 'zxcvbnm'))
_lang("fi", "abcdefghijklmnopqrstuvwxyzåäö", _accents(keep="åäö"),
      note="å ä ö alfabenin sonunda; å yalnız alıntılarda",
      order="abcdefghijklmnopqrstuvwxyzåäö", keys=('qwertyuiopå', 'asdfghjklöä', 'zxcvbnm'))
_lang("tr", "abcçdefgğhıijklmnoöprsştuüvyz", {"â": "a", "î": "i", "û": "u"},
      note="29 harf, Türk alfabesi sırası",
      order="abcçdefgğhıijklmnoöprsştuüvyz", keys=('ertyuıopğü', 'asdfghjklşi', 'zcvbnmöç'))
_lang("ru", "абвгдежзийклмнопрстуфхцчшщъыьэюя", {"ё": "е"},
      note="32 harf; ё yaygın yazımda е",
      order="абвгдежзийклмнопрстуфхцчшщъыьэюя", keys=('йцукенгшщзхъ', 'фывапролджэ', 'ячсмитьбю'))
_lang("ar", "ابتثجحخدذرزسشصضطظعغفقكلمنهوي",
      {"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ى": "ي", "ة": "ه",
       "ؤ": "و", "ئ": "ي", "ء": ""},
      note="28 harf; hemze biçimleri ve ta marbuta birleştirilir", expanded=True,
      order="ابتثجحخدذرزسشصضطظعغفقكلمنهوي", keys=('ضصثقفغعهخحج', 'شسيبلاتنمك', 'ظطذدزرو'))


def fold_word(word, lang):
    """Kaynak kelimeyi oyunun alfabesine indirger; indirgenemezse None."""
    cfg = LANGS[lang]
    out = []
    for ch in word.lower():
        ch = cfg["fold"].get(ch, ch)
        out.append(ch)
    folded = "".join(out)
    letters = set(cfg["letters"])
    return folded if folded and all(c in letters for c in folded) else None


def check():
    """Tablo tutarlı mı: klavye alfabenin aynısını kapsamalı, sıralama da."""
    bad = []
    for tag, cfg in LANGS.items():
        letters, order, keys = set(cfg["letters"]), cfg["order"], cfg["keys"]
        if set(order) != letters or len(order) != len(cfg["letters"]):
            bad.append(f"{tag}: sıralama alfabeyle örtüşmüyor")
        flat = "".join(keys)
        if set(flat) != letters or len(flat) != len(cfg["letters"]):
            missing = "".join(sorted(letters - set(flat)))
            extra = "".join(sorted(set(flat) - letters))
            bad.append(f"{tag}: klavye {len(flat)}/{len(cfg['letters'])} tuş"
                       + (f", eksik '{missing}'" if missing else "")
                       + (f", fazla '{extra}'" if extra else ""))
    return bad


if __name__ == "__main__":
    import sys
    problems = check()
    for p in problems:
        print("HATA  " + p)
    print(f"{len(LANGS)} dil, {'hata yok' if not problems else str(len(problems)) + ' hata'}")
    sys.exit(1 if problems else 0)
