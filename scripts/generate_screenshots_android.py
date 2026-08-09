import os
import time
import shutil
import re
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from deep_translator import GoogleTranslator

# Base directories
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEVICES_DIR = os.path.join(PROJECT_ROOT, "docs/internal/images/screenshots/devices")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "docs/internal/images/screenshots/android")
BACKGROUND_PATH = os.path.join(PROJECT_ROOT, "docs/internal/images/hintergrund_hochformat.png")

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

# Output display size (Google Play Store standard portrait is 1080x1920, 16:9)
TARGET_SIZE = (1080, 1920)

# Bounding box of the clean screen inside the old 1187x2513 template mockup
SCREEN_CROP_BOX = (63, 195, 1124, 2390)

# Verified German and English screenshot text lists (matching original layouts)
VERIFIED_SS = {
    "de": [
        ("Nie wieder\nBad-Stau!", "Der smarte Plan für Bad, Frühstück\nund ganz entspanntes Aufstehen."),
        ("Passend\nfür jeden", "Die App plant die Badezimmer-Zeiten\nfair und ganz ohne Wartezeit."),
        ("Guten Morgen,\nPapa!", "Persönliche Weckrufe mit dem\nsüßen Panda-Maskottchen."),
        ("Länger\nausschlafen", "Wochenende? Feiertag? FamWake lässt\ndich automatisch länger schlafen."),
        ("Blitzschnell\nvernetzt", "Code teilen. Familie einladen.\nEntspannt in den Tag starten."),
        ("Gemeinsam\nfrühstücken", "FamWake plant euren Morgen –\nvom Aufstehen bis zum Frühstück.")
    ],
    "en": [
        ("No More\nMorning Chaos!", "The smart plan for bath, breakfast\nand a relaxed wake-up."),
        ("Perfect\nfor Everyone", "The app plans bathroom times\nfairly — no more waiting."),
        ("Good Morning,\nDad!", "Personal wake-up calls with the\ncute panda mascot."),
        ("Sleep In\nLonger", "Weekends? Holidays? FamWake lets\nyou sleep in automatically."),
        ("Instantly\nConnected", "Share code. Invite family.\nStart your day relaxed."),
        ("Breakfast\nTogether", "FamWake plans your morning –\nfrom wake-up to breakfast.")
    ]
}

# Source templates
SLIDES_TEMPLATES = [
    {
        "device_file": "main_scrolled.png",
        "layout": "bottom"
    },
    {
        "device_file": "times.png",
        "layout": "bottom"
    },
    {
        "device_file": "alarm.png",
        "layout": "top"
    },
    {
        "device_file": "pause.png",
        "layout": "bottom"
    },
    {
        "device_file": "share.png",
        "layout": "bottom"
    },
    {
        "device_file": "main_full.png",
        "layout": "top"
    }
]

# Total 22 app languages
LANGUAGES = [
    "bn", "da", "de", "en", "es", "fr", "hi", "id", "it", "ja", "ko",
    "mr", "nl", "no", "pl", "pt", "ru", "sv", "tr", "uk", "vi", "zh-CN"
]

# Android Locale Map (Google Play Store languages mapping)
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
    "pl": ["pl-PL"],
    "pt": ["pt-PT", "pt-BR"],
    "ru": ["ru-RU"],
    "sv": ["sv-SE"],
    "tr": ["tr-TR"],
    "uk": ["uk"],
    "vi": ["vi"],
    "zh-CN": ["zh-CN"]
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

def draw_bottom_vignette(img):
    # Draw a gradient at the bottom to increase text readability on light backgrounds
    w, h = img.size
    overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    
    # We create a gradient from Y=1150 (alpha=0) to Y=1920 (alpha=170)
    start_y = 1150
    end_y = 1920
    for y in range(start_y, end_y):
        alpha = int(170 * ((y - start_y) / (end_y - start_y)))
        # Dark indigo matching the app's dark theme
        draw.line([(0, y), (w, y)], fill=(11, 17, 30, alpha))
        
    return Image.alpha_composite(img.convert("RGBA"), overlay)

def draw_android_status_bar(draw, clock_font, fill_color):
    # Left clock: 10:00 (Android default)
    draw.text((80, 68), "10:00", font=clock_font, fill=fill_color)
    
    # Right battery body
    draw.rectangle([1055, 70, 1070, 86], outline=fill_color, width=2)
    draw.rectangle([1059, 66, 1066, 70], fill=fill_color)
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
    
    # Fill status bar background to cover top area
    bg_color = (r, g, b, 255)
    screen_draw.rectangle([25, 25, 1105, 125], fill=bg_color)
    
    fill_color = "#FFFFFF" if is_dark else "#000000"
    
    draw_android_status_bar(screen_draw, font_bold, fill_color)
    
    # Punch hole camera centered at X=565, Y=65 (radius=16)
    screen_draw.ellipse([565 - 16, 65 - 16, 565 + 16, 65 + 16], fill="#000000")
    
    # Android navigation bar line at the bottom
    try:
        bot_r, bot_g, bot_b = ss_resized.getpixel((540, 2220))[:3]
        bot_brightness = 0.299 * bot_r + 0.587 * bot_g + 0.114 * bot_b
        nav_color = "#FFFFFF" if bot_brightness < 128 else "#000000"
    except Exception:
        nav_color = "#FFFFFF"
        
    screen_draw.rounded_rectangle([565 - 140, 2257, 565 + 140, 2261], radius=2, fill=nav_color)
    
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

def build_screenshot(slide, lang, target_size):
    img = fit_background(BACKGROUND_PATH, target_size)
    
    layout = slide["layout"]
    if layout == "top":
        img = draw_bottom_vignette(img)
        
    draw = ImageDraw.Draw(img)
    
    # 1080x1920 absolute design specifications
    h_size = 100
    d_size = 44
    
    if lang in ("ja", "zh-CN"):
        font_h = ImageFont.truetype(SYSTEM_JAPANESE, h_size, index=2)
        font_d = ImageFont.truetype(SYSTEM_JAPANESE, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_JAPANESE, 34, index=2)
    elif lang == "ko":
        font_h = ImageFont.truetype(SYSTEM_KOREAN, h_size, index=6)
        font_d = ImageFont.truetype(SYSTEM_KOREAN, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_KOREAN, 34, index=6)
    elif lang in ("hi", "mr"):
        font_h = ImageFont.truetype(SYSTEM_DEVANAGARI, h_size, index=1)
        font_d = ImageFont.truetype(SYSTEM_DEVANAGARI, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_DEVANAGARI, 34, index=1)
    elif lang == "bn":
        font_h = ImageFont.truetype(SYSTEM_BENGALI, h_size, index=1)
        font_d = ImageFont.truetype(SYSTEM_BENGALI, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_BENGALI, 34, index=1)
    elif lang in ("ru", "uk", "pl", "tr", "vi"):
        font_h = ImageFont.truetype(SYSTEM_HELVETICA, h_size, index=1)
        font_d = ImageFont.truetype(SYSTEM_HELVETICA, d_size, index=0)
        font_status = ImageFont.truetype(SYSTEM_HELVETICA, 34, index=1)
    else:
        try:
            font_h = ImageFont.truetype(FONT_BOLD_PATH, h_size)
            font_d = ImageFont.truetype(FONT_REGULAR_PATH, d_size)
            font_status = ImageFont.truetype(FONT_BOLD_PATH, 34)
        except Exception as e:
            print(f"Font loading failed: {e}. Falling back to Helvetica.")
            font_h = ImageFont.truetype(SYSTEM_HELVETICA, h_size, index=1)
            font_d = ImageFont.truetype(SYSTEM_HELVETICA, d_size, index=0)
            font_status = ImageFont.truetype(SYSTEM_HELVETICA, 34, index=1)

    device_file = slide["device_file"]
    
    # Locate raw screenshot (always read from persistent iOS screenshots folder)
    device_path = os.path.join(DEVICES_DIR, lang, device_file)
    if not os.path.exists(device_path):
        device_path = os.path.join(DEVICES_DIR, "en", device_file)
        if not os.path.exists(device_path):
            print(f"Device file {device_file} not found, skipping!")
            return None

    with Image.open(device_path) as dev_img:
        w, h = dev_img.size
        if dev_img.size == (1187, 2513):
            screen_img = dev_img.crop(SCREEN_CROP_BOX)
        elif w == 1290 and h == 2796:
            # Keep top intact (Y=0) to prevent cutting off the app header text, crop only bottom home indicator
            screen_img = dev_img.crop((0, 0, 1290, 2796 - 60))
        elif w == 1242 and h == 2688:
            screen_img = dev_img.crop((0, 0, 1242, 2688 - 50))
        else:
            screen_img = dev_img.crop((0, 0, w, int(h * 0.98)))
        
    # Create framed Android mockup
    phone = create_android_mockup(screen_img, font_status)
    
    # Scale phone mockup to occupy almost full width (880px on 1080px wide canvas)
    phone_tgt_w = 880
    phone_ratio = phone.width / phone.height
    phone_tgt_h = int(phone_tgt_w / phone_ratio)
    phone = phone.resize((phone_tgt_w, phone_tgt_h), Image.Resampling.LANCZOS)
    
    # Create drop shadow
    shadow_blur_radius = 24
    shadow = Image.new('RGBA', phone.size, (0, 0, 0, 0))
    alpha = phone.split()[3]
    shadow.paste((0, 0, 0, 120), mask=alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(shadow_blur_radius))
    
    # Wrap text
    max_text_width = 920  # Margin 80 on each side
    wrapped_headline = wrap_text(slide["headline"], font_h, max_text_width, draw)
    wrapped_desc = wrap_text(slide["description"], font_d, max_text_width, draw)
    
    layout = slide["layout"]
    text_x = 80
    
    if layout == "bottom":
        # Text at top, Phone at bottom running off screen
        text_y = 120
        draw.text((text_x, text_y), wrapped_headline, font=font_h, fill=ACCENT_COLOR, spacing=15)
        bbox_h = draw.textbbox((text_x, text_y), wrapped_headline, font=font_h)
        desc_y = bbox_h[3] + 35
        draw.text((text_x, desc_y), wrapped_desc, font=font_d, fill=TEXT_COLOR, spacing=15)
        
        paste_x = (1080 - phone.width) // 2
        paste_y = 630
    else:  # layout == "top"
        # Phone at top running off screen, Text at bottom
        paste_x = (1080 - phone.width) // 2
        paste_y = -470
        
        text_y = 1370
        draw.text((text_x, text_y), wrapped_headline, font=font_h, fill=ACCENT_COLOR, spacing=15)
        bbox_h = draw.textbbox((text_x, text_y), wrapped_headline, font=font_h)
        desc_y = bbox_h[3] + 35
        draw.text((text_x, desc_y), wrapped_desc, font=font_d, fill=TEXT_COLOR, spacing=15)
        
    shadow_offset = (0, 15)
    img.paste(shadow, (paste_x + shadow_offset[0], paste_y + shadow_offset[1]), shadow)
    img.paste(phone, (paste_x, paste_y), phone)
    
    if img.mode == 'RGBA':
        img = img.convert('RGB')
        
    return img

def main():
    android_metadata_dir = os.path.join(PROJECT_ROOT, "android/fastlane/metadata/android")
    
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"Target root docs directory: {OUTPUT_DIR}")
    
    for lang in LANGUAGES:
        if lang not in LOCALE_MAP:
            continue
            
        lang_output_dir = os.path.join(OUTPUT_DIR, lang)
        os.makedirs(lang_output_dir, exist_ok=True)
        
        if lang in VERIFIED_SS:
            slides_text = VERIFIED_SS[lang]
        else:
            print(f"Translating texts for Android language '{lang}' dynamically...")
            slides_text = []
            for hl, tx in VERIFIED_SS["en"]:
                t_hl = translate_text(hl, lang)
                t_tx = translate_text(tx, lang)
                slides_text.append((t_hl, t_tx))
                time.sleep(0.15)
                
        slides = []
        for idx, temp in enumerate(SLIDES_TEMPLATES):
            slides.append({
                "headline": slides_text[idx][0],
                "description": slides_text[idx][1],
                "device_file": temp["device_file"],
                "layout": temp["layout"]
            })
            
        target_locales = LOCALE_MAP[lang]
        
        print(f"Generating Android screenshots for '{lang}'...")
        for idx, slide in enumerate(slides):
            img = build_screenshot(slide, lang, TARGET_SIZE)
            if img:
                # 1. Save to docs/android/{lang}/
                filename = f"screenshot_{idx+1}_{lang}.jpg"
                save_path = os.path.join(lang_output_dir, filename)
                img.save(save_path, "JPEG", quality=93)
                
                # 2. Save directly to android/fastlane/metadata/android/{locale}/images/phoneScreenshots/
                for locale in target_locales:
                    fastlane_lang_dir = os.path.join(android_metadata_dir, locale, "images", "phoneScreenshots")
                    os.makedirs(fastlane_lang_dir, exist_ok=True)
                    fastlane_filename = f"0{idx+1}_Slide.png"
                    fastlane_save_path = os.path.join(fastlane_lang_dir, fastlane_filename)
                    img.save(fastlane_save_path, "PNG")
                    
        print(f"  Done Android '{lang}'")

if __name__ == "__main__":
    main()
