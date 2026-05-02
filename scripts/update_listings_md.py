import os

NAMES = {
    "de": "FamWake Familienwecker",
    "en": "FamWake Family Alarm Clock",
    "es": "FamWake Despertador familiar",
    "fr": "FamWake Réveil familial",
    "it": "FamWake Sveglia familiare",
    "nl": "FamWake Familiewekker",
    "da": "FamWake Familievækkeur",
    "sv": "FamWake Familjeväckarklocka",
    "no": "FamWake Familievekkerklokke",
    "pl": "FamWake Budzik Rodzinny",
    "pt": "FamWake Despertador Familiar",
    "tr": "FamWake Aile Çalar Saati",
    "ru": "FamWake Семейный будильник",
    "uk": "FamWake Сімейний будильник",
    "ja": "FamWake 家族の目覚まし時計",
    "zh-CN": "FamWake 家庭闹钟",
    "ko": "FamWake 가족 알람시계",
    "id": "FamWake Jam Alarm Keluarga",
    "vi": "FamWake Đồng hồ báo thức gia đình",
    "hi": "FamWake पारिवारिक अलार्म",
    "bn": "FamWake पारिवारिक অ্যালার্ম",
    "mr": "FamWake कौटुंबिक अलार्म"
}

dir_path = "docs/internal/play_store_listings"
for filename in os.listdir(dir_path):
    if not filename.endswith(".md"): continue
    lang = filename.replace(".md", "")
    if lang not in NAMES:
        continue
    filepath = os.path.join(dir_path, filename)
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    lines = content.split('\n')
    changed = False
    for i, line in enumerate(lines):
        if line.lower().startswith("## app-name") or line.lower().startswith("## app name"):
            # find next non empty line
            for j in range(i+1, len(lines)):
                if lines[j].strip() != "":
                    if lines[j] != NAMES[lang]:
                        lines[j] = NAMES[lang]
                        changed = True
                    break
            break
            
    if changed:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write('\n'.join(lines))
        print(f"Updated {lang}.md")
