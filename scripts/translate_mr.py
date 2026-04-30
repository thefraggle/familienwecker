import os
import xml.etree.ElementTree as ET
import translators as ts
import re

def translate_xml_mr():
    lang_code = 'mr'
    print(f"Translating {lang_code}...")
    source_path = 'app/src/main/res/values/strings.xml'
    target_path = 'app/src/main/res/values-mr/strings.xml'
    
    tree = ET.parse(source_path)
    root = tree.getroot()
    
    pattern = r'(%\d\$[sd]|%[sd]|\\n|\\\'|\\\"|&amp;|&lt;|&gt;)'
    
    for child in root:
        if child.tag == 'string':
            text = child.text
            if text and child.attrib.get('name') not in ['app_name_short', 'settings_footer_version']:
                try:
                    res = ts.translate_text(text, translator='bing', from_language='en', to_language=lang_code)
                    child.text = res
                except:
                    pass
        elif child.tag == 'string-array':
            for item in child:
                text = item.text
                if text:
                    try:
                        res = ts.translate_text(text, translator='bing', from_language='en', to_language=lang_code)
                        item.text = res
                    except:
                        pass
                        
    tree.write(target_path, encoding='utf-8', xml_declaration=True)
    print(f"Saved {target_path}")

if __name__ == '__main__':
    translate_xml_mr()
