#!/usr/bin/env python3
"""Cevap havuzundan tutulan kaba sözcükler.

Bunlar geçerli tahmin olmaya devam eder (oyuncu yazarsa kabul edilir); yalnız
günün kelimesi ya da Türetme tabanı olarak seçilmezler. Kaynak altyazı derlemi
olduğu için küfür sıklık listesinin üst sıralarına çıkar; eleme olmazsa
günlük bulmacada herkese aynı kelime gösterilir.

Liste eksiksiz değil, olması da beklenmiyor: amaç en sık geçenleri ayıklamak.
Yeni bir tane görülürse buraya eklenir ve listeler yeniden üretilir.
"""

_RAW = {
    "en": "bitch pussy dicks cunts whore cocks fucks fucked fucker shits "
          "nigga niggas retard boobs balls slut sluts penis wanker",
    "de": "ficken fotze scheiß arsch huren nutte schwuchtel wichser titten "
          "schlampe möse penis pissen arschloch",
    "fr": "putes salop merde bites chatte enculé encule pétasse salope "
          "couilles bordel connard conne nique",
    "nl": "kutje hoeren neuken lullen kanker teringlijer klote pissen kut "
          "tieten slet hoer",
    "es": "puta putas polla pollas coño coños joder joden mierda zorra "
          "zorras cabrón cabron follar folla teta tetas",
    "pt": "puta putas porra caralho buceta foder fodas merda vadia cuzao "
          "cuzão peito piroca xoxota",
    "it": "cazzo cazzi figa fighe merda puttana troia troie stronzo "
          "stronza vaffanculo culo scopare",
    "da": "fisse pikke luder kusse røvhul bøsse skide lorte pikken",
    "nb": "fitte pikken hore rævhøl kuk dritt faen kødd pule",
    "sv": "fitta kukar hora kuken skit knulla knullar fittan bögjävel",
    "fi": "vittu vitut perse paska mulkku kyrpä huora runkkari pillu",
    "tr": "yarak amcık orosp pipiş sikik sikiş yavşa",
    "ru": "сука суки блядь бляди хуйня пизда пизде ебать ебал ебут "
          "мудак гондон шлюха хуев",
    "ar": "شرموط عاهرة زبي كسمك نيك منيك خول قحبة",
}

BLOCKLIST = {lang: set(words.split()) for lang, words in _RAW.items()}
