import os
import re

base_dir = "app/src/main/res"

translations = {
    "values": "✨ Auto-adjust times",
    "values-de": "✨ Zeiten automatisch anpassen",
    "values-da": "✨ Tilpas tider automatisk",
    "values-no": "✨ Tilpass tider automatisk",
    "values-sv": "✨ Anpassa tider automatiskt",
    "values-nl": "✨ Tijden automatisch aanpassen",
    "values-fr": "✨ Ajuster les heures automatiquement",
    "values-es": "✨ Ajustar horas automáticamente",
    "values-it": "✨ Adatta gli orari automaticamente",
    "values-pt": "✨ Ajustar horários automaticamente",
    "values-pl": "✨ Automatycznie dostosuj czasy",
    "values-ru": "✨ Автоматически настроить время",
    "values-tr": "✨ Saatleri otomatik ayarla",
    "values-uk": "✨ Автоматично налаштувати час",
    "values-ja": "✨ 時間を自動調整",
    "values-ko": "✨ 시간 자동 조정",
    "values-zh-rCN": "✨ 自动调整时间",
    "values-id": "✨ Sesuaikan waktu secara otomatis",
    "values-in": "✨ Sesuaikan waktu secara otomatis",
    "values-vi": "✨ Tự động điều chỉnh thời gian",
    "values-bn": "✨ স্বয়ংক্রিয়ভাবে সময় সামঞ্জস্য করুন",
    "values-mr": "✨ वेळा स्वयंचलितपणे समायोजित करा",
    "values-hi": "✨ समय अपने आप सेट करें",
    "values-b+swg": "✨ Zeita automatisch aabassa",
    "values-b+gsw": "✨ Ziite automatisch aapasse",
    "values-b+ksh": "✨ Zeiten von alleine anpassen"
}

pattern = re.compile(r'<string name="schedule_auto_fix">.*?</string>')

for val_dir, text in translations.items():
    strings_file = os.path.join(base_dir, val_dir, "strings.xml")
    if os.path.exists(strings_file):
        with open(strings_file, "r", encoding="utf-8") as f:
            content = f.read()
        
        new_content = pattern.sub(f'<string name="schedule_auto_fix">{text}</string>', content)
        
        with open(strings_file, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Updated {strings_file}")
