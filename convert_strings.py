import os
import xml.etree.ElementTree as ET
import re

ANDROID_DIR = "app/src/main/res"
IOS_DIR = "ios/FamWake/Resources"

# Android to iOS folder mapping
mapping = {
    "values": "en",
    "values-de": "de",
    "values-b+gsw": "gsw",
    "values-b+ksh": "ksh",
    "values-b+swg": "swg",
    "values-zh-rCN": "zh",
    "values-in": "id", # Android sometimes uses in for id
}

def clean_android_string(text):
    if text is None:
        return ""
    # Android uses \n, \', \", \@
    text = text.replace(r"\'", "'")
    text = text.replace(r'\"', '"')
    # Unescape escaped newlines?
    # Replace format parameters %1$s -> %@, %1$d -> %d
    text = re.sub(r'%([0-9]+\$)?s', '%@', text)
    text = re.sub(r'%([0-9]+\$)?d', '%d', text)
    # Escape quotes for iOS
    text = text.replace('"', '\\"')
    return text

for folder in os.listdir(ANDROID_DIR):
    if not folder.startswith("values"):
        continue
    
    xml_path = os.path.join(ANDROID_DIR, folder, "strings.xml")
    if not os.path.exists(xml_path):
        continue
        
    lang = mapping.get(folder)
    if not lang:
        if folder.startswith("values-"):
            lang = folder.split("-")[1]
            
    # Some overrides
    if lang == "zh-rCN" or lang == "zh_CN":
        lang = "zh"
    if lang == "in":
        lang = "id"
        
    lproj_dir = os.path.join(IOS_DIR, f"{lang}.lproj")
    if not os.path.exists(lproj_dir):
        print(f"Skipping {folder} because {lproj_dir} does not exist.")
        continue
        
    strings_out = []
    strings_out.append("/* Auto-generated from Android strings.xml */\n")
    
    tree = ET.parse(xml_path)
    root = tree.getroot()
    
    for string_elem in root.findall('string'):
        key = string_elem.get('name')
        if not key:
            continue
        # Check translatable
        if string_elem.get('translatable') == 'false':
            continue
            
        val = string_elem.text
        if val is None:
            # Maybe has children
            val = "".join(string_elem.itertext())
            
        cleaned_val = clean_android_string(val)
        
        strings_out.append(f'"{key}" = "{cleaned_val}";')
        
    for array_elem in root.findall('string-array'):
        key = array_elem.get('name')
        if not key:
            continue
        if array_elem.get('translatable') == 'false':
            continue
            
        items = []
        for item_elem in array_elem.findall('item'):
            val = item_elem.text
            if val is None:
                val = "".join(item_elem.itertext())
            items.append(clean_android_string(val))
            
        combined_val = "||".join(items)
        strings_out.append(f'"{key}_array" = "{combined_val}";')
        
    out_path = os.path.join(lproj_dir, "Localizable.strings")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(strings_out))
        f.write("\n")
        
    print(f"Exported {len(strings_out)-1} strings to {out_path}")

print("Done.")
