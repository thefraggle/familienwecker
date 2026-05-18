import os
import xml.etree.ElementTree as ET
import glob

android_res = "app/src/main/res"
ios_res = "ios/FamWake/Resources"

lproj_to_android = {
    "en.lproj": "values",
    "zh.lproj": "values-zh-rCN",
    "id.lproj": "values-in",
    "gsw.lproj": "values-b+gsw",
    "ksh.lproj": "values-b+ksh",
    "swg.lproj": "values-b+swg"
}

keys_to_port = ["buffer_after_bath", "buffer_between_display", "schedule_message_buffer_reduced"]

for ios_file in glob.glob(f"{ios_res}/*.lproj/Localizable.strings"):
    lproj = os.path.basename(os.path.dirname(ios_file))
    
    if lproj in lproj_to_android:
        android_dir = lproj_to_android[lproj]
    else:
        lang_code = lproj.replace(".lproj", "")
        android_dir = f"values-{lang_code}"
        
    strings_xml = os.path.join(android_res, android_dir, "strings.xml")
    if not os.path.exists(strings_xml):
        print(f"Cannot find android {strings_xml} for {lproj}")
        continue
        
    try:
        tree = ET.parse(strings_xml)
        root = tree.getroot()
        
        for key in keys_to_port:
            node = root.find(f".//string[@name='{key}']")
            if node is not None:
                val = node.text
                if val:
                    val = val.replace("\\'", "'").replace('\"', '"').replace('%1$s', '%1$@').replace('%2$s', '%2$@').replace('%1$d', '%d').replace('%2$d', '%d').replace('%d', '%d').replace('%s', '%@')
                    with open(ios_file, 'a', encoding='utf-8') as f:
                        f.write(f'\n"{key}" = "{val}";\n')
                    print(f"Added to {lproj}: {val}")
            else:
                print(f"Key {key} missing in {strings_xml}")
    except Exception as e:
        print(f"Error parsing {strings_xml}: {e}")

