"""
Generate multi-language App Store Connect metadata in Fastlane format.

Generates three files per locale:
  - release_notes.txt    (What's New — from changelogs, translated on-the-fly)
  - description.txt      (App description — from existing Play Store listings)
  - promotional_text.txt (Promotional text — translated from DE/EN source)

Note: App Store Connect rejects emoji characters in all metadata fields.
"""
import os
import re


def truncate_to_bytes(text, max_bytes=4000, suffix="..."):
    """App Store Connect counts UTF-8 bytes. Max 4000 per field."""
    encoded = text.encode('utf-8')
    if len(encoded) <= max_bytes:
        return text
    suffix_bytes = suffix.encode('utf-8')
    truncated = encoded[:max_bytes - len(suffix_bytes)]
    return truncated.decode('utf-8', errors='ignore') + suffix


def strip_emojis(text):
    """Remove all characters that App Store Connect rejects.
    Uses Unicode category whitelist instead of emoji blacklist regex,
    because Apple's rejection list is unpredictable and keeps growing."""
    import unicodedata
    cleaned = []
    for ch in text:
        cat = unicodedata.category(ch)
        # Exclude variation selectors (U+FE00-FE0F) — emoji modifiers with Mn category
        if '\uFE00' <= ch <= '\uFE0F':
            continue
        # Keep: Letters (L*), Numbers (N*), Punctuation (P*),
        # Separators/spaces (Zs), diacritics (Mn, Mc), currency (Sc),
        # math symbols (Sm), modifier symbols (Sk), and whitespace
        if cat[0] in ('L', 'N', 'P') or cat in ('Zs', 'Mn', 'Mc', 'Sc', 'Sm', 'Sk'):
            cleaned.append(ch)
        elif ch in '\n\r\t ':
            cleaned.append(ch)
        # Skip everything else: So (other symbols = emoji), Me (enclosing marks),
        # Cf (format chars like ZWJ), Co (private use), Cn (unassigned)
    return ''.join(cleaned)


def strip_html(text):
    """Remove HTML tags — App Store uses plain text, not HTML like Play Store."""
    return re.sub(r'<[^>]+>', '', text)


def get_latest_changelog(file_path):
    """Extract the first version block from a Markdown changelog."""
    if not os.path.exists(file_path):
        return None
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    match = re.search(r'## [\d.]+.*?\n(.*?)(?=\n## \d|$)', content, re.DOTALL)
    if not match:
        return None

    lines = match.group(1).strip().split('\n')
    cleaned = []
    for line in lines:
        line = line.strip()
        if not line or line.startswith('###'):
            continue
        # Strip Markdown formatting
        line = re.sub(r'(\*\*|\*|__|_)', '', line)
        line = line.lstrip('- ').strip()
        if line:
            cleaned.append(line)

    return ". ".join(cleaned) if cleaned else None


def get_description_from_listing(listing_path):
    """Extract the full description from a Play Store listing markdown file.
    The description is always the 3rd ## section in the file
    (after app name and short description)."""
    if not os.path.exists(listing_path):
        return None
    with open(listing_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find all ## section positions
    sections = list(re.finditer(r'^## .+', content, re.MULTILINE))
    if len(sections) < 3:
        return None

    # Description starts after the 3rd ## header
    desc_start = sections[2].end()
    # Description ends at the 4th ## header, or end of file
    desc_end = sections[3].start() if len(sections) > 3 else len(content)
    desc = content[desc_start:desc_end].strip()

    return desc if desc else None


# Mapping: ASC locale → Play Store listing file key
ASC_TO_LISTING = {
    'de-DE':   'de',
    'en-US':   'en',
    'en-GB':   'en',
    'fr-FR':   'fr',
    'es-ES':   'es',
    'es-MX':   'es',
    'pt-BR':   'pt',
    'pt-PT':   'pt',
    'it':      'it',
    'nl-NL':   'nl',
    'ja':      'ja',
    'ko':      'ko',
    'zh-Hans': 'zh-CN',
    'ru':      'ru',
    'tr':      'tr',
    'pl':      'pl',
    'sv':      'sv',
    'da':      'da',
    'no':      'no',
    'id':      'id',
    'vi':      'vi',
    'hi':      'hi',
    'uk':      'uk',
}

# Changelog source files per locale (only DE/EN have dedicated files)
CHANGELOG_SOURCES = {
    'de-DE': 'docs/CHANGELOG.md',
    'en-US': 'docs/CHANGELOG.en.md',
    'en-GB': 'docs/CHANGELOG.en.md',
}

# Translation target codes for deep-translator
TRANSLATION_TARGETS = {
    'de-DE': 'de', 'en-US': 'en', 'en-GB': 'en',
    'fr-FR': 'fr', 'es-ES': 'es', 'es-MX': 'es',
    'pt-BR': 'pt', 'pt-PT': 'pt', 'it': 'it',
    'nl-NL': 'nl', 'ja': 'ja', 'ko': 'ko',
    'zh-Hans': 'zh-CN', 'ru': 'ru', 'tr': 'tr',
    'pl': 'pl', 'sv': 'sv', 'da': 'da',
    'no': 'no', 'id': 'id', 'vi': 'vi',
    'hi': 'hi', 'uk': 'uk',
}

# Hardcoded promotional texts per locale — verified and locked in.
# Update these manually when the promo message changes.
PROMO_TEXTS = {
    'de-DE':   'Schluss mit Morgenchaos! FamWake koordiniert Weckzeiten, Badezimmer-Reihenfolge und Frühstück für die ganze Familie.',
    'en-US':   'No more morning chaos! FamWake coordinates wake-up times, bathroom turns, and breakfast for the whole family.',
    'en-GB':   'No more morning chaos! FamWake coordinates wake-up times, bathroom turns, and breakfast for the whole family.',
    'fr-FR':   'Fini le chaos matinal ! FamWake coordonne les heures de réveil, les tours de toilettes et le petit-déjeuner pour toute la famille.',
    'es-ES':   '¡No más caos matutino! FamWake coordina los horarios de despertarse, ir al baño y desayunar para toda la familia.',
    'es-MX':   '¡No más caos matutino! FamWake coordina los horarios de despertarse, ir al baño y desayunar para toda la familia.',
    'pt-BR':   'Chega de caos matinal! FamWake coordena horários de despertar, horários de ir ao banheiro e café da manhã para toda a família.',
    'pt-PT':   'Chega de caos matinal! FamWake coordena horários de despertar, horários de ir ao banheiro e café da manhã para toda a família.',
    'it':      'Niente più caos mattutino! FamWake coordina gli orari di sveglia, i turni del bagno e la colazione per tutta la famiglia.',
    'nl-NL':   'Geen ochtendchaos meer! FamWake coördineert de wektijden, de badkamerbeurten en het ontbijt voor het hele gezin.',
    'ja':      '朝の混乱はもうありません！ FamWake は、家族全員の起床時間、トイレの順番、朝食を調整します。',
    'ko':      '더 이상 아침의 혼란은 없습니다! FamWake는 온 가족을 위한 기상 시간, 화장실 회전 및 아침 식사를 조정합니다.',
    'zh-Hans': '早上不再混乱！ FamWake 协调全家人的起床时间、上厕所时间和早餐。',
    'ru':      'Больше никакого утреннего хаоса! FamWake координирует время пробуждения, посещения туалета и завтрака для всей семьи.',
    'tr':      'Artık sabah kaosuna son! FamWake tüm aile için uyanma saatlerini, banyo dönüşlerini ve kahvaltıyı koordine eder.',
    'pl':      'Nigdy więcej porannego chaosu! FamWake koordynuje godziny pobudek, toalety i śniadanie dla całej rodziny.',
    'sv':      'Inget mer morgonkaos! FamWake koordinerar väckningstider, badrumsvängningar och frukost för hela familjen.',
    'da':      'Ikke mere morgenkaos! FamWake koordinerer opvågningstider, badeværelsesvendinger og morgenmad for hele familien.',
    'no':      'Ikke mer morgenkaos! FamWake koordinerer oppvåkningstider, baderomsvendinger og frokost for hele familien.',
    'id':      'Tidak ada lagi kekacauan pagi hari! FamWake mengoordinasikan waktu bangun, pergantian kamar mandi, dan sarapan untuk seluruh keluarga.',
    'vi':      'Không còn sự hỗn loạn buổi sáng! FamWake điều phối thời gian thức dậy, thời gian đi vệ sinh và bữa sáng cho cả gia đình.',
    'hi':      'अब सुबह की अव्यवस्था नहीं! फैमवेक पूरे परिवार के लिए जागने के समय, बाथरूम जाने और नाश्ते के समय का समन्वय करता है।',
    'uk':      'Більше ніякого ранкового хаосу! FamWake координує час пробудження, чергування в туалеті та сніданок для всієї родини.',
}

# App Store keywords: max 100 chars, comma-separated, no spaces after commas
# These are the core search terms users would use to find this app
KEYWORDS_DE = 'Familienwecker,Morgenroutine,Badezimmer,Wecker,Kinder,Aufstehen,Frühstück,Zeitplan,Snooze,Familie'
KEYWORDS_EN = 'family alarm,morning routine,bathroom,schedule,kids alarm,wake up,breakfast,planner,snooze,organizer'

# App Store subtitle: max 30 chars, shown below app name.
# Hardcoded per locale for consistency.
SUBTITLES = {
    'de-DE':   'Familienwecker & Morgenplaner',
    'en-US':   'Family Alarm & Morning Planner',
    'en-GB':   'Family Alarm & Morning Planner',
    'fr-FR':   'R\u00e9veil familial & planning',
    'es-ES':   'Alarma familiar & planificador',
    'es-MX':   'Alarma familiar & planificador',
    'pt-BR':   'Alarme familiar & planejador',
    'pt-PT':   'Alarme familiar & planejador',
    'it':      'Sveglia famiglia & pianifica',
    'nl-NL':   'Familiewekker & ochtendplanner',
    'ja':      '家族目覚まし＆朝のプランナー',
    'ko':      '가족 알람 및 아침 플래너',
    'zh-Hans': '家庭闹钟和早晨规划',
    'ru':      'Семейный будильник & планер',
    'tr':      'Aile alarm\u0131 & sabah planlay\u0131c\u0131',
    'pl':      'Budzik rodzinny & planer',
    'sv':      'Familjev\u00e4ckare & morgonplan',
    'da':      'Familiev\u00e6kker & morgenplan',
    'no':      'Familievekker & morgenplan',
    'id':      'Alarm keluarga & perencana',
    'vi':      'B\u00e1o th\u1ee9c gia \u0111\u00ecnh & k\u1ebf ho\u1ea1ch',
    'hi':      'परिवार अलार्म और प्लानर',
    'uk':      'С\u0456мейний будильник & планер',
}

def write_file(path, content):
    """Write content to file, creating directories as needed."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)


def main():
    listings_dir = os.path.join('docs', 'internal', 'play_store_listings')
    metadata_base = os.path.join('ios', 'fastlane', 'metadata')
    os.makedirs(metadata_base, exist_ok=True)

    changelog_en = get_latest_changelog('docs/CHANGELOG.en.md') or \
        "Maintenance update and performance improvements."

    # deep-translator for non-DE/EN locales
    try:
        from deep_translator import GoogleTranslator
        translator_available = True
    except ImportError:
        print("⚠️  deep-translator not installed, falling back to EN for all locales")
        translator_available = False

    print("=" * 60)
    print("  Generating App Store Connect metadata (Fastlane format)")
    print("=" * 60)

    for asc_locale in ASC_TO_LISTING:
        locale_dir = os.path.join(metadata_base, asc_locale)
        os.makedirs(locale_dir, exist_ok=True)
        target_lang = TRANSLATION_TARGETS[asc_locale]
        listing_key = ASC_TO_LISTING[asc_locale]

        # ── 1. RELEASE NOTES ─────────────────────────────────────
        changelog_path = CHANGELOG_SOURCES.get(asc_locale)
        notes = ""
        if changelog_path:
            notes = get_latest_changelog(changelog_path) or ""

        if not notes and translator_available:
            try:
                notes = GoogleTranslator(source='en', target=target_lang).translate(changelog_en)
            except Exception as e:
                print(f"  ⚠️  Release notes translation failed for {asc_locale}: {e}")

        if not notes:
            notes = changelog_en

        notes = strip_emojis(notes)
        # Remove label prefixes that remain after emoji stripping
        notes = re.sub(r'^\s*(Improved|Verbessert)\s*:\s*\.?\s*', '', notes, flags=re.IGNORECASE)
        notes = notes.replace("..", ".").strip()
        notes = truncate_to_bytes(notes)
        write_file(os.path.join(locale_dir, 'release_notes.txt'), notes)

        # ── 2. DESCRIPTION ───────────────────────────────────────
        listing_file = os.path.join(listings_dir, f'{listing_key}.md')
        desc = get_description_from_listing(listing_file)

        if not desc:
            print(f"  ⚠️  [FALLBACK] Using EN description for {asc_locale} (listing file: {listing_key}.md)")
            desc = get_description_from_listing(os.path.join(listings_dir, 'en.md')) or ""

        desc = strip_html(desc)
        desc = strip_emojis(desc)
        # Clean up artifacts from stripping: double spaces, leading spaces on lines
        desc = re.sub(r' {2,}', ' ', desc)
        desc = re.sub(r'\n ', '\n', desc)
        desc = desc.replace("..", ".").strip()
        desc = truncate_to_bytes(desc, max_bytes=3900)
        write_file(os.path.join(locale_dir, 'description.txt'), desc)

        # ── 3. PROMOTIONAL TEXT ───────────────────────────────────
        promo = PROMO_TEXTS.get(asc_locale, PROMO_TEXTS['en-US'])
        write_file(os.path.join(locale_dir, 'promotional_text.txt'), promo)

        # ── 4. KEYWORDS ─────────────────────────────────────────────
        if asc_locale.startswith('de'):
            keywords = KEYWORDS_DE
        elif asc_locale.startswith('en'):
            keywords = KEYWORDS_EN
        elif translator_available:
            try:
                keywords = GoogleTranslator(
                    source='en', target=target_lang
                ).translate(KEYWORDS_EN)
            except Exception:
                keywords = KEYWORDS_EN
        else:
            keywords = KEYWORDS_EN

        keywords = strip_emojis(keywords).strip()
        # App Store limit: 100 characters for keywords
        if len(keywords) > 100:
            # Trim to last complete keyword within 100 chars
            keywords = keywords[:100].rsplit(',', 1)[0]
        write_file(os.path.join(locale_dir, 'keywords.txt'), keywords)

        # ── 5. SUBTITLE ────────────────────────────────────────────
        subtitle = SUBTITLES.get(asc_locale, SUBTITLES['en-US'])
        write_file(os.path.join(locale_dir, 'subtitle.txt'), subtitle)

        desc_bytes = len(desc.encode('utf-8'))
        notes_bytes = len(notes.encode('utf-8'))
        promo_len = len(promo)
        kw_len = len(keywords)
        sub_len = len(subtitle)
        print(f"  ✅ {asc_locale:10s} | notes: {notes_bytes:4d}B | desc: {desc_bytes:4d}B | promo: {promo_len:3d}/170 | kw: {kw_len:3d}/100 | sub: {sub_len:2d}/30")

    print(f"\n✅ {len(ASC_TO_LISTING)} locales generated in {metadata_base}/")
    print("   Files per locale: release_notes.txt, description.txt, promotional_text.txt, keywords.txt, subtitle.txt")


if __name__ == "__main__":
    main()
