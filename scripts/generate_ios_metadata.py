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
    """Remove all emoji characters — App Store Connect rejects them."""
    # Covers all standard emoji ranges including miscellaneous symbols,
    # dingbats, emoticons, transport, flags, and variation selectors
    emoji_pattern = re.compile(
        r'[\U0001F600-\U0001F64F'  # Emoticons
        r'\U0001F300-\U0001F5FF'   # Misc Symbols & Pictographs
        r'\U0001F680-\U0001F6FF'   # Transport & Map
        r'\U0001F1E0-\U0001F1FF'   # Flags
        r'\U0001FA00-\U0001FA6F'   # Chess, extended-A
        r'\U0001FA70-\U0001FAFF'   # Symbols extended-A
        r'\U0001F900-\U0001F9FF'   # Supplemental
        r'\U00002702-\U000027B0'   # Dingbats
        r'\U0000FE00-\U0000FE0F'   # Variation selectors
        r'\U0000200D'               # Zero-width joiner
        r'\U00002600-\U000026FF'   # Misc Symbols (⏰ ☕ etc.)
        r'\U00002300-\U000023FF'   # Misc Technical (⏱ etc.)
        r'\U00002B50-\U00002B55'   # Stars
        r'\U0000203C-\U00003299'   # CJK symbols, enclosed
        r']+', flags=re.UNICODE
    )
    return emoji_pattern.sub('', text)


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
    Looks for '## Full Description' or '## Vollständige Beschreibung' or similar,
    then takes everything until the end of file."""
    if not os.path.exists(listing_path):
        return None
    with open(listing_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Match the full description section (various languages use different headers)
    match = re.search(
        r'##\s+(?:Full Description|Vollständige Beschreibung|Description complète|'
        r'Descripción completa|Descrição completa|Descrizione completa|'
        r'Volledige beschrijving|Полное описание|Tam Açıklama|'
        r'Pełny opis|Fullständig beskrivning|Fuld beskrivelse|'
        r'Fullstendig beskrivelse|Deskripsi Lengkap|Mô tả đầy đủ|'
        r'पूर्ण विवरण|Повний опис|완전한 설명|完整描述|完全な説明|'
        r'সম্পূর্ণ বিবরণ|पूर्ण वर्णन).*?\n(.*)',
        content, re.DOTALL | re.IGNORECASE
    )
    if not match:
        return None

    return match.group(1).strip()


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

PROMO_TEXT_DE = 'Schluss mit Morgenchaos! FamWake koordiniert Weckzeiten, Badezimmer-Reihenfolge und Frühstück für die ganze Familie.'
PROMO_TEXT_EN = 'No more morning chaos! FamWake coordinates wake-up times, bathroom turns, and breakfast for the whole family.'


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
            # Fallback to EN description
            desc = get_description_from_listing(os.path.join(listings_dir, 'en.md')) or ""

        desc = strip_html(desc)
        desc = strip_emojis(desc)
        # Clean up artifacts from stripping: double spaces, leading spaces on lines
        desc = re.sub(r' {2,}', ' ', desc)
        desc = re.sub(r'\n ', '\n', desc)
        desc = desc.replace("..", ".").strip()
        desc = truncate_to_bytes(desc, max_bytes=4000)
        write_file(os.path.join(locale_dir, 'description.txt'), desc)

        # ── 3. PROMOTIONAL TEXT ───────────────────────────────────
        if asc_locale.startswith('de'):
            promo = PROMO_TEXT_DE
        elif asc_locale.startswith('en'):
            promo = PROMO_TEXT_EN
        elif translator_available:
            try:
                promo = GoogleTranslator(source='en', target=target_lang).translate(PROMO_TEXT_EN)
            except Exception:
                promo = PROMO_TEXT_EN
        else:
            promo = PROMO_TEXT_EN

        promo = strip_emojis(promo).strip()
        # Promotional text limit: 170 chars
        if len(promo) > 170:
            promo = promo[:167] + "..."
        write_file(os.path.join(locale_dir, 'promotional_text.txt'), promo)

        desc_bytes = len(desc.encode('utf-8'))
        notes_bytes = len(notes.encode('utf-8'))
        promo_len = len(promo)
        print(f"  ✅ {asc_locale:10s} | notes: {notes_bytes:4d}B | desc: {desc_bytes:4d}B | promo: {promo_len:3d}/170")

    print(f"\n✅ {len(ASC_TO_LISTING)} locales generated in {metadata_base}/")
    print("   Files per locale: release_notes.txt, description.txt, promotional_text.txt")


if __name__ == "__main__":
    main()
