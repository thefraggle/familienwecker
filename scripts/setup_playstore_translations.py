import os
import time
from deep_translator import GoogleTranslator

# Files to update
FEATURE_GRAPHICS = ".antigravity/generate_feature_graphics.py"
SCREENSHOTS = ".antigravity/generate_screenshots.py"

NEW_LANGS = ["id", "vi", "bn", "mr", "hi"]

# English data for Feature Graphics
EN_FG_TITLE = "Family Alarm Clock"
EN_FG_SUB = "No More Morning Chaos"
EN_FG_DESC = "The smart plan for bath, breakfast and a relaxed wake-up"

# English data for Screenshots (list of tuples: headline, text)
EN_SS = [
    ("No More\nMorning Chaos!", "The smart plan for bath, breakfast\nand a relaxed wake-up."),
    ("Perfect\nfor Everyone", "The app plans bathroom times\nfairly — no more waiting."),
    ("Good Morning,\nDad!", "Personal wake-up calls with the\ncute panda mascot."),
    ("Sleep In\nLonger", "Weekends? Holidays? FamWake lets\nyou sleep in automatically."),
    ("Instantly\nConnected", "Share code. Invite family.\nStart your day relaxed."),
    ("Breakfast\nTogether", "FamWake plans your morning –\nfrom wake-up to breakfast.")
]

def trans_for_code(text, lang):
    if not text.strip(): return text
    try:
        t = GoogleTranslator(source='en', target=lang).translate(text.replace("\n", " ||| "))
        # Replace back with literal backslash n for code writing
        return t.replace(" ||| ", "\\n").replace('"', '\\"')
    except:
        return text.replace("\n", "\\n").replace('"', '\\"')

def trans_for_md(text, lang):
    if not text.strip(): return text
    try:
        return GoogleTranslator(source='en', target=lang).translate(text)
    except:
        return text

def add_fg():
    with open(FEATURE_GRAPHICS, "r", encoding="utf-8") as f:
        content = f.read()
    
    inject = ""
    for lang in NEW_LANGS:
        if f'("{lang}"' in content: continue
        print(f"Translating FG for {lang}...")
        t_title = trans_for_code(EN_FG_TITLE, lang)
        t_sub = trans_for_code(EN_FG_SUB, lang)
        t_desc = trans_for_code(EN_FG_DESC, lang)
        # Using en screenshots for the device since we don't have translated device screenshots for these languages
        inject += f'    ("{lang}", "FamWake", "{t_title}", "{t_sub}", "{t_desc}", f"{{DEV}}/en/main_scrolled.png"),\n'
    
    if inject:
        content = content.replace('    ("zh-CN"', inject + '    ("zh-CN"')
        with open(FEATURE_GRAPHICS, "w", encoding="utf-8") as f:
            f.write(content)

def add_ss():
    with open(SCREENSHOTS, "r", encoding="utf-8") as f:
        content = f.read()
    
    inject = ""
    for lang in NEW_LANGS:
        if f'"{lang}": [' in content: continue
        print(f"Translating SS for {lang}...")
        inject += f'    "{lang}": [\n'
        for i, (hl, tx) in enumerate(EN_SS):
            t_hl = trans_for_code(hl, lang)
            t_tx = trans_for_code(tx, lang)
            # using en screenshots files
            device_files = ["main_scrolled.png", "times.png", "alarm.png", "pause.png", "share.png", "main_full.png"]
            layouts = ["bottom", "bottom", "top", "bottom", "bottom", "top"]
            inject += f'        ({i+1}, "{t_hl}", "{t_tx}", "{device_files[i]}", "{layouts[i]}"),\n'
        inject += '    ],\n'

    if inject:
        content = content.replace('    "zh-CN": [', inject + '    "zh-CN": [')
        with open(SCREENSHOTS, "w", encoding="utf-8") as f:
            f.write(content)

def translate_listings():
    en_file = "docs/internal/play_store_listings/en.md"
    with open(en_file, "r", encoding="utf-8") as f:
        en_content = f.read()
    
    for lang in NEW_LANGS:
        out_file = f"docs/internal/play_store_listings/{lang}.md"
        if os.path.exists(out_file): continue
        print(f"Translating Listing for {lang}...")
        
        # We split by paragraphs and translate line by line to keep markdown intact
        lines = en_content.split('\n')
        trans_lines = []
        for line in lines:
            if not line.strip():
                trans_lines.append("")
                continue
            if line.startswith("#"):
                # keep hashes
                hashes = line.split(" ")[0]
                text = line[len(hashes):].strip()
                t = trans_for_md(text, lang)
                trans_lines.append(f"{hashes} {t}")
            elif line.startswith("- "):
                text = line[2:]
                t = trans_for_md(text, lang)
                trans_lines.append(f"- {t}")
            else:
                trans_lines.append(trans_for_md(line, lang))
            time.sleep(0.2)
            
        with open(out_file, "w", encoding="utf-8") as f:
            f.write('\n'.join(trans_lines))


if __name__ == "__main__":
    add_fg()
    add_ss()
    translate_listings()
    print("Done setting up translations.")
