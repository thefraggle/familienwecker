"""
Generate multi-language release notes for App Store Connect (Fastlane metadata format).
Note: App Store Connect rejects emoji characters in release notes.

Reads the latest changelog entry from docs/CHANGELOG.en.md (EN) and docs/CHANGELOG.md (DE),
then translates to all supported App Store languages via deep-translator.

Output: ios/fastlane/metadata/{locale}/release_notes.txt

App Store Connect uses different locale codes than Google Play:
  - Play Store: ja-JP, ko-KR, zh-CN, it-IT → App Store: ja, ko, zh-Hans, it
  - App Store limit: 4000 bytes (vs Play Store 500 bytes)
"""
import os
import re


def truncate_to_bytes(text, max_bytes=4000, suffix="..."):
    """App Store Connect counts UTF-8 bytes. Max 4000 per release note."""
    encoded = text.encode('utf-8')
    if len(encoded) <= max_bytes:
        return text
    suffix_bytes = suffix.encode('utf-8')
    truncated = encoded[:max_bytes - len(suffix_bytes)]
    return truncated.decode('utf-8', errors='ignore') + suffix


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


def main():
    # App Store Connect locale → translation target language code
    # Keys = ASC locale (used for directory names)
    # Values = (changelog source file or None, deep-translator target code)
    asc_locales = {
        'de-DE':  ('docs/CHANGELOG.md',    'de'),
        'en-US':  ('docs/CHANGELOG.en.md', 'en'),
        'en-GB':  ('docs/CHANGELOG.en.md', 'en'),
        'fr-FR':  (None, 'fr'),
        'es-ES':  (None, 'es'),
        'es-MX':  (None, 'es'),
        'pt-BR':  (None, 'pt'),
        'pt-PT':  (None, 'pt'),
        'it':     (None, 'it'),
        'nl-NL':  (None, 'nl'),
        'ja':     (None, 'ja'),
        'ko':     (None, 'ko'),
        'zh-Hans': (None, 'zh-CN'),
        'ru':     (None, 'ru'),
        'tr':     (None, 'tr'),
        'pl':     (None, 'pl'),
        'sv':     (None, 'sv'),
        'da':     (None, 'da'),
        'no':     (None, 'no'),
        'id':     (None, 'id'),
        'vi':     (None, 'vi'),
        'hi':     (None, 'hi'),
        'uk':     (None, 'uk'),
    }

    changelog_en = get_latest_changelog('docs/CHANGELOG.en.md') or \
        "Maintenance update and performance improvements."

    # deep-translator for non-DE/EN locales
    try:
        from deep_translator import GoogleTranslator
        translator_available = True
    except ImportError:
        print("⚠️  deep-translator not installed, falling back to EN for all locales")
        translator_available = False

    metadata_base = os.path.join('ios', 'fastlane', 'metadata')
    os.makedirs(metadata_base, exist_ok=True)

    for asc_locale, (changelog_path, target_lang) in asc_locales.items():
        locale_dir = os.path.join(metadata_base, asc_locale)
        os.makedirs(locale_dir, exist_ok=True)
        dest_file = os.path.join(locale_dir, 'release_notes.txt')

        content = ""

        # Try source changelog first (DE/EN have dedicated files)
        if changelog_path:
            content = get_latest_changelog(changelog_path) or ""

        # Translate from EN for all other languages
        if not content and translator_available:
            try:
                print(f"  Translating for {asc_locale} ({target_lang})...")
                content = GoogleTranslator(
                    source='en', target=target_lang
                ).translate(changelog_en)
            except Exception as e:
                print(f"  ⚠️  Translation failed for {asc_locale}: {e}")

        # Final fallback: English
        if not content:
            content = changelog_en

        # Cleanup: strip emojis (ASC rejects them), remove label prefixes, enforce byte limit
        # Remove all emoji characters (Unicode emoji ranges)
        content = re.sub(
            r'[\U0001F300-\U0001F9FF\U00002702-\U000027B0\U0000FE00-\U0000FE0F'
            r'\U0000200D\U00002600-\U000026FF\U00002B50-\U00002B55]+', '', content
        )
        # Remove standalone label prefixes like "Improved:" / "Verbessert:" that were preceded by emoji
        content = re.sub(r'^\s*(Improved|Verbessert|Améliore|Migliorato|Mejorado|Melhorado)\s*:\s*\.?\s*', '', content, flags=re.IGNORECASE)
        content = content.replace("..", ".").strip()
        content = truncate_to_bytes(content)

        with open(dest_file, 'w', encoding='utf-8') as f:
            f.write(content)

        byte_len = len(content.encode('utf-8'))
        print(f"  ✅ {asc_locale:10s} ({byte_len:4d} bytes) → {dest_file}")

    print(f"\n✅ {len(asc_locales)} App Store release notes generated in {metadata_base}/")


if __name__ == "__main__":
    main()
