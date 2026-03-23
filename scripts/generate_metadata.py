import os
import re
import subprocess
import glob

def get_latest_changelog(file_path):
    if not os.path.exists(file_path):
        return None
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Match the first ## [Version] header and everything until the next one
    match = re.search(r'## \d+\.\d+\.\d+.*?\n(.*?)(?=\n## \d+\.\d+\.\d+|$)', content, re.DOTALL)
    if match:
        lines = match.group(1).strip().split('\n')
        # Clean up lines (remove ###, **, etc. and keep it user-centric)
        cleaned_lines = []
        for line in lines:
            line = line.strip()
            if not line or line.startswith('###'): continue
            # Remove Markdown bold/italic
            line = re.sub(r'(\*\*|\*|__|_)', '', line)
            # Remove leading bullet points
            line = line.lstrip('- ').strip()
            if line:
                cleaned_lines.append(line)
        
        # Combine into a concise string for Play Store (limited to 500 chars)
        result = ". ".join(cleaned_lines)
        if len(result) > 497:
            result = result[:497] + "..."
        return result
    return None

def get_version_code(aab_path):
    try:
        # Use aapt2 to extract versionCode
        result = subprocess.run(['aapt2', 'dump', 'badging', aab_path], capture_output=True, text=True)
        match = re.search(r"versionCode='(\d+)'", result.stdout)
        if match:
            return match.group(1)
    except Exception as e:
        print(f"Error getting versionCode: {e}")
    return None

def main():
    # Target locales mentioned by user
    target_locales = {
        'de-DE': 'docs/CHANGELOG.md',
        'en-US': 'docs/CHANGELOG.en.md',
        'fr-FR': None,
        'it-IT': None,
        'es-ES': None
    }
    
    changelog_en = get_latest_changelog('docs/CHANGELOG.en.md') or "Maintenance update and performance optimizations."
    
    # Use googletrans for other languages if possible
    translator = None
    try:
        from googletrans import Translator
        translator = Translator()
    except Exception as e:
        print(f"googletrans setup failed: {e}")

    # New base directory for all metadata as requested by user
    dest_dir = 'release-notes'
    if os.path.exists(dest_dir):
        import shutil
        shutil.rmtree(dest_dir)
    os.makedirs(dest_dir, exist_ok=True)

    for locale, changelog_path in target_locales.items():
        # Flat naming convention: whatsnew-<locale>
        dest_file = f'{dest_dir}/whatsnew-{locale}'
        
        content = ""
        if changelog_path:
            content = get_latest_changelog(changelog_path)
        
        # Translation logic for non-DE/EN
        if not content:
            target_lang = locale.split('-')[0]
            if translator and changelog_en:
                try:
                    print(f"Translating for {locale}...")
                    translation = translator.translate(changelog_en, dest=target_lang)
                    content = translation.text
                except Exception as e:
                    print(f"Translation failed for {locale}: {e}")
            
            # FINAL FALLBACK: Never leave it empty. Use English if translation failed.
            if not content:
                content = changelog_en
        
        if content:
            # Ensure it's not too long for Play Store (500 char limit)
            if len(content) > 500:
                content = content[:497] + "..."
            
            # Sanitize: remove double dots if they were generated
            content = content.replace("..", ".")
                
            with open(dest_file, 'w', encoding='utf-8') as f:
                f.write(content)
            
            print(f"--- META FOR {locale} ({len(content)} chars) ---")
            print(content)
            print(f"Path: {dest_file}")

if __name__ == "__main__":
    main()
