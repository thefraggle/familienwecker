import os
import xml.etree.ElementTree as ET
import translators as ts
import re
import time

KEYS_TO_TRANSLATE = [
    "already_have_account",
    "no_account",
    "onboarding_done",
    "onboarding_login_create",
    "onboarding_slide5_title",
    "onboarding_slide5_body",
    "onboarding_slide4_title",
    "onboarding_slide4_body",
    "settings_anonymous_login_button"
]

def translate_xml(source_path, target_path, lang_code):
    print(f"Translating {lang_code}...")
    
    # Read the target tree to preserve existing translations and only update the keys we want
    target_tree = ET.parse(target_path)
    target_root = target_tree.getroot()
    
    # Read source tree to get the English text
    source_tree = ET.parse(source_path)
    source_root = source_tree.getroot()
    
    source_texts = {}
    for child in source_root:
        if child.tag == 'string' and child.attrib.get('name') in KEYS_TO_TRANSLATE:
            source_texts[child.attrib.get('name')] = child.text
            
    elements_to_translate = []
    texts_to_translate = []
    
    # Find the matching elements in the target XML
    target_elements_dict = {child.attrib.get('name'): child for child in target_root if child.tag == 'string'}
    
    for key in KEYS_TO_TRANSLATE:
        if key in source_texts:
            if key in target_elements_dict:
                elements_to_translate.append(target_elements_dict[key])
                texts_to_translate.append(source_texts[key])
            else:
                # Create missing element
                new_element = ET.SubElement(target_root, 'string', name=key)
                # Pretty print formatting: add a newline and indent before the new element
                new_element.tail = '\n    '
                elements_to_translate.append(new_element)
                texts_to_translate.append(source_texts[key])
                
    if not texts_to_translate:
        print(f"No keys to translate found in source for {target_path}")
        return

    # Protect placeholders
    pattern = r'(%\d\$[sd]|%[sd]|\\n|\\\'|\\\"|&amp;|&lt;|&gt;)'
    protected_texts = []
    all_placeholders = []
    for text in texts_to_translate:
        placeholders = []
        def replacer(match):
            placeholders.append(match.group(0))
            return f"[P{len(placeholders)-1}]"
        protected_texts.append(re.sub(pattern, replacer, text))
        all_placeholders.append(placeholders)

    # Translate individual elements
    translated_texts = []
    try:
        for t in protected_texts:
            res = ts.translate_text(t, translator='google', from_language='en', to_language=lang_code)
            translated_texts.append(res)
            time.sleep(0.5)
    except Exception as e:
        print(f"Translation failed for {lang_code}: {e}")
        return

    # Restore placeholders
    final_texts = []
    for translated, placeholders in zip(translated_texts, all_placeholders):
        if translated is None:
            translated = ""
        for i, p in enumerate(placeholders):
            translated = translated.replace(f"[P{i}]", p)
            translated = translated.replace(f"[ P{i} ]", p)
            translated = translated.replace(f"[P {i}]", p)
            translated = translated.replace(f"[ P{i}]", p)
            
        # Post-process for XML validity (e.g. escaping quotes)
        # We don't want to re-escape single quotes if they are already escaped, but google translate might mess them up.
        # Simple fix: unescape then re-escape
        translated = translated.replace("\\'", "'").replace("'", "\\'")
        final_texts.append(translated)
        
    for element, translated_text in zip(elements_to_translate, final_texts):
        element.text = translated_text
        
    target_tree.write(target_path, encoding='utf-8', xml_declaration=True)
    print(f"Saved {target_path}")

def main():
    source = 'app/src/main/res/values/strings.xml'
    res_dir = 'app/src/main/res'
    
    # Find all values-* directories
    target_dirs = [d for d in os.listdir(res_dir) if d.startswith('values-') and os.path.isdir(os.path.join(res_dir, d))]
    
    # Exclude values-de (we did it manually)
    if 'values-de' in target_dirs:
        target_dirs.remove('values-de')
        
    for d in target_dirs:
        # Extract lang code (e.g. 'values-fr' -> 'fr', 'values-zh-rCN' -> 'zh-CN')
        parts = d.split('-')
        if len(parts) == 2:
            lang = parts[1]
        elif len(parts) == 3:
            lang = f"{parts[1]}-{parts[2].replace('r', '')}"
        else:
            continue
            
        # special case for google translate code mapping
        if lang == 'in': lang = 'id'
        if lang == 'b+gsw': continue # Swiss German usually not supported well
        if lang == 'b+swg': continue # Swabian
        if lang == 'b+ksh': continue # Colognian
        
        path = os.path.join(res_dir, d, 'strings.xml')
        if os.path.exists(path):
            translate_xml(source, path, lang)

if __name__ == '__main__':
    main()
