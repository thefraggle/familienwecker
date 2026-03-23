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
    # We no longer strictly need the version_code for whatsnew-<track> files,
    # so we skip the aapt2 check to avoid environment-specific failures.
    
    # Cleanup old fastlane structure if it exists to avoid confusion
    base_dir = 'android/fastlane/metadata/android'
    if os.path.exists(base_dir):
        for locale in os.listdir(base_dir):
            changelog_dir = os.path.join(base_dir, locale, 'changelogs')
            if os.path.isdir(changelog_dir):
                import shutil
                shutil.rmtree(changelog_dir)
                print(f"Cleaned up legacy directory: {changelog_dir}")

    locales = {
        'de-DE': 'docs/CHANGELOG.md',
        'en-US': 'docs/CHANGELOG.en.md',
        'fr-FR': None,
        'it-IT': None,
        'es-ES': None
    }
    changelog_de = get_latest_changelog('docs/CHANGELOG.md') or ""
    changelog_en = get_latest_changelog('docs/CHANGELOG.en.md') or ""

    # Use googletrans for other languages if possible
    translator = None
    try:
        from googletrans import Translator
        translator = Translator()
    except Exception as e:
        print(f"googletrans setup failed: {e}")

    track_name = "FamWake"

    for locale, changelog_path in locales.items():
        # Flatten directory structure: directly in the locale folder
        dest_dir = f'android/fastlane/metadata/android/{locale}'
        os.makedirs(dest_dir, exist_ok=True)
        
        # Multiple naming variants to be absolutely sure the GH Action finds them
        dest_files = [
            f'{dest_dir}/whatsnew-{track_name}',         # Original (whatsnew-FamWake)
            f'{dest_dir}/whatsnew-{track_name.lower()}', # Lowercase (whatsnew-famwake)
            f'{dest_dir}/whatsnew',                      # Standard (whatsnew)
            f'{dest_dir}/default.txt',                   # Fallback txt
            f'{dest_dir}/default'                        # Fallback no-extension
        ]
        
        content = ""
        if changelog_path:
            content = get_latest_changelog(changelog_path)
        
        # Translation logic for non-DE/EN
        if not content:
            target_lang = locale.split('-')[0]
            if translator and changelog_en:
                try:
                    # Translate from English to target language
                    print(f"Translating for {locale}...")
                    translation = translator.translate(changelog_en, dest=target_lang)
                    content = translation.text
                except Exception as e:
                    print(f"Translation failed for {locale}: {e}")
            
            # Final fallback to existing default.txt content if we have it
            if not content:
                fallback_path = f'{dest_dir}/default.txt'
                if os.path.exists(fallback_path):
                    with open(fallback_path, 'r', encoding='utf-8') as f:
                        content = f.read().strip()
                else:
                    content = changelog_en or "Maintenance update."
        
        if content:
            # Ensure it's not too long for Play Store (500 char limit)
            if len(content) > 500:
                content = content[:497] + "..."
                
            # Write to ALL possible locations to be absolutely sure
            for df in dest_files:
                with open(df, 'w', encoding='utf-8') as f:
                    f.write(content)
            
            print(f"Generated metadata for {locale} in {dest_dir} (Files: {len(dest_files)})")

if __name__ == "__main__":
    main()
