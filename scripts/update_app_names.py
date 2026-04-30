import os
import xml.etree.ElementTree as ET

app_names = {
    'values-de': 'FamWake Familienwecker',
    'values-ksh': 'FamWake Familienwecker (Ruhrpott)',
    'values-swg': 'FamWake Familienwecker (Schwäbisch)',
    'values-gsw': 'FamWake Familienwecker (Schweizerdeutsch)',
    'values': 'FamWake Family Alarm Clock', # en is default
    'values-en': 'FamWake Family Alarm Clock',
    'values-nl': 'FamWake Familiewekker',
    'values-fr': 'FamWake Réveil familial',
    'values-es': 'FamWake Despertador familiar',
    'values-it': 'FamWake Sveglia familiare',
    'values-pt': 'FamWake Despertador Familiar',
    'values-da': 'FamWake Familievækkeur',
    'values-no': 'FamWake Familie vekkerklokke',
    'values-sv': 'FamWake Familjens väckarklocka',
    'values-pl': 'FamWake Budzik rodzinny',
    'values-ru': 'FamWake Семейный будильник',
    'values-uk': 'FamWake Сімейний будильник',
    'values-tr': 'FamWake Aile Çalar Saati',
    'values-ja': 'FamWake 家族の目覚まし時計',
    'values-ko': 'FamWake 가족 알람시계',
    'values-zh': 'FamWake 家庭闹钟',
    'values-zh-rCN': 'FamWake 家庭闹钟',
    'values-id': 'FamWake Jam Alarm Keluarga',
    'values-in': 'FamWake Jam Alarm Keluarga',
    'values-vi': 'FamWake Đồng hồ báo thức gia đình',
    'values-bn': 'FamWake পারিবারিক অ্যালার্ম ঘড়ি',
    'values-mr': 'FamWake कौटुंबिक अलार्म घड्याळ',
    'values-hi': 'FamWake पारिवारिक अलार्म घड़ी',
}

base_dir = 'app/src/main/res'

for folder, new_name in app_names.items():
    strings_path = os.path.join(base_dir, folder, 'strings.xml')
    if os.path.exists(strings_path):
        try:
            tree = ET.parse(strings_path)
            root = tree.getroot()
            modified = False
            for child in root:
                if child.tag == 'string' and child.attrib.get('name') == 'app_name_short':
                    if child.text != new_name:
                        child.text = new_name
                        modified = True
                        break
            if modified:
                tree.write(strings_path, encoding='utf-8', xml_declaration=True)
                print(f"Updated {folder} to '{new_name}'")
        except Exception as e:
            print(f"Error processing {folder}: {e}")
