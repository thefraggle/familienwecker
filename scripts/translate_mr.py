import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time
import re

def translate_xml_mr():
    lang_code = 'mr'
    print(f"Translating {lang_code}...")
    source_path = 'app/src/main/res/values/strings.xml'
    target_path = 'app/src/main/res/values-mr/strings.xml'
    
    os.makedirs(os.path.dirname(target_path), exist_ok=True)
    
    tree = ET.parse(source_path)
    root = tree.getroot()
    translator = GoogleTranslator(source='en', target='mr')
    
    pattern = r'(%\d\$[sd]|%[sd]|\\n|\\\'|\\\"|&amp;|&lt;|&gt;)'
    
    def do_translation(text):
        if not text: return text
        placeholders = []
        def replacer(match):
            placeholders.append(match.group(0))
            return f"[P{len(placeholders)-1}]"
        
        protected = re.sub(pattern, replacer, text)
        
        for attempt in range(3):
            try:
                translated = translator.translate(protected)
                if not translated:
                    translated = protected
                
                for i, p in enumerate(placeholders):
                    translated = translated.replace(f"[P{i}]", p)
                    translated = translated.replace(f"[ P{i} ]", p)
                    translated = translated.replace(f"[P {i}]", p)
                    translated = translated.replace(f"[ P{i}]", p)
                return translated
            except Exception as e:
                time.sleep(1)
        return text

    total = 0
    for child in root:
        if child.tag == 'string':
            total += 1
        elif child.tag == 'string-array':
            for item in child:
                total += 1

    count = 0
    for child in root:
        if child.tag == 'string':
            if child.attrib.get('name') not in ['app_name_short', 'settings_footer_version']:
                child.text = do_translation(child.text)
            count += 1
            if count % 20 == 0:
                print(f"Translated {count}/{total}")
        elif child.tag == 'string-array':
            for item in child:
                item.text = do_translation(item.text)
                count += 1
                if count % 20 == 0:
                    print(f"Translated {count}/{total}")

    tree.write(target_path, encoding='utf-8', xml_declaration=True)
    print(f"Saved {target_path}")

if __name__ == '__main__':
    translate_xml_mr()
