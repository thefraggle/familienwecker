import os
import time
import shutil
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from deep_translator import GoogleTranslator

# Base directories
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEVICES_DIR = os.path.join(PROJECT_ROOT, "docs/internal/images/screenshots/devices")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "docs/internal/images/feature_graphics")
BACKGROUND_PATH = os.path.join(PROJECT_ROOT, "docs/internal/images/hintergrund_quer.png")

# Font paths
FONT_REGULAR_PATH = os.path.join(PROJECT_ROOT, "app/src/main/res/font/nunito_regular.ttf")
FONT_BOLD_PATH = os.path.join(PROJECT_ROOT, "app/src/main/res/font/nunito_bold.ttf")

SYSTEM_HELVETICA = "/System/Library/Fonts/Helvetica.ttc"
SYSTEM_JAPANESE = "/System/Library/Fonts/Hiragino Sans GB.ttc"
SYSTEM_KOREAN = "/System/Library/Fonts/AppleSDGothicNeo.ttc"
SYSTEM_DEVANAGARI = "/System/Library/Fonts/Supplemental/Devanagari Sangam MN.ttc"
SYSTEM_BENGALI = "/System/Library/Fonts/Supplemental/Bangla Sangam MN.ttc"

# Design Constants
ACCENT_COLOR = "#FF8C42"  # Sunrise Orange
TEXT_COLOR = "#FFFFFF"    # White

# Canvas size for Play Store feature graphic
TARGET_SIZE = (1024, 500)

# Total 22 app languages
LANGUAGES = [
    "bn", "da", "de", "en", "es", "fr", "hi", "id", "it", "ja", "ko",
    "mr", "nl", "no", "nb", "pl", "pt", "ru", "sv", "tr", "uk", "vi", "zh-CN"
]

# Android Locale Map
LOCALE_MAP = {
    "bn": ["bn-BD"],
    "da": ["da-DK"],
    "de": ["de-DE"],
    "en": ["en-US", "en-GB", "en-IN"],
    "es": ["es-ES", "es-419", "es-US"],
    "fr": ["fr-FR", "fr-CA"],
    "hi": ["hi-IN"],
    "id": ["id"],
    "it": ["it-IT"],
    "ja": ["ja-JP"],
    "ko": ["ko-KR"],
    "mr": ["mr-IN"],
    "nl": ["nl-NL"],
    "no": ["no-NO", "nb-NO"],
    "nb": ["no-NO", "nb-NO"],
    "pl": ["pl-PL"],
    "pt": ["pt-PT", "pt-BR"],
    "ru": ["ru-RU"],
    "sv": ["sv-SE"],
    "tr": ["tr-TR"],
    "uk": ["uk"],
    "vi": ["vi"],
    "zh-CN": ["zh-CN"]
}

VERIFIED_FG = {
    "de": {
        "title_1": "FamWake",
        "title_2": "Familienwecker",
        "subtitle": "Schluss mit dem Morgen-Chaos",
        "desc": "Der smarte Plan für Bad, Frühstück und\nentspanntes Aufstehen."
    },
    "en": {
        "title_1": "FamWake",
        "title_2": "Family Alarm Clock",
        "subtitle": "No More Morning Chaos",
        "desc": "The smart plan for bath, breakfast and\na relaxed wake-up."
    },
    "no": {
        "title_1": "FamWake",
        "title_2": "Familievekker",
        "subtitle": "Ingen mer morgenkaos",
        "desc": "Den smarte planen for bad, frokost og\nen avslappet start på dagen."
    },
    "nb": {
        "title_1": "FamWake",
        "title_2": "Familievekker",
        "subtitle": "Ingen mer morgenkaos",
        "desc": "Den smarte planen for bad, frokost og\nen avslappet start på dagen."
    }
}

def translate_text(text, target_lang):
    if not text.strip(): return text
    try:
        translated = GoogleTranslator(source='en', target=target_lang).translate(text.replace("\n", " ||| "))
        return translated.replace(" ||| ", "\n").replace("|||", "\n")
    except Exception as e:
        print(f"Translation error to {target_lang}: {e}. Fallback to English.")
        return text

def fit_background(bg_path, target_size):
    bg = Image.open(bg_path)
    bg_w, bg_h = bg.size
    tgt_w, tgt_h = target_size
    
    ratio = max(tgt_w / bg_w, tgt_h / bg_h)
    new_w = int(bg_w * ratio)
    new_h = int(bg_h * ratio)
    bg_resized = bg.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    left = (new_w - tgt_w) // 2
    top = (new_h - tgt_h) // 2
    right = left + tgt_w
    bottom = top + tgt_h
    return bg_resized.crop((left, top, right, bottom))

def draw_android_status_bar(draw, clock_font, fill_color):
    # Left clock: 10:00 (Android default)
    draw.text((80, 68), "10:00", font=clock_font, fill=fill_color)
    
    # Right battery body
    draw.rectangle([1055, 70, 1070, 86], outline=fill_color, width=2)
    # Battery tip
    draw.rectangle([1070, 74, 1073, 82], fill=fill_color)
    # Battery level (80%)
    draw.rectangle([1058, 73, 1067, 83], fill=fill_color)
    
    # WiFi icon (Android styled wedge)
    draw.polygon([(1010, 86), (1020, 70), (1030, 86)], fill=fill_color)
    
    # Signal icon (Android styled wedge)
    draw.polygon([(970, 86), (985, 70), (985, 86)], fill=fill_color)

def create_android_mockup(screen_img, font_bold):
    bezel_w, bezel_h = 1130, 2295
    mock = Image.new("RGBA", (bezel_w, bezel_h), (0, 0, 0, 0))
    
    # Resize screen to fit screen area (1080x2245)
    ss_resized = screen_img.resize((1080, 2245), Image.Resampling.LANCZOS).convert("RGBA")
    
    # Create phone outline bezel
    draw = ImageDraw.Draw(mock)
    
    # Bezel shadow / dark metal rim (Pixel style - less rounded)
    draw.rounded_rectangle([0, 0, bezel_w, bezel_h], radius=60, fill="#2C2C2C")
    # Bezel inner black
    draw.rounded_rectangle([4, 4, bezel_w-4, bezel_h-4], radius=56, fill="#000000")
    
    # Paste screenshot inside bezel
    mock.paste(ss_resized, (25, 25), ss_resized)
    
    # Draw Android components on top
    screen_draw = ImageDraw.Draw(mock)
    
    # Detect background color of the status bar from screenshot
    try:
        r, g, b = ss_resized.getpixel((50, 50))[:3]
    except Exception:
        r, g, b = 15, 23, 42
        
    brightness = 0.299 * r + 0.587 * g + 0.114 * b
    is_dark = brightness < 128
    
    # Fill status bar background to cover any raw simulator icons/text
    bg_color = (r, g, b, 255)
    screen_draw.rectangle([25, 25, 1105, 125], fill=bg_color)
    
    fill_color = "#FFFFFF" if is_dark else "#000000"
    draw_android_status_bar(screen_draw, font_bold, fill_color)
    
    # Android punch hole camera (centered at X=565, Y=65)
    screen_draw.ellipse([565 - 18, 65 - 18, 565 + 18, 65 + 18], fill="#000000")
    
    # Android bottom navigation gesture pill (centered at X=565, Y=2255)
    gesture_color = "#FFFFFF" if is_dark else "#000000"
    screen_draw.rounded_rectangle([565 - 120, 2253, 565 + 120, 2257], radius=2, fill=gesture_color)
    
    return mock

def get_text_width(text, font, draw):
    if not text: return 0
    bbox = draw.textbbox((0, 0), text, font=font)
    return bbox[2] - bbox[0]

def wrap_text(text, font, max_width, draw):
    paragraphs = text.split("\n")
    wrapped_lines = []
    
    for para in paragraphs:
        if not para.strip():
            wrapped_lines.append("")
            continue
            
        is_cjk = any(ord(char) > 0x3000 or (0xAC00 <= ord(char) <= 0xD7A3) for char in para)
        
        if is_cjk:
            current_line = ""
            for char in para:
                w = get_text_width(current_line + char, font, draw)
                if w <= max_width:
                    current_line += char
                else:
                    wrapped_lines.append(current_line)
                    current_line = char
            if current_line:
                wrapped_lines.append(current_line)
        else:
            words = para.split(" ")
            current_line = ""
            for word in words:
                test_line = (current_line + " " + word).strip()
                w = get_text_width(test_line, font, draw)
                if w <= max_width:
                    current_line = test_line
                else:
                    wrapped_lines.append(current_line)
                    current_line = word
            if current_line:
                wrapped_lines.append(current_line)
                
    return "\n".join(wrapped_lines)

def build_feature_graphic(lang, title_1, title_2, subtitle, desc):
    img = fit_background(BACKGROUND_PATH, TARGET_SIZE)
    draw = ImageDraw.Draw(img)
    
    # Font sizes
    t_size = 54
    sub_size = 38
    d_size = 22
    
    # Determine font paths based on language
    if lang in ("ja", "zh-CN"):
        font_t = ImageFont.truetype(SYSTEM_JAPANESE, t_size, index=2)
        font_sub = ImageFont.truetype(SYSTEM_JAPANESE, sub_size, index=2)
        font_d = ImageFont.truetype(SYSTEM_JAPANESE, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_JAPANESE, 34, index=2)
    elif lang == "ko":
        font_t = ImageFont.truetype(SYSTEM_KOREAN, t_size, index=6)
        font_sub = ImageFont.truetype(SYSTEM_KOREAN, sub_size, index=6)
        font_d = ImageFont.truetype(SYSTEM_KOREAN, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_KOREAN, 34, index=6)
    elif lang in ("hi", "mr"):
        font_t = ImageFont.truetype(SYSTEM_DEVANAGARI, t_size, index=1)
        font_sub = ImageFont.truetype(SYSTEM_DEVANAGARI, sub_size, index=1)
        font_d = ImageFont.truetype(SYSTEM_DEVANAGARI, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_DEVANAGARI, 34, index=1)
    elif lang == "bn":
        font_t = ImageFont.truetype(SYSTEM_BENGALI, t_size, index=1)
        font_sub = ImageFont.truetype(SYSTEM_BENGALI, sub_size, index=1)
        font_d = ImageFont.truetype(SYSTEM_BENGALI, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_BENGALI, 34, index=1)
    elif lang in ("ru", "uk", "pl", "tr", "vi"):
        font_t = ImageFont.truetype(SYSTEM_HELVETICA, t_size, index=1)
        font_sub = ImageFont.truetype(SYSTEM_HELVETICA, sub_size, index=1)
        font_d = ImageFont.truetype(SYSTEM_HELVETICA, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_HELVETICA, 34, index=1)
    else:
        try:
            font_t = ImageFont.truetype(FONT_BOLD_PATH, t_size)
            font_sub = ImageFont.truetype(FONT_BOLD_PATH, sub_size)
            font_d = ImageFont.truetype(FONT_REGULAR_PATH, d_size)
            font_status = ImageFont.truetype(FONT_BOLD_PATH, 34)
        except Exception as e:
            print(f"Font loading failed: {e}. Falling back to Helvetica.")
            font_t = ImageFont.truetype(SYSTEM_HELVETICA, t_size, index=1)
            font_sub = ImageFont.truetype(SYSTEM_HELVETICA, sub_size, index=1)
            font_d = ImageFont.truetype(SYSTEM_HELVETICA, d_size, index=0)
            font_status = ImageFont.truetype(SYSTEM_HELVETICA, 34, index=1)

    # 1. Draw Title
    text_x = 60
    draw.text((text_x, 50), title_1, font=font_t, fill=ACCENT_COLOR)
    draw.text((text_x, 110), title_2, font=font_t, fill=TEXT_COLOR)
    
    # 2. Draw Subtitle & Description (wrap description if needed)
    max_text_width = 500
    wrapped_desc = wrap_text(desc, font_d, max_text_width, draw)
    
    draw.text((text_x, 235), subtitle, font=font_sub, fill=TEXT_COLOR)
    draw.text((text_x, 305), wrapped_desc, font=font_d, fill=TEXT_COLOR, spacing=8)
    
    # 3. Add slanted phone mockup on the right side
    # Use "main_scrolled.png" (raw dashboard screen)
    device_file = "main_scrolled.png"
    device_path = os.path.join(DEVICES_DIR, lang, device_file)
    if not os.path.exists(device_path):
        device_path = os.path.join(DEVICES_DIR, "en", device_file)
        if not os.path.exists(device_path):
            print("Device main_scrolled.png raw screenshot not found, skipping phone!")
            return img

    with Image.open(device_path) as dev_img:
        screen_img = dev_img.copy()
        
    phone = create_android_mockup(screen_img, font_status)
    
    # Scale phone to height 780
    phone_h = 780
    phone_ratio = phone.width / phone.height
    phone_w = int(phone_h * phone_ratio)
    phone = phone.resize((phone_w, phone_h), Image.Resampling.LANCZOS)
    
    # Rotate phone mockup counter-clockwise by 12 degrees
    phone_rotated = phone.rotate(12, resample=Image.Resampling.BICUBIC, expand=True)
    
    # Create drop shadow for the rotated phone
    shadow = Image.new('RGBA', phone_rotated.size, (0, 0, 0, 0))
    alpha = phone_rotated.split()[3]
    shadow.paste((0, 0, 0, 90), mask=alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(16))
    
    # Calculate position for slanted phone (centered vertically, aligned right and running off-screen)
    rot_w, rot_h = phone_rotated.size
    paste_x = 1024 - rot_w + 140
    paste_y = (500 - rot_h) // 2
    
    # Paste drop shadow & phone
    img.paste(shadow, (paste_x + 10, paste_y + 12), shadow)
    img.paste(phone_rotated, (paste_x, paste_y), phone_rotated)
    
    if img.mode == 'RGBA':
        img = img.convert('RGB')
        
    return img

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"Target feature graphics directory: {OUTPUT_DIR}")
    
    for lang in LANGUAGES:
        lang_output_dir = OUTPUT_DIR
        
        # Load or translate texts
        if lang in VERIFIED_FG:
            data = VERIFIED_FG[lang]
            title_1 = data["title_1"]
            title_2 = data["title_2"]
            subtitle = data["subtitle"]
            desc = data["desc"]
        else:
            print(f"Translating Feature Graphic text for '{lang}' dynamically...")
            title_1 = "FamWake"
            title_2 = translate_text(VERIFIED_FG["en"]["title_2"], lang)
            subtitle = translate_text(VERIFIED_FG["en"]["subtitle"], lang)
            desc = translate_text(VERIFIED_FG["en"]["desc"], lang)
            time.sleep(0.15) # Rate limit protection

        # Generate feature graphic image
        img = build_feature_graphic(lang, title_1, title_2, subtitle, desc)
        
        if img:
            # 1. Save to docs/ (as PNG for preview)
            filename = f"feature_graphic_{lang}.png"
            save_path = os.path.join(lang_output_dir, filename)
            img.save(save_path, "PNG")
            
            # 2. Save directly to Google Play Store fastlane metadata
            if lang in LOCALE_MAP:
                target_locales = LOCALE_MAP[lang]
                for locale in target_locales:
                    fastlane_dir = os.path.join(PROJECT_ROOT, f"android/fastlane/metadata/android/{locale}/images")
                    os.makedirs(fastlane_dir, exist_ok=True)
                    fastlane_path = os.path.join(fastlane_dir, "featureGraphic.png")
                    img.save(fastlane_path, "PNG")
                    
            print(f"  Done Feature Graphic for '{lang}'")

if __name__ == "__main__":
    main()
