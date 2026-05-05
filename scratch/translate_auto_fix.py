import os
import re
from deep_translator import GoogleTranslator

base_dir = "app/src/main/res"

# Die Basis-Sprache ist Englisch
base_text_en = "Automatically resolve conflict"
base_text_de = "Konflikt automatisch lösen"

locales = {
    "values": "en",
    "values-de": "de",
    "values-da": "da",
    "values-no": "no",
    "values-sv": "sv",
    "values-nl": "nl",
    "values-fr": "fr",
    "values-es": "es",
    "values-it": "it",
    "values-pt": "pt",
    "values-pl": "pl",
    "values-ru": "ru",
    "values-tr": "tr",
    "values-uk": "uk",
    "values-ja": "ja",
    "values-ko": "ko",
    "values-zh-rCN": "zh-CN",
    "values-id": "id",
    "values-in": "id", # Indonesian alternative code
    "values-vi": "vi",
    "values-bn": "bn",
    "values-mr": "mr",
    "values-hi": "hi"
}

# Dialects don't get automatic google translate easily, so we fallback to German or specific phrases
dialects = {
    "values-b+swg": "Konflikt automatisch lösa",
    "values-b+gsw": "Konflikt automatisch löse",
    "values-b+ksh": "Konflikt automatesch löse"
}

for val_dir, lang_code in locales.items():
    strings_file = os.path.join(base_dir, val_dir, "strings.xml")
    if os.path.exists(strings_file):
        # Read file
        with open(strings_file, "r", encoding="utf-8") as f:
            content = f.read()
        
        # Check if already exists
        if 'name="schedule_auto_fix"' in content:
            print(f"Skipping {strings_file}, already contains schedule_auto_fix")
            continue

        if lang_code == "en":
            translated = base_text_en
        elif lang_code == "de":
            translated = base_text_de
        else:
            try:
                translated = GoogleTranslator(source='auto', target=lang_code).translate(base_text_en)
            except Exception as e:
                print(f"Error translating to {lang_code}: {e}")
                translated = base_text_en

        # Append before </resources>
        new_string = f'    <string name="schedule_auto_fix">✨ {translated}</string>\n</resources>'
        new_content = content.replace("</resources>", new_string)
        
        with open(strings_file, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Updated {strings_file} with '{translated}'")

for val_dir, translated in dialects.items():
    strings_file = os.path.join(base_dir, val_dir, "strings.xml")
    if os.path.exists(strings_file):
        with open(strings_file, "r", encoding="utf-8") as f:
            content = f.read()
        
        if 'name="schedule_auto_fix"' in content:
            continue

        new_string = f'    <string name="schedule_auto_fix">✨ {translated}</string>\n</resources>'
        new_content = content.replace("</resources>", new_string)
        
        with open(strings_file, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Updated {strings_file} with '{translated}'")

