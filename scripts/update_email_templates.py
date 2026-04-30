#!/usr/bin/env python3
import os
import time
import re
from deep_translator import GoogleTranslator

# Target languages
NEW_LANGS = ['id', 'vi', 'bn', 'mr', 'hi', 'zh-CN', 'ko', 'ruhr', 'swg', 'gsw']

# Since Google Translate doesn't support 'ruhr', 'swg', 'gsw', we map them to 'de' to avoid errors
LANG_MAP = {
    'ruhr': 'de',
    'swg': 'de',
    'gsw': 'de',
    'zh-CN': 'zh-CN'
}

def get_trans_lang(lang):
    return LANG_MAP.get(lang, lang)

def translate_dict(en_dict, lang):
    t = GoogleTranslator(source='en', target=get_trans_lang(lang))
    res = {}
    for key, text in en_dict.items():
        if "<strong>FamWake</strong>" in text:
            # Simple replacement so it doesn't break HTML tags, translate around it
            parts = text.split("<strong>FamWake</strong>")
            trans_parts = []
            for part in parts:
                if part.strip():
                    try:
                        trans_parts.append(t.translate(part))
                    except:
                        trans_parts.append(part)
                else:
                    trans_parts.append(part)
            res[key] = "<strong>FamWake</strong>".join(trans_parts)
        elif "daniel.notthoff@gmail.com" in text:
            parts = text.split("daniel.notthoff@gmail.com")
            trans_parts = []
            for part in parts:
                if part.strip():
                    try:
                        # protect tags if any
                        clean_part = part.replace('<strong>', '||S||').replace('</strong>', '||E||')
                        tr = t.translate(clean_part)
                        if tr:
                            tr = tr.replace('||S||', '<strong>').replace('||E||', '</strong>')
                            trans_parts.append(tr)
                        else:
                            trans_parts.append(part)
                    except:
                        trans_parts.append(part)
                else:
                    trans_parts.append(part)
            res[key] = "daniel.notthoff@gmail.com".join(trans_parts)
        else:
            try:
                # protect tags
                clean_text = text.replace('<strong>', '||S||').replace('</strong>', '||E||')
                tr = t.translate(clean_text)
                if tr:
                    tr = tr.replace('||S||', '<strong>').replace('||E||', '</strong>')
                    res[key] = tr
                else:
                    res[key] = text
            except:
                res[key] = text
        time.sleep(0.5)
    return res

EN_EMAIL_CONTENT = {
    "subject": "🔑 Reset your password – FamWake Family Alarm",
    "appName": "<strong>FamWake</strong> Family Alarm",
    "greeting": "Hello!",
    "intro": "We received a request to reset the password for your <strong>FamWake</strong> Family Alarm account.",
    "instruction": "Click the button below to set a new password:",
    "button": "Reset Password",
    "fallback": "If the button doesn't work, please paste this link into your browser:",
    "security": "⚠️ If you did <strong>not</strong> request this, you can safely ignore this email. Your password will remain unchanged. If you notice any suspicious activity, please contact us at: daniel.notthoff@gmail.com",
    "footerNote": "This is an automated message. Please do not reply directly to this email."
}

EN_EMAIL_CONTENT_CONFIRM = {
    "subject": "✅ Password successfully changed – FamWake Family Alarm",
    "appName": "<strong>FamWake</strong> Family Alarm",
    "greeting": "Hello!",
    "intro": "The password for your <strong>FamWake</strong> Family Alarm account has been successfully changed.",
    "instruction": "You can now log in to the app with your new password.",
    "security": "⚠️ If you did <strong>not</strong> change your password, please contact us immediately: daniel.notthoff@gmail.com",
    "footerNote": "This is an automated message. Please do not reply directly to this email."
}

EN_EMAIL_CONTENT_VERIFY = {
    "subject": "🚀 Confirm your FamWake Family Alarm account",
    "appName": "<strong>FamWake</strong> Family Alarm",
    "greeting": "Welcome to FamWake Family Alarm!",
    "intro": "Thank you for registering with <strong>FamWake</strong> Family Alarm. We look forward to helping you and your family start the day stress-free!",
    "instruction": "Please confirm your email address to activate your account:",
    "button": "Confirm email address",
    "fallback": "If the button doesn't work, please paste this link into your browser:",
    "privacy": "<strong>Note:</strong> For privacy reasons, this link and your unconfirmed registration data will be automatically deleted after 48 hours if no activation occurs.",
    "security": "If you did not create this account, you can safely ignore this email. No data will be permanently stored.",
    "footerNote": "This is an automated message. Please do not reply directly to this email."
}

def format_dict(d):
    res = "  {\n"
    for k, v in d.items():
        res += f'    {k}: "{v}",\n'
    res += "  }"
    return res

def add_langs_to_file(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    for lang in NEW_LANGS:
        print(f"Translating for {lang}...")
        
        # Dialect overwrites (use German versions)
        if lang in ['ruhr', 'swg', 'gsw']:
            # We skip them because usually dialekts in emails are handled by Fallback or we just copy German
            continue

        print("  - EMAIL_CONTENT")
        trans_content = translate_dict(EN_EMAIL_CONTENT, lang)
        print("  - EMAIL_CONTENT_CONFIRM")
        trans_confirm = translate_dict(EN_EMAIL_CONTENT_CONFIRM, lang)
        print("  - EMAIL_CONTENT_VERIFY")
        trans_verify = translate_dict(EN_EMAIL_CONTENT_VERIFY, lang)

        # Inject into EMAIL_CONTENT
        if f"  {lang}: {{" not in content and f"  '{lang}': {{" not in content:
            inject_str = f"  '{lang}': {format_dict(trans_content)[2:]},\n"
            content = re.sub(r'(const EMAIL_CONTENT = \{.*?)(};\n)', r'\1' + inject_str + r'\2', content, flags=re.DOTALL)

        # Inject into EMAIL_CONTENT_CONFIRM
        if f"  {lang}: {{" not in content[content.find("const EMAIL_CONTENT_CONFIRM = {"):] and f"  '{lang}': {{" not in content[content.find("const EMAIL_CONTENT_CONFIRM = {"):]:
            inject_str = f"  '{lang}': {format_dict(trans_confirm)[2:]},\n"
            content = re.sub(r'(const EMAIL_CONTENT_CONFIRM = \{.*?)(};\n)', r'\1' + inject_str + r'\2', content, flags=re.DOTALL)

        # Inject into EMAIL_CONTENT_VERIFY
        if f"  {lang}: {{" not in content[content.find("const EMAIL_CONTENT_VERIFY = {"):] and f"  '{lang}': {{" not in content[content.find("const EMAIL_CONTENT_VERIFY = {"):]:
            inject_str = f"  '{lang}': {format_dict(trans_verify)[2:]},\n"
            content = re.sub(r'(const EMAIL_CONTENT_VERIFY = \{.*?)(};\n)', r'\1' + inject_str + r'\2', content, flags=re.DOTALL)

    # Manual add for German Dialects by copying 'de' text from the file if needed, but the user didn't mention it.
    
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)

if __name__ == "__main__":
    add_langs_to_file("functions/index.js")
    print("Done generating email templates in index.js")
