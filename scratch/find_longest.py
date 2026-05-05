import os
import xml.etree.ElementTree as ET

base_dir = "app/src/main/res"
keys_to_check = [
    "login_button",
    "register_button",
    "registration_disclaimer",
    "already_have_account",
    "no_account",
    "login_forgot_password"
]

results = []

for item in os.listdir(base_dir):
    if item == "values" or item.startswith("values-"):
        strings_file = os.path.join(base_dir, item, "strings.xml")
        if os.path.isfile(strings_file):
            try:
                tree = ET.parse(strings_file)
                root = tree.getroot()
                total_length = 0
                lengths = {}
                for child in root:
                    if child.tag == "string" and child.attrib.get("name") in keys_to_check:
                        text = "".join(child.itertext())
                        total_length += len(text)
                        lengths[child.attrib.get("name")] = len(text)
                results.append((total_length, item, lengths))
            except Exception as e:
                pass

results.sort(reverse=True, key=lambda x: x[0])

for total, lang, lengths in results[:5]:
    print(f"Language: {lang.replace('values-', '') if lang != 'values' else 'en'} - Total Length: {total}")
    for k, v in lengths.items():
        print(f"  {k}: {v}")
    print()
