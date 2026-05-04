import os
import xml.etree.ElementTree as ET

translations = {
    "values": {
        "settings_steal_title": "Take over profile?",
        "settings_steal_text": "%1$s is already taken. Do you want to take it over?",
        "settings_steal_confirm": "Take over"
    },
    "values-de": {
        "settings_steal_title": "Profil übernehmen?",
        "settings_steal_text": "%1$s ist bereits belegt. Möchtest du es übernehmen?",
        "settings_steal_confirm": "Übernehmen"
    },
    "values-b+gsw": {
        "settings_steal_title": "Profil übernehmen?",
        "settings_steal_text": "%1$s ist bereits belegt. Möchtest du es übernehmen?",
        "settings_steal_confirm": "Übernehmen"
    },
    "values-b+ksh": {
        "settings_steal_title": "Profil übernehmen?",
        "settings_steal_text": "%1$s ist bereits belegt. Möchtest du es übernehmen?",
        "settings_steal_confirm": "Übernehmen"
    },
    "values-b+swg": {
        "settings_steal_title": "Profil übernehmen?",
        "settings_steal_text": "%1$s ist bereits belegt. Möchtest du es übernehmen?",
        "settings_steal_confirm": "Übernehmen"
    },
    "values-bn": {
        "settings_steal_title": "প্রোফাইল নেব?",
        "settings_steal_text": "%1$s ইতিমধ্যেই নেওয়া হয়েছে। আপনি কি এটি নিতে চান?",
        "settings_steal_confirm": "নিন"
    },
    "values-da": {
        "settings_steal_title": "Overtag profil?",
        "settings_steal_text": "%1$s er allerede i brug. Vil du overtage den?",
        "settings_steal_confirm": "Overtag"
    },
    "values-es": {
        "settings_steal_title": "¿Asumir perfil?",
        "settings_steal_text": "%1$s ya está ocupado. ¿Quieres asumirlo?",
        "settings_steal_confirm": "Asumir"
    },
    "values-fr": {
        "settings_steal_title": "Reprendre le profil ?",
        "settings_steal_text": "%1$s est déjà pris. Voulez-vous le reprendre ?",
        "settings_steal_confirm": "Reprendre"
    },
    "values-hi": {
        "settings_steal_title": "प्रोफ़ाइल लें?",
        "settings_steal_text": "%1$s पहले ही लिया जा चुका है। क्या आप इसे लेना चाहते हैं?",
        "settings_steal_confirm": "लें"
    },
    "values-id": {
        "settings_steal_title": "Ambil alih profil?",
        "settings_steal_text": "%1$s sudah digunakan. Apakah Anda ingin mengambil alih?",
        "settings_steal_confirm": "Ambil alih"
    },
    "values-in": {
        "settings_steal_title": "Ambil alih profil?",
        "settings_steal_text": "%1$s sudah digunakan. Apakah Anda ingin mengambil alih?",
        "settings_steal_confirm": "Ambil alih"
    },
    "values-it": {
        "settings_steal_title": "Subentrare nel profilo?",
        "settings_steal_text": "%1$s è già occupato. Vuoi subentrare?",
        "settings_steal_confirm": "Subentrare"
    },
    "values-ja": {
        "settings_steal_title": "プロファイルを引き継ぎますか？",
        "settings_steal_text": "%1$s は既に使用されています。引き継ぎますか？",
        "settings_steal_confirm": "引き継ぐ"
    },
    "values-ko": {
        "settings_steal_title": "프로필 가져오기?",
        "settings_steal_text": "%1$s님은 이미 사용 중입니다. 프로필을 가져오시겠습니까?",
        "settings_steal_confirm": "가져오기"
    },
    "values-mr": {
        "settings_steal_title": "प्रोफाइल घ्यायची?",
        "settings_steal_text": "%1$s आधीच घेतले आहे. तुम्हाला हे घ्यायचे आहे का?",
        "settings_steal_confirm": "घ्या"
    },
    "values-nl": {
        "settings_steal_title": "Profiel overnemen?",
        "settings_steal_text": "%1$s is al bezet. Wil je dit profiel overnemen?",
        "settings_steal_confirm": "Overnemen"
    },
    "values-no": {
        "settings_steal_title": "Ta over profil?",
        "settings_steal_text": "%1$s er allerede i bruk. Vil du ta over profilen?",
        "settings_steal_confirm": "Ta over"
    },
    "values-pl": {
        "settings_steal_title": "Przejąć profil?",
        "settings_steal_text": "Profil %1$s jest już zajęty. Czy chcesz go przejąć?",
        "settings_steal_confirm": "Przejmij"
    },
    "values-pt": {
        "settings_steal_title": "Assumir perfil?",
        "settings_steal_text": "%1$s já está ocupado. Deseja assumi-lo?",
        "settings_steal_confirm": "Assumir"
    },
    "values-ru": {
        "settings_steal_title": "Занять профиль?",
        "settings_steal_text": "Профиль %1$s уже занят. Хотите занять его?",
        "settings_steal_confirm": "Занять"
    },
    "values-sv": {
        "settings_steal_title": "Ta över profil?",
        "settings_steal_text": "%1$s är redan upptagen. Vill du ta över profilen?",
        "settings_steal_confirm": "Ta över"
    },
    "values-tr": {
        "settings_steal_title": "Profili devral?",
        "settings_steal_text": "%1$s zaten kullanımda. Profili devralmak istiyor musunuz?",
        "settings_steal_confirm": "Devral"
    },
    "values-uk": {
        "settings_steal_title": "Зайняти профіль?",
        "settings_steal_text": "Профіль %1$s вже зайнятий. Бажаєте зайняти його?",
        "settings_steal_confirm": "Зайняти"
    },
    "values-vi": {
        "settings_steal_title": "Tiếp quản hồ sơ?",
        "settings_steal_text": "%1$s đã có người dùng. Bạn có muốn tiếp quản không?",
        "settings_steal_confirm": "Tiếp quản"
    },
    "values-zh-rCN": {
        "settings_steal_title": "接管资料？",
        "settings_steal_text": "%1$s 已被占用。您想接管吗？",
        "settings_steal_confirm": "接管"
    }
}

res_dir = "app/src/main/res"

def update_strings():
    # Iterate through all values folders
    for dir_name in os.listdir(res_dir):
        if not dir_name.startswith("values"):
            continue
            
        strings_file = os.path.join(res_dir, dir_name, "strings.xml")
        if not os.path.exists(strings_file):
            continue
            
        print(f"Updating {strings_file}...")
        
        # Determine fallback translation (use english if not specific)
        trans = translations.get(dir_name, translations["values"])
        
        with open(strings_file, "r", encoding="utf-8") as f:
            content = f.read()
            
        # Check if already present to avoid duplicates
        if "settings_steal_title" in content:
            print(f"  Already has steal strings, skipping.")
            continue
            
        # Find closing tag
        closing_tag = "</resources>"
        if closing_tag not in content:
            print(f"  Could not find {closing_tag}")
            continue
            
        # Create elements
        new_strings = f'''    <string name="settings_steal_title">{trans["settings_steal_title"]}</string>
    <string name="settings_steal_text">{trans["settings_steal_text"]}</string>
    <string name="settings_steal_confirm">{trans["settings_steal_confirm"]}</string>
</resources>'''
        
        content = content.replace(closing_tag, new_strings)
        
        with open(strings_file, "w", encoding="utf-8") as f:
            f.write(content)

if __name__ == "__main__":
    update_strings()
