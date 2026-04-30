import os
import xml.etree.ElementTree as ET
import translators as ts
import re
import time

def translate_xml(source_path, target_path, lang_code):
    print(f"Translating {lang_code}...")
    tree = ET.parse(source_path)
    root = tree.getroot()
    
    elements_to_translate = []
    texts_to_translate = []
    
    for child in root:
        if child.tag == 'string':
            text = child.text
            if text and child.attrib.get('name') not in ['app_name_short', 'settings_footer_version']:
                elements_to_translate.append(child)
                texts_to_translate.append(text)
        elif child.tag == 'string-array':
            for item in child:
                text = item.text
                if text:
                    elements_to_translate.append(item)
                    texts_to_translate.append(text)
                    
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

    # Translate
    # translators package can translate text. We join with "|||"
    batch_text = " ||| ".join(protected_texts)
    
    # Need to split into smaller chunks (e.g. 2000 chars)
    # Actually bing supports up to 1000 chars easily, Google 5000.
    chunks = []
    current_chunk = []
    current_len = 0
    for pt in protected_texts:
        if current_len + len(pt) > 4000:
            chunks.append(current_chunk)
            current_chunk = [pt]
            current_len = len(pt)
        else:
            current_chunk.append(pt)
            current_len += len(pt) + 5 # " ||| "
    if current_chunk:
        chunks.append(current_chunk)
        
    translated_texts = []
    try:
        for i, chunk in enumerate(chunks):
            print(f"Translating chunk {i+1}/{len(chunks)} for {lang_code}...")
            joined = " ||| ".join(chunk)
            # using 'google' engine
            res = ts.translate_text(joined, translator='google', from_language='en', to_language=lang_code)
            res_split = [s.strip() for s in res.split("|||")]
            if len(res_split) != len(chunk):
                print(f"Warning: Chunk length mismatch. Expected {len(chunk)}, got {len(res_split)}. Falling back to element-by-element.")
                # Fallback to individual
                for t in chunk:
                    translated_texts.append(ts.translate_text(t, translator='google', from_language='en', to_language=lang_code))
            else:
                translated_texts.extend(res_split)
            time.sleep(1)
    except Exception as e:
        print(f"Translation failed: {e}")
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
        final_texts.append(translated)
        
    for element, translated_text in zip(elements_to_translate, final_texts):
        element.text = translated_text
        
    tree.write(target_path, encoding='utf-8', xml_declaration=True)
    print(f"Saved {target_path}")

def main():
    source = 'app/src/main/res/values/strings.xml'
    targets = {
        'id': 'app/src/main/res/values-id/strings.xml',
        'vi': 'app/src/main/res/values-vi/strings.xml',
        'bn': 'app/src/main/res/values-bn/strings.xml',
        'mr': 'app/src/main/res/values-mr/strings.xml',
        'hi': 'app/src/main/res/values-hi/strings.xml'
    }
    
    for lang, path in targets.items():
        if not os.path.exists(os.path.dirname(path)):
            os.makedirs(os.path.dirname(path), exist_ok=True)
        translate_xml(source, path, lang)

if __name__ == '__main__':
    main()
