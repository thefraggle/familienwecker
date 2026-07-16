"""Generate Google Play Store metadata from existing Play Store listing files.

Produces Fastlane supply-compatible directory structure:
  android/fastlane/metadata/android/{locale}/title.txt
  android/fastlane/metadata/android/{locale}/short_description.txt
  android/fastlane/metadata/android/{locale}/full_description.txt

Source: docs/internal/play_store_listings/{lang}.md
"""

import os
import re
import unicodedata


def strip_unsafe_chars(text):
    """Remove characters that Google Play rejects (emojis, special symbols).
    Uses Unicode category whitelist — same approach as iOS metadata script."""
    cleaned = []
    for ch in text:
        cat = unicodedata.category(ch)
        # Exclude variation selectors (U+FE00-FE0F)
        if '\uFE00' <= ch <= '\uFE0F':
            continue
        # Keep: Letters, Numbers, Punctuation, Spaces, Diacritics,
        # Currency, Math symbols, Modifier symbols
        if cat[0] in ('L', 'N', 'P') or cat in ('Zs', 'Mn', 'Mc', 'Sc', 'Sm', 'Sk'):
            cleaned.append(ch)
        elif ch in '\n\r\t ':
            cleaned.append(ch)
    return ''.join(cleaned)


def strip_html(text):
    """Remove HTML tags — Play Store renders <b> natively, but we keep it clean."""
    return re.sub(r'<[^>]+>', '', text)


def extract_sections(listing_path):
    """Extract title, short description, and full description from a listing file.
    Uses position-based extraction (1st/2nd/3rd ## section)."""
    if not os.path.exists(listing_path):
        return None, None, None

    with open(listing_path, 'r', encoding='utf-8') as f:
        content = f.read()

    sections = list(re.finditer(r'^## .+', content, re.MULTILINE))
    if len(sections) < 3:
        return None, None, None

    # Section 1: Title (between 1st and 2nd ##)
    title = content[sections[0].end():sections[1].start()].strip()

    # Section 2: Short description (between 2nd and 3rd ##)
    short_desc = content[sections[1].end():sections[2].start()].strip()

    # Section 3: Full description (from 3rd ## to 4th ## or EOF)
    desc_end = sections[3].start() if len(sections) > 3 else len(content)
    full_desc = content[sections[2].end():desc_end].strip()

    return title, short_desc, full_desc


def write_file(path, content):
    """Write content to file, creating directories as needed."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)


# Mapping: Play Store locale → listing file key
# Google Play uses different locale codes than our file names
PLAY_TO_LISTING = {
    'de-DE': 'de',
    'en-US': 'en',
    'en-GB': 'en',
    'en-IN': 'en-IN',
    'fr-FR': 'fr',
    'fr-CA': 'fr',
    'es-ES': 'es',
    'es-US': 'es',
    'es-419': 'es',
    'pt-PT': 'pt',
    'pt-BR': 'pt',
    'it-IT': 'it',
    'nl-NL': 'nl',
    'ja-JP': 'ja',
    'ko-KR': 'ko',
    'zh-CN': 'zh-CN',
    'ru-RU': 'ru',
    'tr-TR': 'tr',
    'pl-PL': 'pl',
    'sv-SE': 'sv',
    'da-DK': 'da',
    'nb-NO': 'no',
    'id': 'id',
    'vi': 'vi',
    'bn-IN': 'bn',
    'bn-BD': 'bn',
    'mr-IN': 'mr',
    'hi-IN': 'hi',
    'uk': 'uk',
}


def main():
    listings_dir = os.path.join('docs', 'internal', 'play_store_listings')
    metadata_base = os.path.join('android', 'fastlane', 'metadata', 'android')

    print("=" * 60)
    print("  Generating Google Play metadata (Fastlane supply format)")
    print("=" * 60)

    en_title, en_short, en_full = extract_sections(
        os.path.join(listings_dir, 'en.md')
    )

    for play_locale, listing_key in PLAY_TO_LISTING.items():
        locale_dir = os.path.join(metadata_base, play_locale)
        listing_file = os.path.join(listings_dir, f'{listing_key}.md')

        title, short_desc, full_desc = extract_sections(listing_file)

        # Fallback to EN if extraction failed
        if not title:
            title = en_title or 'FamWake Family Alarm Clock'
        if not short_desc:
            short_desc = en_short or 'Smart alarm clock for families.'
        if not full_desc:
            full_desc = en_full or ''

        # Play Store allows <b> tags in full description, but short_desc must be plain
        short_desc = strip_html(short_desc)
        short_desc = strip_unsafe_chars(short_desc).strip()
        # Short description limit: 80 chars
        if len(short_desc) > 80:
            short_desc = short_desc[:77] + "..."

        # Title: 30 chars max, plain text
        title = strip_html(title)
        title = strip_unsafe_chars(title).strip()
        if len(title) > 30:
            title = title[:27] + "..."

        # Full description: keep <b> tags for Play Store formatting,
        # but strip emojis and other unsafe chars
        full_desc = strip_unsafe_chars(full_desc).strip()
        # 4000 bytes max
        encoded = full_desc.encode('utf-8')
        if len(encoded) > 4000:
            full_desc = encoded[:3997].decode('utf-8', errors='ignore') + "..."

        write_file(os.path.join(locale_dir, 'title.txt'), title)
        write_file(os.path.join(locale_dir, 'short_description.txt'), short_desc)
        write_file(os.path.join(locale_dir, 'full_description.txt'), full_desc)

        desc_bytes = len(full_desc.encode('utf-8'))
        print(f"  ✅ {play_locale:10s} | title: {len(title):2d}/30 | short: {len(short_desc):2d}/80 | desc: {desc_bytes:4d}B")

    print(f"\n✅ {len(PLAY_TO_LISTING)} locales generated in {metadata_base}/")
    print("   Files per locale: title.txt, short_description.txt, full_description.txt")


if __name__ == "__main__":
    main()
