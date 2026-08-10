import os
import time
import shutil
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from deep_translator import GoogleTranslator

# Base directories
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEVICES_DIR = os.path.join(PROJECT_ROOT, "docs/internal/images/screenshots/devices")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "docs/internal/images/screenshots/ios")
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

# Output display sizes (Width, Height)
SIZES = {
    "6.5": (1242, 2688),
    "6.7": (1290, 2796)
}

# Bounding box of the clean screen inside the 1187x2513 Android mockup.
SCREEN_CROP_BOX = (63, 195, 1124, 2390)

# Map original design templates to raw Fastlane snapshot filenames (light mode versions)
SNAPSHOT_MAPPING = {
    "main_scrolled.png": "iPhone 17 Pro Max-02_MainDashboard_Active_Dark.png",
    "times.png": "iPhone 17 Pro Max-03_MemberSettings_Dark.png",
    "pause.png": "iPhone 17 Pro Max-01_MainDashboard_Empty_Dark.png",
    "share.png": "iPhone 17 Pro Max-04_ShareFamily_Dark.png",
    "main_full.png": "iPhone 17 Pro Max-02_MainDashboard_Active_Dark.png"
}

# Verified German and English screenshot text lists
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

# Source data for translation
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

# Total 22 app languages (excluding dialects like en-IN)
LANGUAGES = [
    "bn", "da", "de", "en", "es", "fr", "hi", "id", "it", "ja", "ko",
    "mr", "nl", "no", "pl", "pt", "ru", "sv", "tr", "uk", "vi", "zh-CN"
]

LOCALE_MAP = {
    "bn": ["bn-BD"],
    "da": ["da"],
    "de": ["de-DE"],
    "en": ["en-US"],
    "es": ["es-ES"],
    "fr": ["fr-FR"],
    "hi": ["hi"],
    "id": ["id"],
    "it": ["it"],
    "ja": ["ja"],
    "ko": ["ko"],
    "mr": ["mr-IN"],
    "nl": ["nl-NL"],
    "no": ["no"],
    "pl": ["pl"],
    "pt": ["pt-PT", "pt-BR"],
    "ru": ["ru"],
    "sv": ["sv"],
    "tr": ["tr"],
    "uk": ["uk"],
    "vi": ["vi"],
    "zh-CN": ["zh-Hans"]
}


def translate_text(text, target_lang):
    if not text.strip(): return text
    try:
        # Preserve newlines by substituting them with a placeholder
        translated = GoogleTranslator(source='en', target=target_lang).translate(text.replace("\n", " ||| "))
        return translated.replace(" ||| ", "\n").replace("|||", "\n")
    except Exception as e:
        print(f"Translation error to {target_lang}: {e}. Fallback to English.")
        return text

def fit_background(bg_path, target_size):
    bg = Image.open(bg_path)
    bg_w, bg_h = bg.size
    tgt_w, tgt_h = target_size
    
    # Scale to cover target size
    ratio = max(tgt_w / bg_w, tgt_h / bg_h)
    new_w = int(bg_w * ratio)
    new_h = int(bg_h * ratio)
    bg_resized = bg.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    # Crop center
    left = (new_w - tgt_w) // 2
    top = (new_h - tgt_h) // 2
    right = left + tgt_w
    bottom = top + tgt_h
    return bg_resized.crop((left, top, right, bottom))

def draw_bottom_vignette(img, target_size):
    # Draw a gradient at the bottom to increase text readability on light backgrounds
    w, h = target_size
    overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    
    # We create a gradient from Y=1600*scale (alpha=0) to bottom (alpha=170)
    scale = w / 1242.0
    start_y = int(1600 * scale)
    end_y = h
    for y in range(start_y, end_y):
        if y >= h: break
        alpha = int(170 * ((y - start_y) / (end_y - start_y)))
        # Dark indigo matching the app's dark theme
        draw.line([(0, y), (w, y)], fill=(11, 17, 30, alpha))
        
    return Image.alpha_composite(img.convert("RGBA"), overlay)

def draw_iphone_status_bar(draw, clock_font, fill_color):
    # Left clock
    draw.text((120, 68), "9:41", font=clock_font, fill=fill_color)
    
    # Right icons: signal bars
    draw.rectangle([980, 78, 984, 88], fill=fill_color)
    draw.rectangle([988, 74, 992, 88], fill=fill_color)
    draw.rectangle([996, 70, 1000, 88], fill=fill_color)
    draw.rectangle([1004, 66, 1008, 88], fill=fill_color)
    
    # WiFi triangle
    draw.polygon([(1025, 86), (1035, 70), (1045, 86)], fill=fill_color)
    
    # Battery body
    draw.rounded_rectangle([1060, 70, 1095, 86], radius=4, outline=fill_color, width=2)
    # Battery tip
    draw.rectangle([1095, 74, 1098, 82], fill=fill_color)
    # Battery level
    draw.rectangle([1063, 73, 1088, 83], fill=fill_color)

def create_iphone_mockup(screen_img, font_bold):
    # Base canvas for mock with bezel: 1130x2295
    bezel_w, bezel_h = 1130, 2295
    mock = Image.new("RGBA", (bezel_w, bezel_h), (0, 0, 0, 0))
    
    # Resize screen to fit screen area (1080x2245)
    ss_resized = screen_img.resize((1080, 2245), Image.Resampling.LANCZOS).convert("RGBA")
    
    # Create phone outline bezel
    draw = ImageDraw.Draw(mock)
    
    # Bezel shadow / dark metal rim
    draw.rounded_rectangle([0, 0, bezel_w, bezel_h], radius=75, fill="#1F1F1F")
    # Bezel inner black
    draw.rounded_rectangle([5, 5, bezel_w-5, bezel_h-5], radius=70, fill="#000000")
    
    # Paste screenshot inside bezel
    mock.paste(ss_resized, (25, 25), ss_resized)
    
    # Draw iOS components on top
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
    
    draw_iphone_status_bar(screen_draw, font_bold, fill_color)
    
    # Dynamic Island pill centered at X=565, Y=65 (width=220, height=55)
    screen_draw.rounded_rectangle([565 - 110, 52, 565 + 110, 88], radius=18, fill="#000000")
    
    # Home indicator at the bottom (centered, Y=2260, width=320, height=6)
    try:
        bot_r, bot_g, bot_b = ss_resized.getpixel((540, 2220))[:3]
        bot_brightness = 0.299 * bot_r + 0.587 * bot_g + 0.114 * bot_b
        home_color = "#FFFFFF" if bot_brightness < 128 else "#000000"
    except Exception:
        home_color = "#FFFFFF"
        
    screen_draw.rounded_rectangle([565 - 160, 2256, 565 + 160, 2262], radius=3, fill=home_color)
    
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
            
        # Detect CJK characters (Japanese and Chinese range) or Korean
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

def build_screenshot(slide, lang, size_name, target_size):
    # Fit background
    img = fit_background(BACKGROUND_PATH, target_size)
    
    # Setup fonts and scales
    tgt_w, tgt_h = target_size
    scale = tgt_w / 1242.0
    
    layout = slide["layout"]
    if layout == "top":
        img = draw_bottom_vignette(img, target_size)
        
    draw = ImageDraw.Draw(img)
    
    h_size = int(120 * scale)
    d_size = int(50 * scale)
    
    # Determine font paths and indices based on language
    if lang in ("ja", "zh-CN"):
        font_h = ImageFont.truetype(SYSTEM_JAPANESE, h_size, index=2) # Bold (W6)
        font_d = ImageFont.truetype(SYSTEM_JAPANESE, d_size, index=0) # Regular (W3)
        font_status = ImageFont.truetype(SYSTEM_JAPANESE, 34, index=2)
    elif lang == "ko":
        font_h = ImageFont.truetype(SYSTEM_KOREAN, h_size, index=6) # Bold
        font_d = ImageFont.truetype(SYSTEM_KOREAN, d_size, index=0) # Regular
        font_status = ImageFont.truetype(SYSTEM_KOREAN, 34, index=6)
    elif lang in ("hi", "mr"):
        font_h = ImageFont.truetype(SYSTEM_DEVANAGARI, h_size, index=1) # Bold
        font_d = ImageFont.truetype(SYSTEM_DEVANAGARI, d_size, index=0) # Regular
        font_status = ImageFont.truetype(SYSTEM_DEVANAGARI, 34, index=1)
    elif lang == "bn":
        font_h = ImageFont.truetype(SYSTEM_BENGALI, h_size, index=1) # Bold
        font_d = ImageFont.truetype(SYSTEM_BENGALI, d_size, index=0) # Regular
        font_status = ImageFont.truetype(SYSTEM_BENGALI, 34, index=1)
    elif lang in ("ru", "uk", "pl", "tr", "vi"):
        font_h = ImageFont.truetype(SYSTEM_HELVETICA, h_size, index=1) # Bold
        font_d = ImageFont.truetype(SYSTEM_HELVETICA, d_size, index=0) # Regular
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

    # Locate the image file (check fastlane snapshots first, then fall back to devices folder)
    device_file = slide["device_file"]
    device_path = None

    if device_file in SNAPSHOT_MAPPING:
        snapshot_name = SNAPSHOT_MAPPING[device_file]
        fastlane_lang = "zh-Hans" if lang == "zh-CN" else lang
        path = os.path.join(PROJECT_ROOT, "ios/fastlane/screenshots_temp", fastlane_lang, snapshot_name)
        if os.path.exists(path):
            device_path = path
            # Persist raw screenshot inside docs/internal/images/screenshots/devices/{lang}/
            pers_dir = os.path.join(DEVICES_DIR, lang)
            os.makedirs(pers_dir, exist_ok=True)
            pers_path = os.path.join(pers_dir, device_file)
            try:
                shutil.copy2(device_path, pers_path)
            except Exception as e:
                print(f"Could not persist raw screenshot to devices: {e}")
        else:
            path_en = os.path.join(PROJECT_ROOT, "ios/fastlane/screenshots_temp", "en", snapshot_name)
            if os.path.exists(path_en):
                device_path = path_en
                # Persist raw fallback screenshot inside docs/internal/images/screenshots/devices/{lang}/
                pers_dir = os.path.join(DEVICES_DIR, lang)
                os.makedirs(pers_dir, exist_ok=True)
                pers_path = os.path.join(pers_dir, device_file)
                try:
                    shutil.copy2(device_path, pers_path)
                except Exception as e:
                    print(f"Could not persist raw fallback screenshot to devices: {e}")

    if not device_path:
        device_path = os.path.join(DEVICES_DIR, lang, device_file)
        if not os.path.exists(device_path):
            device_path = os.path.join(DEVICES_DIR, "en", device_file)
            if not os.path.exists(device_path):
                print(f"Device file {device_file} not found, skipping!")
                return None

    with Image.open(device_path) as dev_img:
        if dev_img.size == (1187, 2513):
            # Old mockup, needs cropping
            screen_img = dev_img.crop(SCREEN_CROP_BOX)
        else:
            # Clean new simulator screenshot, use directly
            screen_img = dev_img.copy()
        
    # Create framed iOS phone mockup
    phone = create_iphone_mockup(screen_img, font_status)
    
    # Scale phone to target size
    phone_tgt_w = int(1010 * scale)
    phone_ratio = phone.width / phone.height
    phone_tgt_h = int(phone_tgt_w / phone_ratio)
    phone = phone.resize((phone_tgt_w, phone_tgt_h), Image.Resampling.LANCZOS)
    
    # Create drop shadow
    shadow_blur_radius = int(24 * scale)
    shadow = Image.new('RGBA', phone.size, (0, 0, 0, 0))
    alpha = phone.split()[3]
    shadow.paste((0, 0, 0, 120), mask=alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(shadow_blur_radius))
    
    # Auto-wrap texts dynamically to prevent screen overflow
    max_text_width = tgt_w - int(200 * scale) # Margins X=100 left/right
    wrapped_headline = wrap_text(slide["headline"], font_h, max_text_width, draw)
    wrapped_desc = wrap_text(slide["description"], font_d, max_text_width, draw)
    
    # Layout and positioning
    layout = slide["layout"]
    text_x = int(100 * scale)
    
    if layout == "bottom":
        text_y = int(220 * scale)
        draw.text((text_x, text_y), wrapped_headline, font=font_h, fill=ACCENT_COLOR, spacing=15)
        bbox_h = draw.textbbox((text_x, text_y), wrapped_headline, font=font_h)
        desc_y = bbox_h[3] + int(45 * scale)
        draw.text((text_x, desc_y), wrapped_desc, font=font_d, fill=TEXT_COLOR, spacing=15)
        
        paste_x = (tgt_w - phone.width) // 2
        paste_y = int(720 * scale)
        
    else:  # layout == "top"
        paste_x = (tgt_w - phone.width) // 2
        paste_y = int(-400 * scale)
        
        text_y = int(1850 * scale)
        draw.text((text_x, text_y), wrapped_headline, font=font_h, fill=ACCENT_COLOR, spacing=15)
        bbox_h = draw.textbbox((text_x, text_y), wrapped_headline, font=font_h)
        desc_y = bbox_h[3] + int(45 * scale)
        draw.text((text_x, desc_y), wrapped_desc, font=font_d, fill=TEXT_COLOR, spacing=15)
        
    shadow_offset = (0, int(15 * scale))
    img.paste(shadow, (paste_x + shadow_offset[0], paste_y + shadow_offset[1]), shadow)
    img.paste(phone, (paste_x, paste_y), phone)
    
    if img.mode == 'RGBA':
        img = img.convert('RGB')
        
    return img

def main():
    import shutil
    
    # 1. Define output directories
    ios_screenshots_dir = os.path.join(PROJECT_ROOT, "ios/fastlane/screenshots")
    temp_screenshots_dir = os.path.join(PROJECT_ROOT, "ios/fastlane/screenshots_temp")
    
    # Remove any existing temp directory
    if os.path.exists(temp_screenshots_dir):
        shutil.rmtree(temp_screenshots_dir)
        
    # 2. Copy current raw screenshots to temp directory
    if os.path.exists(ios_screenshots_dir):
        print(f"Creating backup of raw screenshots to: {temp_screenshots_dir}")
        shutil.copytree(ios_screenshots_dir, temp_screenshots_dir)
    else:
        print("Warning: ios/fastlane/screenshots does not exist. No raw screenshots found!")
        os.makedirs(temp_screenshots_dir, exist_ok=True)
        
    # 3. Clean up and recreate the ios/fastlane/screenshots directory to ensure no invalid folders are left
    if os.path.exists(ios_screenshots_dir):
        shutil.rmtree(ios_screenshots_dir)
    os.makedirs(ios_screenshots_dir, exist_ok=True)
    
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"Target root docs directory: {OUTPUT_DIR}")
    
    # 4. Generate screenshots for mapped languages
    for lang in LANGUAGES:
        # Check if language is supported on iOS App Store Connect
        if lang not in LOCALE_MAP:
            print(f"Skipping iOS screenshot generation for '{lang}' (not supported by App Store Connect)")
            continue
            
        # Target local docs output dir
        lang_output_dir = os.path.join(OUTPUT_DIR, lang)
        os.makedirs(lang_output_dir, exist_ok=True)
        
        # Load or generate localized strings
        if lang in VERIFIED_SS:
            slides_text = VERIFIED_SS[lang]
        else:
            print(f"Translating texts for language '{lang}' dynamically...")
            slides_text = []
            for hl, tx in VERIFIED_SS["en"]:
                t_hl = translate_text(hl, lang)
                t_tx = translate_text(tx, lang)
                slides_text.append((t_hl, t_tx))
                time.sleep(0.15) # Rate limit protection
                
        # Build slides configurations
        slides = []
        for idx, temp in enumerate(SLIDES_TEMPLATES):
            slides.append({
                "headline": slides_text[idx][0],
                "description": slides_text[idx][1],
                "device_file": temp["device_file"],
                "layout": temp["layout"]
            })
            
        # Get target iOS locales
        target_locales = LOCALE_MAP[lang]
        
        for size_name, size in SIZES.items():
            print(f"Generating screenshots for '{lang}' size {size_name} ({size[0]}x{size[1]})...")
            device_name = "iPhone 11 Pro Max" if size_name == "6.5" else "iPhone 17 Pro Max"
            for idx, slide in enumerate(slides):
                img = build_screenshot(slide, lang, size_name, size)
                if img:
                    # 1. Save to docs/ (for preview/checking)
                    filename = f"screenshot_{idx+1}_{lang}_{size_name}.jpg"
                    save_path = os.path.join(lang_output_dir, filename)
                    img.save(save_path, "JPEG", quality=93)
                    
                    # 2. Save directly to fastlane/screenshots/{locale}/ as PNG for automated deliver upload
                    for locale in target_locales:
                        fastlane_lang_dir = os.path.join(ios_screenshots_dir, locale)
                        os.makedirs(fastlane_lang_dir, exist_ok=True)
                        fastlane_filename = f"{device_name}-0{idx+1}_Slide.png"
                        fastlane_save_path = os.path.join(fastlane_lang_dir, fastlane_filename)
                        img.save(fastlane_save_path, "PNG")
                        
            print(f"  Done '{lang}' size {size_name}")
            
    # 5. Clean up temporary directory
    if os.path.exists(temp_screenshots_dir):
        print(f"Cleaning up temporary directory: {temp_screenshots_dir}")
        shutil.rmtree(temp_screenshots_dir)

if __name__ == "__main__":
    main()
